package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;

public class RenkoDataMapper {
    public static RenkoBrickDTO toDTO(RenkoData renkoDataEntity) {
        return new RenkoBrickDTO(
                renkoDataEntity.getRenkoDate(),
                renkoDataEntity.getBrickLow(),
                renkoDataEntity.getBrickHigh(),
                renkoDataEntity.getDirection(),
                renkoDataEntity.getTrend(),
                renkoDataEntity.getZone()
        );
    }
}
