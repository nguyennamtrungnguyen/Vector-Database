package iuh.fit.backend.service.impl;

import iuh.fit.backend.dto.HotelDTO;
import iuh.fit.backend.dto.SearchResponseDTO;
import iuh.fit.backend.dto.SearchResultDTO;
import iuh.fit.backend.entity.HotelEntity;
import iuh.fit.backend.exception.HotelNotFoundException;
import iuh.fit.backend.exception.HotelAlreadyExistsException;
import iuh.fit.backend.mapper.HotelMapper;
import iuh.fit.backend.repository.HotelRepository;
import iuh.fit.backend.service.HotelService;
import iuh.fit.backend.service.HotelVectorIndex;
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

/**
 * Implementation của HotelService:
 * - keywordSearch: dùng JPQL LIKE trên MariaDB
 * - semanticSearch: dùng Gemini Embedding + SimpleVectorStore (hybrid: vector + filter metadata)
 * - findSimilar: tìm khách sạn tương tự qua vector similarity
 */
@Service
public class HotelServiceImpl implements HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelServiceImpl.class);

    private final HotelRepository hotelRepository;
    private final SimpleVectorStore vectorStore;
    private final HotelMapper hotelMapper;
    private final HotelVectorIndex hotelVectorIndex;

    public HotelServiceImpl(HotelRepository hotelRepository,
                            SimpleVectorStore vectorStore,
                            HotelMapper hotelMapper,
                            HotelVectorIndex hotelVectorIndex) {
        this.hotelRepository = hotelRepository;
        this.vectorStore = vectorStore;
        this.hotelMapper = hotelMapper;
        this.hotelVectorIndex = hotelVectorIndex;
    }

    @Override
    public HotelDTO getHotel(String id) {
        return hotelRepository.findById(id)
                .map(hotelMapper::toDTO)
                .orElseThrow(() -> new HotelNotFoundException(id));
    }

    @Override
    public HotelDTO createHotel(HotelDTO hotel) {
        if (hotelRepository.existsById(hotel.id())) {
            throw new HotelAlreadyExistsException(hotel.id());
        }
        HotelEntity saved = hotelRepository.save(hotelMapper.toEntity(hotel));
        hotelVectorIndex.upsert(saved);
        return hotelMapper.toDTO(saved);
    }

    @Override
    public HotelDTO updateHotel(String id, HotelDTO hotel) {
        if (!hotelRepository.existsById(id)) {
            throw new HotelNotFoundException(id);
        }
        HotelDTO normalized = new HotelDTO(id, hotel.name(), hotel.city(), hotel.price(),
                hotel.stars(), hotel.description(), hotel.amenities());
        HotelEntity saved = hotelRepository.save(hotelMapper.toEntity(normalized));
        hotelVectorIndex.upsert(saved);
        return hotelMapper.toDTO(saved);
    }

    @Override
    public void deleteHotel(String id) {
        if (!hotelRepository.existsById(id)) {
            throw new HotelNotFoundException(id);
        }
        hotelRepository.deleteById(id);
        hotelVectorIndex.delete(id);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Override
    public List<HotelDTO> getAllHotels() {
        return hotelRepository.findAll()
                .stream()
                .map(hotelMapper::toDTO)
                .toList();
    }

    // =========================================================
    // KEYWORD SEARCH (SQL LIKE via JPA Repository)
    // =========================================================

    @Override
    public SearchResponseDTO keywordSearch(String query, int topK, String city, Long maxPrice) {
        long startTime = System.currentTimeMillis();

        boolean hasCity  = (city != null && !city.isBlank());
        boolean hasPrice = (maxPrice != null);

        List<HotelEntity> entities;
        if (hasCity && hasPrice) {
            entities = hotelRepository.findByKeywordAndCityAndMaxPrice(query, city, maxPrice);
        } else if (hasCity) {
            entities = hotelRepository.findByKeywordAndCity(query, city);
        } else if (hasPrice) {
            entities = hotelRepository.findByKeywordAndMaxPrice(query, maxPrice);
        } else {
            entities = hotelRepository.findByKeyword(query);
        }

        List<SearchResultDTO> results = entities.stream()
                .limit(topK)
                .map(entity -> new SearchResultDTO(hotelMapper.toDTO(entity), null))
                .toList();

        long tookMs = System.currentTimeMillis() - startTime;
        log.debug("Keyword search '{}' → {} kết quả trong {} ms", query, results.size(), tookMs);

        return new SearchResponseDTO("keyword", tookMs, results);
    }

    // =========================================================
    // SEMANTIC SEARCH (Gemini Embedding + SimpleVectorStore)
    // =========================================================

    @Override
    public SearchResponseDTO semanticSearch(String query, int topK, String city, Long maxPrice) {
        long startTime = System.currentTimeMillis();

        List<SearchResultDTO> results = new ArrayList<>();
        try {
            SearchRequest searchRequest = buildSemanticRequest(query, topK, city, maxPrice);
            List<Document> matchedDocs = vectorStore.similaritySearch(searchRequest);

            results = matchedDocs.stream()
                    .map(doc -> {
                        String hotelId = (String) doc.getMetadata().get("hotelId");
                        return hotelRepository.findById(hotelId)
                                .map(entity -> new SearchResultDTO(hotelMapper.toDTO(entity), extractScore(doc)))
                                .orElse(null);
                    })
                    .filter(r -> r != null)
                    .toList();

        } catch (Exception ex) {
            log.error("Lỗi semantic search (kiểm tra GEMINI_API_KEY / kết nối mạng): {}", ex.getMessage());
        }

        long tookMs = System.currentTimeMillis() - startTime;
        log.debug("Semantic search '{}' → {} kết quả trong {} ms", query, results.size(), tookMs);

        return new SearchResponseDTO("semantic", tookMs, results);
    }

    // =========================================================
    // FIND SIMILAR
    // =========================================================

    @Override
    public List<SearchResultDTO> findSimilar(String hotelId, int topK) {
        HotelEntity target = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new HotelNotFoundException(hotelId));

        String query = target.getName() + " " + target.getDescription();
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK + 1)   // +1 để loại trừ chính nó
                .build();

        List<Document> docs = vectorStore.similaritySearch(request);

        return docs.stream()
                .map(doc -> {
                    String id = (String) doc.getMetadata().get("hotelId");
                    if (id == null || id.equals(hotelId)) return null;
                    return hotelRepository.findById(id)
                            .map(entity -> new SearchResultDTO(hotelMapper.toDTO(entity), extractScore(doc)))
                            .orElse(null);
                })
                .filter(r -> r != null)
                .limit(topK)
                .toList();
    }

    // =========================================================
    // PRIVATE HELPERS
    // =========================================================

    /**
     * Xây dựng SearchRequest kết hợp FilterExpression (hybrid query: vector + metadata filter).
     */
    private SearchRequest buildSemanticRequest(String query, int topK, String city, Long maxPrice) {
        var builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

        boolean hasCity  = (city != null && !city.isBlank());
        boolean hasPrice = (maxPrice != null);

        if (hasCity || hasPrice) {
            var fb = new FilterExpressionBuilder();
            if (hasCity && hasPrice) {
                builder.filterExpression(fb.and(fb.eq("city", city), fb.lte("price", maxPrice)).build());
            } else if (hasCity) {
                builder.filterExpression(fb.eq("city", city).build());
            } else {
                builder.filterExpression(fb.lte("price", maxPrice).build());
            }
        }

        return builder.build();
    }

    /**
     * Trích xuất điểm cosine similarity từ metadata của Document.
     */
    private Double extractScore(Document doc) {
        Map<String, Object> metadata = doc.getMetadata();
        Object score = metadata.get("score");
        if (score == null) score = metadata.get("distance");
        return (score instanceof Number num) ? num.doubleValue() : null;
    }
}
