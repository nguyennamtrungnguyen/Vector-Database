package iuh.fit.backend.service;

import iuh.fit.backend.entity.HotelEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/** Keeps the in-memory vector index and its on-disk cache in sync with hotel records. */
@Component
public class HotelVectorIndex {

    private static final Logger log = LoggerFactory.getLogger(HotelVectorIndex.class);

    private final SimpleVectorStore vectorStore;

    @Value("${app.vector-store.file-path:data/vector-store-gemini.json}")
    private String vectorStoreFilePath;

    public HotelVectorIndex(SimpleVectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void initialize(List<HotelEntity> hotels) {
        if (hotels.isEmpty()) {
            return;
        }
        vectorStore.add(hotels.stream().map(this::toDocument).toList());
        saveCache();
    }

    public void upsert(HotelEntity hotel) {
        vectorStore.add(List.of(toDocument(hotel)));
        saveCache();
    }

    public void delete(String hotelId) {
        vectorStore.delete(List.of(hotelId));
        saveCache();
    }

    private Document toDocument(HotelEntity hotel) {
        List<String> amenities = hotel.getAmenities() == null ? List.of() : hotel.getAmenities();
        String content = hotel.getName() + ". " + hotel.getDescription()
                + " Tiện ích: " + String.join(", ", amenities) + ".";
        Map<String, Object> metadata = Map.of(
                "hotelId", hotel.getId(),
                "city", hotel.getCity(),
                "price", hotel.getPrice(),
                "stars", hotel.getStars()
        );
        return new Document(hotel.getId(), content, metadata);
    }

    private void saveCache() {
        File file = new File(vectorStoreFilePath);
        File parent = file.getAbsoluteFile().getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Không tạo được thư mục vector store: " + parent);
        }
        vectorStore.save(file);
        log.debug("Đã lưu vector store vào {}", file.getAbsolutePath());
    }
}
