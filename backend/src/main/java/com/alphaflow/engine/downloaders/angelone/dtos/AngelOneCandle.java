package com.alphaflow.engine.downloaders.angelone.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AngelOneCandle(
    OffsetDateTime timestamp,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume) {}
