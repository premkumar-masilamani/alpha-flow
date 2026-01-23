package com.alphaflow.domain.service;

import com.alphaflow.api.dtos.RenkoBrickDTO;
import com.alphaflow.api.dtos.RenkoResponseDTO;
import com.alphaflow.infrastructure.persistence.entities.RenkoData;
import com.alphaflow.infrastructure.persistence.entities.Ticker;
import com.alphaflow.infrastructure.persistence.mappers.RenkoDataMapper;
import com.alphaflow.infrastructure.persistence.repositories.RenkoDataRepository;
import com.alphaflow.infrastructure.persistence.repositories.TickerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.alphaflow.infrastructure.config.Constants.DB_MATH_CONTEXT;
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
                .orElseThrow(() -> new RuntimeException("Ticker not found: " + symbol));

        List<RenkoData> bricks = renkoDataRepository.findByTickerOrderByRenkoDateAscRenkoDataIdAsc(ticker);
        if (bricks.isEmpty()) {
            return new RenkoResponseDTO(List.of(), BigDecimal.ZERO, BigDecimal.ZERO);
        }

        List<RenkoBrickDTO> brickDTOs = bricks.stream()
                .map(RenkoDataMapper::toDTO)
                .toList();

        Prices prices = calculatePrices(bricks);

        return new RenkoResponseDTO(brickDTOs, prices.currentPrice(), prices.slPrice());
    }

    private record Prices(BigDecimal currentPrice, BigDecimal slPrice) {}

    private Prices calculatePrices(List<RenkoData> bricks) {
        if (bricks.isEmpty()) {
            return new Prices(BigDecimal.ZERO, BigDecimal.ZERO);
        }

        RenkoData latestBrick = bricks.get(bricks.size() - 1);
        String latestDirection = latestBrick.getDirection();
        String oppositeDirection = latestDirection.equals("up") ? "down" : "up";

        RenkoData latestOppositeBrick = null;
        for (int i = bricks.size() - 1; i >= 0; i--) {
            if (bricks.get(i).getDirection().equals(oppositeDirection)) {
                latestOppositeBrick = bricks.get(i);
                break;
            }
        }

        RenkoData currentBrick;
        if (latestOppositeBrick == null) {
            currentBrick = latestBrick;
        } else {
            int latestBrickTrend = latestBrick.getTrend();
            int latestOppositeBrickZone = getZoneFromTrend(latestOppositeBrick.getTrend());
            if (latestBrickTrend > (latestOppositeBrickZone + 1)) {
                currentBrick = latestBrick;
            } else {
                currentBrick = latestOppositeBrick;
            }
        }

        int currentBrickZone = getZoneFromTrend(currentBrick.getTrend());
        BigDecimal brickSize = currentBrick.getBrickHigh().subtract(currentBrick.getBrickLow());

        BigDecimal currentPrice;
        BigDecimal slPrice;

        if (currentBrick.getDirection().equals("up")) {
            currentPrice = currentBrick.getBrickHigh();
            slPrice = currentPrice.subtract(brickSize.multiply(valueOf(currentBrickZone + 2), DB_MATH_CONTEXT), DB_MATH_CONTEXT);
        } else {
            currentPrice = currentBrick.getBrickLow();
            slPrice = currentPrice.add(brickSize.multiply(valueOf(currentBrickZone + 2), DB_MATH_CONTEXT), DB_MATH_CONTEXT);
        }

        return new Prices(currentPrice, slPrice);
    }

    private int getZoneFromTrend(int trend) {
        if (trend <= 3) return 0;
        if (trend <= 9) return 1;
        if (trend <= 27) return 2;
        if (trend <= 81) return 3;
        return 4;
    }
}
