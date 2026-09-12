package com.alphaflow.engine.calculators.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExtremaPoint(int index, LocalDate date, BigDecimal price, boolean isHigh) {}
