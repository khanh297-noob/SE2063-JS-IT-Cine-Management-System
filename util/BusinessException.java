package cinemanagement.util;

/** Loi nghiep vu (vi pham business rule) - thong bao duoc hien thi truc tiep cho nguoi dung. */
public class BusinessException extends Exception {
    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
