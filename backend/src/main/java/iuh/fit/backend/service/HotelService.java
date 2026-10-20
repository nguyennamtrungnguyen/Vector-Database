package iuh.fit.backend.service;

import iuh.fit.backend.dto.HotelDTO;
import iuh.fit.backend.dto.SearchResponseDTO;
import iuh.fit.backend.dto.SearchResultDTO;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ quản lý và tìm kiếm khách sạn.
 * Controller chỉ phụ thuộc vào interface này, không biết implementation cụ thể.
 */
public interface HotelService {

    HotelDTO getHotel(String id);

    HotelDTO createHotel(HotelDTO hotel);

    HotelDTO updateHotel(String id, HotelDTO hotel);

    void deleteHotel(String id);

    /**
     * Lấy danh sách tất cả khách sạn từ DB.
     */
    List<HotelDTO> getAllHotels();

    /**
     * Tìm kiếm theo từ khóa (SQL LIKE trên name + description).
     *
     * @param query    từ khóa tìm kiếm
     * @param topK     số kết quả tối đa trả về
     * @param city     lọc theo thành phố (null = không lọc)
     * @param maxPrice lọc theo giá tối đa (null = không lọc)
     */
    SearchResponseDTO keywordSearch(String query, int topK, String city, Long maxPrice);

    /**
     * Tìm kiếm theo ngữ nghĩa (Gemini Embedding + SimpleVectorStore).
     *
     * @param query    câu truy vấn tự nhiên
     * @param topK     số kết quả tối đa trả về
     * @param city     lọc theo thành phố (null = không lọc)
     * @param maxPrice lọc theo giá tối đa (null = không lọc)
     */
    SearchResponseDTO semanticSearch(String query, int topK, String city, Long maxPrice);

    /**
     * Tìm các khách sạn tương tự với khách sạn đã cho (dựa trên embedding).
     *
     * @param hotelId ID khách sạn gốc
     * @param topK    số kết quả tối đa (loại trừ chính nó)
     */
    List<SearchResultDTO> findSimilar(String hotelId, int topK);
}
