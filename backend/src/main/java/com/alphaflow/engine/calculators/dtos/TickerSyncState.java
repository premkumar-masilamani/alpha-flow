package com.alphaflow.engine.calculators.dtos;

import com.alphaflow.persistence.entities.Ticker;
import java.time.LocalDate;

public record TickerSyncState(
    Ticker ticker, LocalDate latestSourceDate, LocalDate latestCalculatedDate) {}
