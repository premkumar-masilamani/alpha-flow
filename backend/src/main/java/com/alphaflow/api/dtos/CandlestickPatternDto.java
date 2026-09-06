package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.PatternSentiment;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record CandlestickPatternDto(
    @JsonProperty("date") LocalDate date,
    @JsonProperty("shortName") String shortName,
    @JsonProperty("longName") String longName,
    @JsonProperty("sentiment") PatternSentiment sentiment) {}
