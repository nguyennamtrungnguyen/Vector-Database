package iuh.fit.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * DTO trả về thông tin một khách sạn qua API.
 * Tách biệt với HotelEntity để Controller không expose trực tiếp DB schema.
 */
public record HotelDTO(
        @NotBlank @Size(max = 10)
        String id,
        @NotBlank @Size(max = 255)
        String name,
        @NotBlank @Size(max = 255)
        String city,
        @Positive
        long price,
        @Min(1) @Max(5)
        int stars,
        @NotBlank
        String description,
        @NotNull @Size(max = 50)
        List<String> amenities
) {}
