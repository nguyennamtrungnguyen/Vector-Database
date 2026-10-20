package iuh.fit.backend.exception;

public class HotelAlreadyExistsException extends RuntimeException {

    public HotelAlreadyExistsException(String hotelId) {
        super("Khách sạn đã tồn tại với id: " + hotelId);
    }
}
