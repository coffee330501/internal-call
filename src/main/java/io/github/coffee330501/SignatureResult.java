package io.github.coffee330501;

import lombok.Data;

@Data
public class SignatureResult<T> {
    Integer code;
    String msg;
    T data;
}
