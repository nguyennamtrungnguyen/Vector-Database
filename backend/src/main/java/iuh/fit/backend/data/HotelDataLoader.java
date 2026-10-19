package iuh.fit.backend.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.backend.model.Hotel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
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
 * Nạp dữ liệu khách sạn từ hotels.json và tạo vector embedding:
 * - Luôn đọc danh sách khách sạn từ classpath (hotels.json) để phục vụ keyword search.
 * - Nếu file vector store chưa tồn tại: gọi Gemini Embedding API để embed từng khách sạn,
 *   rồi lưu ra file JSON để các lần khởi động sau không phải gọi API lại.
 */
@Component
public class HotelDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(HotelDataLoader.class);

    private final SimpleVectorStore vectorStore;
    private final List<Hotel> hotels;

    @Value("${app.vector-store.file-path:data/vector-store.json}")
    private String vectorStoreFilePath;

    public HotelDataLoader(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.hotels = readHotels();
    }

    public List<Hotel> getAllHotels() {
        return hotels;
    }

    @Override
    public void run(ApplicationArguments args) {
        File file = new File(vectorStoreFilePath);
        if (file.exists()) {
            log.info("VectorStore đã có sẵn tại {}, bỏ qua bước embedding.", file.getAbsolutePath());
            return;
        }

        log.info("Bắt đầu tạo embedding cho {} khách sạn qua Gemini API...", hotels.size());
        List<Document> documents = hotels.stream().map(this::toDocument).toList();
        vectorStore.add(documents);

        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục: " + parent);
        }
        vectorStore.save(file);
        log.info("Đã lưu VectorStore vào {}", file.getAbsolutePath());
    }

    /**
     * Chuyển Hotel thành Document: nội dung = tên + mô tả + tiện ích, metadata dùng để lọc (hybrid query).
     */
    private Document toDocument(Hotel hotel) {
        String content = hotel.name() + ". " + hotel.description()
                + " Tiện ích: " + String.join(", ", hotel.amenities()) + ".";
        Map<String, Object> metadata = Map.of(
                "hotelId", hotel.id(),
                "city", hotel.city(),
                "price", hotel.price(),
                "stars", hotel.stars()
        );
        return new Document(hotel.id(), content, metadata);
    }

    private static List<Hotel> readHotels() {
        try (InputStream in = new ClassPathResource("hotels.json").getInputStream()) {
            return List.copyOf(new ObjectMapper().readValue(in, new TypeReference<List<Hotel>>() {}));
        } catch (IOException e) {
            throw new IllegalStateException("Không đọc được hotels.json", e);
        }
    }
}
