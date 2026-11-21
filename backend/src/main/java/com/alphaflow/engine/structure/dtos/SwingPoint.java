package com.alphaflow.engine.structure.dtos;

import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SwingPoint(
    int barIndex,
    LocalDate date,
    BigDecimal price,
    SwingType type,
    BigDecimal atrAtSwing) {}
