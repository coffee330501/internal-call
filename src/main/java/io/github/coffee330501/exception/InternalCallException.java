package io.github.coffee330501.exception;

public class InternalCallException extends Exception {
    private String message;

    public InternalCallException(String message) {
        super(message);
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
