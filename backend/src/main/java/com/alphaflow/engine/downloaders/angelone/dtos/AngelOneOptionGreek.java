package com.alphaflow.engine.downloaders.angelone.dtos;

import com.alphaflow.common.enums.OptionType;
import java.math.BigDecimal;

public record AngelOneOptionGreek(
    BigDecimal strikePrice,
    OptionType optionType,
    BigDecimal delta,
    BigDecimal gamma,
    BigDecimal theta,
    BigDecimal vega,
    BigDecimal impliedVolatility,
    BigDecimal tradeVolume) {}
