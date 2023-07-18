package io.github.coffee330501;

import lombok.Data;

import java.io.Serializable;

@Data
public class SignatureResult<T> implements Serializable {
    Integer code;
    String msg;
    T data;
}
