package com.alphaflow.api.services;

import com.alphaflow.api.dtos.RenkoDataDTO;
import com.alphaflow.api.dtos.RenkoDataResponseDTO;
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
import static com.alphaflow.infrastructure.generators.RenkoDataGenerator.getZoneFromTrend;
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

    public RenkoDataResponseDTO getRenkoData(String symbol) {
        log.debug("Fetching renko data for symbol: {}", symbol);
        Ticker ticker = tickerRepository.findByTickerSymbol(symbol)
                .orElseThrow(() -> {
                    log.warn("Ticker not found for symbol: {}", symbol);
                    return new ResourceNotFoundException("Ticker not found: " + symbol);
                });

        List<RenkoData> renkoData = renkoDataRepository.findByTickerOrderByRenkoDataDateAsc(ticker);
        return calculatePricesAndCreateResponse(renkoData);
    }

    private RenkoDataResponseDTO calculatePricesAndCreateResponse(List<RenkoData> renkoData) {
        if (renkoData.isEmpty()) {
            return RenkoDataResponseDTO.builder().build();
        }

        RenkoData currentTrendBrick = getCurrentTrendBrick(renkoData);

        BigDecimal brickSize = currentTrendBrick.getBrickHigh().subtract(currentTrendBrick.getBrickLow());

        boolean isUp = currentTrendBrick.getDirection().equals(RENKO_BRICK_DIRECTION_UP);
        BigDecimal currentPrice = isUp ? currentTrendBrick.getBrickHigh() : currentTrendBrick.getBrickLow();
        BigDecimal stopLossDistance = brickSize.multiply(valueOf(getZoneFromTrend(currentTrendBrick.getTrend()) + 2), DB_MATH_CONTEXT);
        BigDecimal stopLossPrice = isUp ? currentPrice.subtract(stopLossDistance) : currentPrice.add(stopLossDistance);

        List<RenkoDataDTO> brickDTOs = renkoData.stream()
                .map(RenkoDataMapper::toDTO)
                .toList();

        return RenkoDataResponseDTO.builder()
                .bricks(brickDTOs)
                .currentPrice(currentPrice)
                .stopLossPrice(stopLossPrice)
                .brickSize(brickSize).build();
    }

    private RenkoData getCurrentTrendBrick(List<RenkoData> renkoData) {

        RenkoData latestBrick = renkoData.getLast();
        String latestDirection = latestBrick.getDirection();
        String oppositeDirection = latestDirection.equals(RENKO_BRICK_DIRECTION_UP) ? RENKO_BRICK_DIRECTION_DOWN : RENKO_BRICK_DIRECTION_UP;

        RenkoData latestOppositeBrick = null;
        for (int i = renkoData.size() - 1; i >= 0; i--) {
            if (renkoData.get(i).getDirection().equals(oppositeDirection)) {
                latestOppositeBrick = renkoData.get(i);
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
