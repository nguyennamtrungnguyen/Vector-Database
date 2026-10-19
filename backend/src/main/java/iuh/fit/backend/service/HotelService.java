package iuh.fit.backend.service;

import iuh.fit.backend.data.HotelDataLoader;
import iuh.fit.backend.model.Hotel;
import iuh.fit.backend.model.SearchResponse;
import iuh.fit.backend.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service xử lý tìm kiếm khách sạn:
 * 1. mode=keyword: Tìm kiếm chuỗi con không phân biệt hoa thường trên name + description (giả lập SQL LIKE '%q%')
 * 2. mode=semantic: VectorStore.similaritySearch kết hợp lọc metadata (truy vấn lai - hybrid)
 * 3. similar: Tìm khách sạn tương tự dựa trên embedding
 */
@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);

    private final HotelDataLoader dataLoader;
    private final SimpleVectorStore vectorStore;

    public HotelService(HotelDataLoader dataLoader, SimpleVectorStore vectorStore) {
        this.dataLoader = dataLoader;
        this.vectorStore = vectorStore;
    }

    public List<Hotel> getAllHotels() {
        return dataLoader.getAllHotels();
    }

    /**
     * Tìm theo từ khóa (Keyword search mô phỏng SQL LIKE '%q%')
     */
    public SearchResponse keywordSearch(String query, int topK, String city, Long maxPrice) {
        long startTime = System.currentTimeMillis();
        String lowerQuery = query.toLowerCase();

        List<SearchResult> results = dataLoader.getAllHotels().stream()
                .filter(hotel -> matchesKeyword(hotel, lowerQuery))
                .filter(hotel -> city == null || hotel.city().equalsIgnoreCase(city))
                .filter(hotel -> maxPrice == null || hotel.price() <= maxPrice)
                .limit(topK)
                .map(hotel -> new SearchResult(hotel, null)) // mode=keyword thì score = null
                .toList();

        long tookMs = System.currentTimeMillis() - startTime;
        log.debug("Keyword search '{}' trả về {} kết quả trong {} ms", query, results.size(), tookMs);

        return new SearchResponse("keyword", tookMs, results);
    }

    /**
     * Tìm theo ngữ nghĩa (Semantic search dùng VectorStore)
     */
    public SearchResponse semanticSearch(String query, int topK, String city, Long maxPrice) {
        long startTime = System.currentTimeMillis();

        List<SearchResult> results = new ArrayList<>();
        try {
            // Xây dựng SearchRequest kết hợp FilterExpression lọc metadata
            SearchRequest searchRequest = buildSemanticRequest(query, topK, city, maxPrice);

            // Gọi similaritySearch thực thụ trên VectorStore
            List<Document> matchedDocuments = vectorStore.similaritySearch(searchRequest);

            results = matchedDocuments.stream()
                    .map(doc -> {
                        String hotelId = (String) doc.getMetadata().get("hotelId");
                        Optional<Hotel> hotelOpt = findHotelById(hotelId);
                        Double score = extractScore(doc);
                        return hotelOpt.map(h -> new SearchResult(h, score)).orElse(null);
                    })
                    .filter(r -> r != null)
                    .toList();
        } catch (Exception ex) {
            log.error("Lỗi khi tìm kiếm ngữ nghĩa với Gemini (kiểm tra GEMINI_API_KEY / kết nối mạng): {}", ex.getMessage());
            // Trả về danh sách rỗng thay vì làm sập API và văng lỗi 500
        }

        long tookMs = System.currentTimeMillis() - startTime;
        log.debug("Semantic search '{}' trả về {} kết quả trong {} ms", query, results.size(), tookMs);

        return new SearchResponse("semantic", tookMs, results);
    }

    /**
     * Tìm khách sạn tương tự (loại trừ chính khách sạn đó)
     */
    public List<SearchResult> findSimilar(String hotelId, int topK) {
        Hotel targetHotel = findHotelById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("Hotel not found with id: " + hotelId));

        // Dùng nội dung mô tả và tiện ích của khách sạn để tìm kiếm tương tự
        String query = targetHotel.name() + " " + targetHotel.description();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK + 1) // +1 để loại trừ chính nó
                .build();

        List<Document> matchedDocs = vectorStore.similaritySearch(request);

        return matchedDocs.stream()
                .map(doc -> {
                    String id = (String) doc.getMetadata().get("hotelId");
                    if (id == null || id.equals(hotelId)) {
                        return null; // Bỏ qua chính nó
                    }
                    return findHotelById(id)
                            .map(h -> new SearchResult(h, extractScore(doc)))
                            .orElse(null);
                })
                .filter(r -> r != null)
                .limit(topK)
                .toList();
    }

    private boolean matchesKeyword(Hotel hotel, String lowerQuery) {
        return hotel.name().toLowerCase().contains(lowerQuery)
                || hotel.description().toLowerCase().contains(lowerQuery);
    }

    /**
     * Xây dựng SearchRequest có kèm FilterExpression để truy vấn lai (hybrid query)
     */
    private SearchRequest buildSemanticRequest(String query, int topK, String city, Long maxPrice) {
        var builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

        if (city != null || maxPrice != null) {
            var fb = new FilterExpressionBuilder();
            boolean hasCity = (city != null && !city.isBlank());
            boolean hasPrice = (maxPrice != null);

            if (hasCity && hasPrice) {
                builder.filterExpression(fb.and(fb.eq("city", city), fb.lte("price", maxPrice)).build());
            } else if (hasCity) {
                builder.filterExpression(fb.eq("city", city).build());
            } else if (hasPrice) {
                builder.filterExpression(fb.lte("price", maxPrice).build());
            }
        }

        return builder.build();
    }

    private Optional<Hotel> findHotelById(String id) {
        return dataLoader.getAllHotels().stream()
                .filter(h -> h.id().equals(id))
                .findFirst();
    }

    /**
     * Trích xuất điểm tương đồng cosine similarity (score) từ Document metadata
     */
    private Double extractScore(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        Object score = metadata.get("score");
        if (score == null) {
            score = metadata.get("distance");
        }
        if (score instanceof Number num) {
            return num.doubleValue();
        }
        return null;
    }
}
