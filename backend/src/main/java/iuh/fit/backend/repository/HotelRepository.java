package iuh.fit.backend.repository;

import iuh.fit.backend.entity.HotelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository giao tiếp với bảng "hotels" trong MariaDB qua Spring Data JPA.
 */
@Repository
public interface HotelRepository extends JpaRepository<HotelEntity, String> {

    /**
     * Keyword search: tìm chuỗi con (không phân biệt hoa thường) trong name và description.
     * Tương đương SQL: WHERE LOWER(name) LIKE '%keyword%' OR LOWER(description) LIKE '%keyword%'
     */
    @Query("SELECT h FROM HotelEntity h WHERE " +
           "LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<HotelEntity> findByKeyword(@Param("keyword") String keyword);

    /**
     * Keyword search kết hợp lọc theo thành phố.
     */
    @Query("SELECT h FROM HotelEntity h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "LOWER(h.city) = LOWER(:city)")
    List<HotelEntity> findByKeywordAndCity(@Param("keyword") String keyword, @Param("city") String city);

    /**
     * Keyword search kết hợp lọc theo giá tối đa.
     */
    @Query("SELECT h FROM HotelEntity h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "h.price <= :maxPrice")
    List<HotelEntity> findByKeywordAndMaxPrice(@Param("keyword") String keyword, @Param("maxPrice") Long maxPrice);

    /**
     * Keyword search kết hợp lọc theo thành phố và giá tối đa.
     */
    @Query("SELECT h FROM HotelEntity h WHERE " +
           "(LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(h.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "LOWER(h.city) = LOWER(:city) AND h.price <= :maxPrice")
    List<HotelEntity> findByKeywordAndCityAndMaxPrice(@Param("keyword") String keyword,
                                                       @Param("city") String city,
                                                       @Param("maxPrice") Long maxPrice);
}
