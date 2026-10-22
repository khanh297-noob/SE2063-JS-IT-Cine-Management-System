package util;

/** Không tìm thấy bản ghi theo id trên URL. BaseServlet trả về 404. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
