package com.alphaflow.domain.model;

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