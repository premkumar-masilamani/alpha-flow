package com.alphaflow.infrastructure.persistence.mappers;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;

public class RenkoDataMapper {

    public static RenkoBrickDTO toDTO(RenkoData entity) {
        return new RenkoBrickDTO(
                entity.getRenkoDate(),
                entity.getBrickLow(),
                entity.getBrickHigh(),
                entity.getDirection(),
                entity.getTrend()
        );
    }
}
