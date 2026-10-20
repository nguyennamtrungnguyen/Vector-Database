package iuh.fit.backend.exception;

/**
 * Ném ra khi không tìm thấy khách sạn theo ID trong DB.
 */
public class HotelNotFoundException extends RuntimeException {

    public HotelNotFoundException(String hotelId) {
        super("Không tìm thấy khách sạn với id: " + hotelId);
    }
}
