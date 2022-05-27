package org.timsoft.utils;

public class ProberException extends RuntimeException {
    public ProberException() {
        super();
    }

    public ProberException(String msg) {
        super(msg);
    }

    public ProberException(String msg, Exception e) {
        super(msg, e);
    }
}