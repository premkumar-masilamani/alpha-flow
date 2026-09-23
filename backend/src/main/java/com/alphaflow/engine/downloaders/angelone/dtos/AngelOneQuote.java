package com.alphaflow.engine.downloaders.angelone.dtos;

import java.math.BigDecimal;

public record AngelOneQuote(
    BigDecimal lastPrice,
    BigDecimal change,
    BigDecimal changePercent,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume,
    String timestamp) {}
