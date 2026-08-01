package com.alphaflow.engine.downloaders.dtos;

import java.math.BigDecimal;
import java.util.List;

public record YahooQuote(
    List<BigDecimal> open,
    List<BigDecimal> high,
    List<BigDecimal> low,
    List<BigDecimal> close,
    List<BigDecimal> volume) {}
