package cinemanagement.util;

import java.io.Serializable;

public class Flash implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String type;
    private final String message;

    public Flash(String type, String message) {
        this.type = type;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }
}
