package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record RenkoResponseDTO(
        @JsonProperty("bricks") List<RenkoBrickDTO> bricks,
        @JsonProperty("current_price") BigDecimal currentPrice,
        @JsonProperty("stop_loss_price") BigDecimal stopLossPrice,
        @JsonProperty("brick_size") BigDecimal brickSize
) {
}
