package com.alphaflow.domain.enums;

public enum TransformationType {

    SMA("SMA"),
    EMA("EMA");

    private final String code;

    TransformationType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}