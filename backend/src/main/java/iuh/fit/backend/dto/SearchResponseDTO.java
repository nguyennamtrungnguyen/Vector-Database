package iuh.fit.backend.dto;

import java.util.List;

/**
 * DTO trả về kết quả tìm kiếm:
 * { "mode": "semantic", "tookMs": 120, "results": [ ... ] }
 */
public record SearchResponseDTO(
        String mode,
        long tookMs,
        List<SearchResultDTO> results
) {}
