package iuh.fit.backend.dto;

/**
 * DTO một phần tử trong danh sách kết quả tìm kiếm.
 * score = null khi mode là keyword; score ∈ [0,1] khi mode là semantic.
 */
public record SearchResultDTO(
        HotelDTO hotel,
        Double score
) {}
