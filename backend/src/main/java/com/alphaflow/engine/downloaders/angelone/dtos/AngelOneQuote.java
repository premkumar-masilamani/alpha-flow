package com.alphaflow.engine.downloaders.angelone.dtos;

import java.math.BigDecimal;

public record AngelOneQuote(BigDecimal lastPrice, String timestamp) {}
