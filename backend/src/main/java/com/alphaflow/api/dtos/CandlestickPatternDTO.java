package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.PatternSentiment;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Builder;

/** Data transfer object representing a detected candlestick pattern occurrence. */
@Builder
public record CandlestickPatternDTO(
    @JsonProperty("date") LocalDate date,
    @JsonProperty("shortName") String shortName,
    @JsonProperty("longName") String longName,
    @JsonProperty("sentiment") PatternSentiment sentiment) {}
