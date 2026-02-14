package com.alphaflow.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record RenkoDataResponseDTO(
        @JsonProperty("bricks") List<RenkoDataDTO> bricks,
        @JsonProperty("current_price") BigDecimal currentPrice,
        @JsonProperty("stop_loss_price") BigDecimal stopLossPrice,
        @JsonProperty("brick_size") BigDecimal brickSize
) {
}
