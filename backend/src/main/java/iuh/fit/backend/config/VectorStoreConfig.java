package iuh.fit.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Cấu hình SimpleVectorStore (in-memory):
 * - Nếu file data/vector-store.json đã tồn tại: load trực tiếp, không tốn thời gian tính toán lại.
 * - Nếu chưa có: khởi tạo rỗng để HotelDataLoader embed qua Gemini API và lưu file.
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Value("${app.vector-store.file-path:data/vector-store.json}")
    private String vectorStoreFilePath;

    @Bean
    public SimpleVectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        File file = new File(vectorStoreFilePath);
        if (file.exists()) {
            // Nạp vector store từ file JSON đã lưu từ trước
            log.info("Nạp VectorStore in-memory từ file: {}", file.getAbsolutePath());
            vectorStore.load(file);
        } else {
            log.info("Chưa tìm thấy file vector store ({}), chuẩn bị tạo mới.", file.getAbsolutePath());
        }

        return vectorStore;
    }
}
