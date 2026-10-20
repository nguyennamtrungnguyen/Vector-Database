package iuh.fit.backend.dto;

/** Normalized search parameters passed from the HTTP layer to the search service. */
public record SearchRequestDTO(
        String q,
        String mode,
        int topK,
        String city,
        Long maxPrice
) {
    public static SearchRequestDTO create(String q, String mode, int topK, String city, Long maxPrice) {
        if (q == null || q.isBlank()) {
            throw new IllegalArgumentException("Tham số 'q' không được để trống");
        }
        if (topK < 1 || topK > 20) {
            throw new IllegalArgumentException("Tham số 'topK' phải nằm trong khoảng từ 1 đến 20");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("Tham số 'maxPrice' không được âm");
        }
        String normalizedMode = mode == null ? "semantic" : mode.toLowerCase(java.util.Locale.ROOT);
        if (!normalizedMode.equals("keyword") && !normalizedMode.equals("semantic")) {
            throw new IllegalArgumentException("Tham số 'mode' phải là 'keyword' hoặc 'semantic'");
        }
        return new SearchRequestDTO(q.trim(), normalizedMode, topK,
                city == null || city.isBlank() ? null : city.trim(), maxPrice);
    }
}
