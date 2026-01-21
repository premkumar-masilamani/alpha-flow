package com.alphaflow.core.models;

public enum MovingAverageType {

    SMA("SMA"),
    EMA("EMA");

    private final String code;

    MovingAverageType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}