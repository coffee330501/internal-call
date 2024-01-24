package io.github.coffee330501.exception;

public class InternalCallException extends RuntimeException {
    private String message;
    private Integer code;

    public InternalCallException(){

    }

    public InternalCallException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    public InternalCallException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
