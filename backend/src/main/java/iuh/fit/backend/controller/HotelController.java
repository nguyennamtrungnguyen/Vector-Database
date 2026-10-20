package iuh.fit.backend.controller;

import iuh.fit.backend.dto.HotelDTO;
import iuh.fit.backend.dto.SearchRequestDTO;
import iuh.fit.backend.dto.SearchResponseDTO;
import iuh.fit.backend.dto.SearchResultDTO;
import iuh.fit.backend.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller cung cấp RESTful APIs quản lý và tìm kiếm khách sạn.
 * Chỉ phụ thuộc vào HotelService (interface), không biết implementation.
 */
@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    /**
     * GET /api/hotels
     * Lấy danh sách tất cả khách sạn.
     */
    @GetMapping
    public List<HotelDTO> getAllHotels() {
        return hotelService.getAllHotels();
    }

    @GetMapping("/{id}")
    public HotelDTO getHotel(@PathVariable String id) {
        return hotelService.getHotel(id);
    }

    @PostMapping
    public ResponseEntity<HotelDTO> createHotel(@Valid @RequestBody HotelDTO hotel) {
        HotelDTO created = hotelService.createHotel(hotel);
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public HotelDTO updateHotel(@PathVariable String id, @Valid @RequestBody HotelDTO hotel) {
        return hotelService.updateHotel(id, hotel);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHotel(@PathVariable String id) {
        hotelService.deleteHotel(id);
    }

    /**
     * GET /api/hotels/search?q=...&mode=keyword|semantic&topK=5&city=...&maxPrice=...
     * Tìm kiếm khách sạn theo từ khóa hoặc ngữ nghĩa.
     */
    @GetMapping("/search")
    public ResponseEntity<SearchResponseDTO> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "semantic") String mode,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) Long maxPrice
    ) {
        SearchRequestDTO request = SearchRequestDTO.create(q, mode, topK, city, maxPrice);

        SearchResponseDTO response = switch (request.mode()) {
            case "keyword" -> hotelService.keywordSearch(
                    request.q(), request.topK(), request.city(), request.maxPrice());
            case "semantic" -> hotelService.semanticSearch(
                    request.q(), request.topK(), request.city(), request.maxPrice());
            default -> throw new IllegalStateException("Unexpected mode after validation");
        };

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/hotels/{id}/similar?topK=5
     * Tìm các khách sạn tương tự theo embedding.
     */
    @GetMapping("/{id}/similar")
    public ResponseEntity<List<SearchResultDTO>> findSimilar(
            @PathVariable String id,
            @RequestParam(defaultValue = "5") int topK
    ) {
        List<SearchResultDTO> results = hotelService.findSimilar(id, topK);
        return ResponseEntity.ok(results);
    }
}
