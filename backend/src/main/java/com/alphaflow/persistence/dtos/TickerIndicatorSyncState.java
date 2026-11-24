package com.alphaflow.persistence.dtos;

import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;

public record TickerIndicatorSyncState(
    Ticker ticker,
    IndicatorDefinition indicatorDefinition,
    LocalDate latestSourceDate,
    LocalDate latestCalculatedDate) {}
