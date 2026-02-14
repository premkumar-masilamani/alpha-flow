package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.RenkoDataDTO;
import com.alphaflow.infrastructure.entities.RenkoData;

public class RenkoDataMapper {
    public static RenkoDataDTO toDTO(RenkoData renkoEntity) {
        return RenkoDataDTO.builder()
                .date(renkoEntity.getRenkoDataDate())
                .low(renkoEntity.getBrickLow())
                .high(renkoEntity.getBrickHigh())
                .direction(renkoEntity.getDirection())
                .trend(renkoEntity.getTrend())
                .zone(renkoEntity.getZone()).build();
    }
}
