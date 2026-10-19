package cinemanagement.util;

import java.util.Collections;
import java.util.List;

/** Lỗi nghiệp vụ: thông điệp hiển thị thẳng cho người dùng. */
public class BusinessException extends Exception {

    private final List<String> errors;

    public BusinessException(String error) {
        this(List.of(error));
    }

    public BusinessException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = Collections.unmodifiableList(errors);
    }

    public List<String> getErrors() {
        return errors;
    }
}
