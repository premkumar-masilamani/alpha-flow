package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.infrastructure.entities.Renko;

public class RenkoMapper {
    public static RenkoBrickDTO toDTO(Renko renkoEntity) {
        return RenkoBrickDTO.builder()
                .date(renkoEntity.getRenkoDate())
                .low(renkoEntity.getBrickLow())
                .high(renkoEntity.getBrickHigh())
                .direction(renkoEntity.getDirection())
                .trend(renkoEntity.getTrend())
                .zone(renkoEntity.getZone()).build();
    }
}
