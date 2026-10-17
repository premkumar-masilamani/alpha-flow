package com.alphaflow.engine.strategies.options.dtos;

import com.alphaflow.common.enums.OptionType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RecommendedOptionDto(
    String tradingSymbol,
    String token,
    BigDecimal strikePrice,
    OptionType optionType,
    LocalDate expiryDate,
    long daysToExpiry,
    BigDecimal lastPrice,
    BigDecimal intrinsicValue,
    BigDecimal timeValue,
    BigDecimal delta,
    BigDecimal impliedVolatility,
    BigDecimal theta) {}
