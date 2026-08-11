package com.alphaflow.api.services;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SupportResistanceService {

  private final TickerRepository tickerRepository;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;

  public SupportResistanceService(
      TickerRepository tickerRepository,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository) {
    this.tickerRepository = tickerRepository;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
  }

  public List<SupportResistanceDto> getDailySupportResistances(String symbol, LocalDate date) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbol(symbol)
            .orElseThrow(() -> new IllegalArgumentException("Ticker not found: " + symbol));

    List<DailySupportResistance> results =
        dailySupportResistanceRepository.findByTickerAndPriceDate(ticker, date);

    return results.stream()
        .map(
            dsr ->
                SupportResistanceDto.builder()
                    .priceDate(dsr.getPriceDate())
                    .zoneBottom(dsr.getZoneBottom())
                    .zoneTop(dsr.getZoneTop())
                    .zoneMidpoint(dsr.getZoneMidpoint())
                    .levelType(dsr.getLevelType())
                    .touchCount(dsr.getTouchCount())
                    .build())
        .collect(Collectors.toList());
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(String symbol, LocalDate date) {
    Ticker ticker =
        tickerRepository
            .findByTickerSymbol(symbol)
            .orElseThrow(() -> new IllegalArgumentException("Ticker not found: " + symbol));

    List<WeeklySupportResistance> results =
        weeklySupportResistanceRepository.findByTickerAndPriceDate(ticker, date);

    return results.stream()
        .map(
            wsr ->
                SupportResistanceDto.builder()
                    .priceDate(wsr.getPriceDate())
                    .zoneBottom(wsr.getZoneBottom())
                    .zoneTop(wsr.getZoneTop())
                    .zoneMidpoint(wsr.getZoneMidpoint())
                    .levelType(wsr.getLevelType())
                    .touchCount(wsr.getTouchCount())
                    .build())
        .collect(Collectors.toList());
  }
}
