package com.alphaflow.engine.enums;

public enum TransformationType {

    SMA("SMA"),
    EMA("EMA"),
    OBV("OBV"),
    CAP_MOM("CAP_MOM");

    private final String code;

    TransformationType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}