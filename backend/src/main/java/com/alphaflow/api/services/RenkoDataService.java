package com.alphaflow.api.services;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.api.mappers.RenkoDataMapper;
import com.alphaflow.infrastructure.entities.RenkoData;
import com.alphaflow.infrastructure.entities.Ticker;
import com.alphaflow.infrastructure.exceptions.ResourceNotFoundException;
import com.alphaflow.infrastructure.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.repositories.TickerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.alphaflow.infrastructure.constants.AppConstants.*;
import static com.alphaflow.infrastructure.generators.RenkoBricksGenerator.getZoneFromTrend;
import static java.math.BigDecimal.valueOf;

@Service
@Transactional(readOnly = true)
public class RenkoDataService {

    private static final Logger log = LoggerFactory.getLogger(RenkoDataService.class);

    private final RenkoDataRepository renkoDataRepository;
    private final TickerRepository tickerRepository;

    public RenkoDataService(RenkoDataRepository renkoDataRepository, TickerRepository tickerRepository) {
        this.renkoDataRepository = renkoDataRepository;
        this.tickerRepository = tickerRepository;
    }

    public RenkoResponseDTO getRenkoData(String symbol) {
        log.debug("Fetching renko data for symbol: {}", symbol);
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> {
                    log.warn("Ticker not found for symbol: {}", symbol);
                    return new ResourceNotFoundException("Ticker not found: " + symbol);
                });

        List<RenkoData> renkoBricks = renkoDataRepository.findByTickerOrderByRenkoDateAsc(ticker);
        return calculatePricesAndCreateResponse(renkoBricks);
    }

    private RenkoResponseDTO calculatePricesAndCreateResponse(List<RenkoData> renkoBricks) {
        if (renkoBricks.isEmpty()) {
            return RenkoResponseDTO.builder().build();
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

        return RenkoResponseDTO.builder()
                .bricks(brickDTOs)
                .currentPrice(currentPrice)
                .stopLossPrice(stopLossPrice)
                .brickSize(brickSize).build();
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
