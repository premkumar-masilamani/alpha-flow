package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.persistence.enums.CandlestickPattern;
import java.time.LocalDate;

public record PatternMatch(LocalDate date, CandlestickPattern pattern) {}
