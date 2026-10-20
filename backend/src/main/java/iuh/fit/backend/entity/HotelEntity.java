package iuh.fit.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/**
 * JPA Entity ánh xạ bảng "hotels" trong MariaDB.
 * amenities được lưu dạng JSON string nhờ {@link StringListConverter}.
 */
@Entity
@Table(name = "hotels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HotelEntity {

    @Id
    @Column(length = 10)
    private String id;                         // "H001", "H002", ...

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Long price;                        // VND

    @Column(nullable = false)
    private Integer stars;                     // 1 - 5

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    /**
     * Danh sách tiện ích lưu dạng JSON string trong DB:
     * ["wifi", "spa", "bể bơi"]
     */
    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> amenities;
}
