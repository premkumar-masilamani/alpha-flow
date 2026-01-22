package com.alphaflow.domain.model;

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