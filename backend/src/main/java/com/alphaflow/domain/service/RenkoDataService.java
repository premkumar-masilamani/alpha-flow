package com.alphaflow.domain.service;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.domain.exceptions.ResourceNotFoundException;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.mappers.RenkoDataMapper;
import com.alphaflow.infrastructure.persistence.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.alphaflow.infrastructure.config.Constants.*;
import static com.alphaflow.infrastructure.util.RenkoUtil.getZoneFromTrend;
import static java.math.BigDecimal.valueOf;

@Service
@Transactional(readOnly = true)
public class RenkoDataService {

    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataService(RenkoDataRepository renkoDataRepository, TickerRepository tickerRepository) {
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    public RenkoResponseDTO getRenkoData(String symbol) {
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> new ResourceNotFoundException("Ticker not found: " + symbol));

        List<RenkoData> renkoBricks = renkoDataRepository.findByTickerOrderByRenkoDateAsc(ticker);
        return calculatePricesAndCreateResponse(renkoBricks);
    }

    private RenkoResponseDTO calculatePricesAndCreateResponse(List<RenkoData> renkoBricks) {
        if (renkoBricks.isEmpty()) {
            return new RenkoResponseDTO(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        RenkoData currentTrendBrick = getCurrentTrendBrick(renkoBricks);

        int currentBrickZone = getZoneFromTrend(currentTrendBrick.getTrend());
        BigDecimal brickSize = currentTrendBrick.getBrickHigh().subtract(currentTrendBrick.getBrickLow());

        BigDecimal currentPrice;
        BigDecimal stopLossPrice;
        BigDecimal stopLossDistance = brickSize.multiply(valueOf(currentBrickZone + 2), DB_MATH_CONTEXT);

        if (currentTrendBrick.getDirection().equals(RENKO_BRICK_DIRECTION_UP)) {
            currentPrice = currentTrendBrick.getBrickHigh();
            stopLossPrice = currentPrice.subtract(stopLossDistance, DB_MATH_CONTEXT);
        } else {
            currentPrice = currentTrendBrick.getBrickLow();
            stopLossPrice = currentPrice.add(stopLossDistance, DB_MATH_CONTEXT);
        }

        List<RenkoBrickDTO> brickDTOs = renkoBricks.stream()
                .map(RenkoDataMapper::toDTO)
                .toList();

        return new RenkoResponseDTO(brickDTOs, currentPrice, stopLossPrice, brickSize);
    }

    private RenkoData getCurrentTrendBrick(List<RenkoData> renkoBricks) {

        RenkoData latestBrick = renkoBricks.getLast();
        String latestDirection = latestBrick.getDirection();
        String oppositeDirection = latestDirection.equals(RENKO_BRICK_DIRECTION_UP) ? RENKO_BRICK_DIRECTION_DOWN : RENKO_BRICK_DIRECTION_UP;

        RenkoData latestOppositeBrick = null;
        for (int i = renkoBricks.size() - 1; i >= 0; i--) {
            if (renkoBricks.get(i).getDirection().equals(oppositeDirection)) {
                latestOppositeBrick = renkoBricks.get(i);
                break;
            }
        }

        RenkoData currentTrendBrick;
        if (latestOppositeBrick == null) {
            currentTrendBrick = latestBrick;
        } else {
            int latestBrickTrend = latestBrick.getTrend();
            int latestOppositeBrickZone = getZoneFromTrend(latestOppositeBrick.getTrend());
            if (latestBrickTrend > (latestOppositeBrickZone + 1)) {
                currentTrendBrick = latestBrick;
            } else {
                currentTrendBrick = latestOppositeBrick;
            }
        }
        return currentTrendBrick;
    }
}
