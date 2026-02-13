package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.infrastructure.entities.RenkoData;

public class RenkoBrickMapper {
    public static RenkoBrickDTO toDTO(RenkoData renkoEntity) {
        return RenkoBrickDTO.builder()
                .date(renkoEntity.getRenkoBrickDate())
                .low(renkoEntity.getBrickLow())
                .high(renkoEntity.getBrickHigh())
                .direction(renkoEntity.getDirection())
                .trend(renkoEntity.getTrend())
                .zone(renkoEntity.getZone()).build();
    }
}
