package com.alphaflow.api.dtos;

import com.alphaflow.persistence.enums.Country;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.Builder;

@Builder
public record MarketHolidayDto(
    @JsonProperty("id") Long id,
    @JsonProperty("country") Country country,
    @JsonProperty("holidayDate") LocalDate holidayDate,
    @JsonProperty("name") String name) {}
