package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;

public class RenkoDataMapper {
    public static RenkoBrickDTO toDTO(RenkoData renkoDataEntity) {
        return RenkoBrickDTO.builder()
                .date(renkoDataEntity.getRenkoDate())
                .low(renkoDataEntity.getBrickLow())
                .high(renkoDataEntity.getBrickHigh())
                .direction(renkoDataEntity.getDirection())
                .trend(renkoDataEntity.getTrend())
                .zone(renkoDataEntity.getZone()).build();
    }
}
