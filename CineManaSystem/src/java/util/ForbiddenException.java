package util;

/** Người dùng không có quyền với chi nhánh / chức năng này. BaseServlet chuyển sang /access-denied. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
