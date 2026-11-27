package com.alphaflow.engine.structure.dtos;

import com.alphaflow.engine.structure.enums.SwingType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable confirmed Dow Theory swing pivot anchored at the bar's wick extreme.
 *
 * @param barIndex zero-based index of the pivot bar within the input price series
 * @param date calendar date of the pivot bar
 * @param price wick extreme price of the pivot ({@code high} for {@link SwingType#HIGH}, {@code
 *     low} for {@link SwingType#LOW})
 * @param type structural polarity of the pivot
 * @param atrAtSwing rolling Wilder ATR recorded on the pivot bar
 */
public record SwingPoint(
    int barIndex, LocalDate date, BigDecimal price, SwingType type, BigDecimal atrAtSwing) {}
