package iuh.fit.backend.mapper;

import iuh.fit.backend.dto.HotelDTO;
import iuh.fit.backend.entity.HotelEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper chuyển đổi giữa HotelEntity (DB layer) và HotelDTO (API layer).
 * Tách biệt logic mapping khỏi Service để dễ bảo trì và test.
 */
@Component
public class HotelMapper {

    /**
     * Entity → DTO (dùng khi trả về API response)
     */
    public HotelDTO toDTO(HotelEntity entity) {
        if (entity == null) return null;
        return new HotelDTO(
                entity.getId(),
                entity.getName(),
                entity.getCity(),
                entity.getPrice(),
                entity.getStars(),
                entity.getDescription(),
                entity.getAmenities()
        );
    }

    /**
     * DTO → Entity (dùng khi tạo mới từ request)
     */
    public HotelEntity toEntity(HotelDTO dto) {
        if (dto == null) return null;
        return HotelEntity.builder()
                .id(dto.id())
                .name(dto.name())
                .city(dto.city())
                .price(dto.price())
                .stars(dto.stars())
                .description(dto.description())
                .amenities(dto.amenities())
                .build();
    }
}
