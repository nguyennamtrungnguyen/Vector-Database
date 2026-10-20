package iuh.fit.backend.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.backend.entity.HotelEntity;
import iuh.fit.backend.repository.HotelRepository;
import iuh.fit.backend.service.HotelVectorIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Khởi động ứng dụng và thực hiện 2 nhiệm vụ:
 * <ol>
 *   <li>Seed data: Nếu bảng hotels trong MariaDB rỗng → đọc hotels.json và lưu vào DB.</li>
 *   <li>Build vector: Nếu file vector-store-gemini.json chưa tồn tại → embed tất cả khách sạn
 *       từ DB qua Gemini API và lưu ra file (cache). Lần sau chỉ load file, không gọi API lại.</li>
 * </ol>
 */
@Component
public class HotelDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HotelDataLoader.class);

    private final HotelRepository hotelRepository;
    private final HotelVectorIndex hotelVectorIndex;

    @Value("${app.vector-store.file-path:data/vector-store-gemini.json}")
    private String vectorStoreFilePath;

    public HotelDataLoader(HotelRepository hotelRepository, HotelVectorIndex hotelVectorIndex) {
        this.hotelRepository = hotelRepository;
        this.hotelVectorIndex = hotelVectorIndex;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedDatabaseIfEmpty();
        buildVectorStoreIfNeeded();
    }

    // =========================================================
    // SEED DATABASE
    // =========================================================

    /**
     * Nếu bảng hotels rỗng, đọc hotels.json từ classpath và lưu vào MariaDB.
     */
    private void seedDatabaseIfEmpty() {
        if (hotelRepository.count() > 0) {
            log.info("DB đã có {} khách sạn, bỏ qua bước seed.", hotelRepository.count());
            return;
        }

        log.info("Bảng hotels rỗng. Bắt đầu seed từ hotels.json...");
        List<HotelEntity> entities = readHotelsFromJson();
        hotelRepository.saveAll(entities);
        log.info("Đã seed {} khách sạn vào MariaDB.", entities.size());
    }

    /**
     * Đọc file hotels.json từ classpath và parse thành danh sách HotelEntity.
     */
    private List<HotelEntity> readHotelsFromJson() {
        try (InputStream in = new ClassPathResource("hotels.json").getInputStream()) {
            // Parse JSON thành raw Map để tránh phụ thuộc vào Hotel record cũ
            List<Map<String, Object>> rawList = new ObjectMapper()
                    .readValue(in, new TypeReference<List<Map<String, Object>>>() {});

            return rawList.stream().map(raw -> {
                @SuppressWarnings("unchecked")
                List<String> amenities = (List<String>) raw.getOrDefault("amenities", List.of());
                return HotelEntity.builder()
                        .id((String) raw.get("id"))
                        .name((String) raw.get("name"))
                        .city((String) raw.get("city"))
                        .price(((Number) raw.get("price")).longValue())
                        .stars(((Number) raw.get("stars")).intValue())
                        .description((String) raw.get("description"))
                        .amenities(amenities)
                        .build();
            }).toList();

        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được hotels.json: " + e.getMessage(), e);
        }
    }

    // =========================================================
    // BUILD VECTOR STORE
    // =========================================================

    /**
     * Nếu file vector cache chưa tồn tại, embed tất cả khách sạn từ DB
     * qua Gemini Embedding API và lưu ra file JSON để cache.
     */
    private void buildVectorStoreIfNeeded() {
        File file = new File(vectorStoreFilePath);
        if (file.exists()) {
            log.info("Vector store cache đã có tại {}, bỏ qua bước embedding.", file.getAbsolutePath());
            return;
        }

        List<HotelEntity> hotels = hotelRepository.findAll();
        log.info("Bắt đầu tạo embedding cho {} khách sạn qua Gemini API...", hotels.size());
        hotelVectorIndex.initialize(hotels);
        log.info("Đã lưu vector store vào {}", file.getAbsolutePath());
    }
}
