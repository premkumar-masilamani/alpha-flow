package com.alphaflow.engine.downloaders.angelone.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AngelOneInstrument(
    String token,
    String symbol,
    String name,
    LocalDate expiry,
    BigDecimal strike,
    int lotSize,
    String instrumentType,
    String exchange) {}
