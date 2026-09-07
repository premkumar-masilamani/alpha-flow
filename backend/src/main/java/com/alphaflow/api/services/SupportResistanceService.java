package com.alphaflow.api.services;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class SupportResistanceService {

  private final TickerService tickerService;
  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;

  public SupportResistanceService(
      TickerService tickerService,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository) {
    this.tickerService = tickerService;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
  }

  public List<SupportResistanceDto> getSupportResistances(
      Ticker ticker, Timeframe timeframe, LocalDate date) {
    if (timeframe == Timeframe.DAILY) {
      return dailySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
          .map(this::toDto)
          .toList();
    } else if (timeframe == Timeframe.WEEKLY) {
      return weeklySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
          .map(this::toDto)
          .toList();
    } else {
      log.error("Unsupported timeframe for support resistance: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  public List<SupportResistanceDto> getDailySupportResistances(Ticker ticker, LocalDate date) {
    return getSupportResistances(ticker, Timeframe.DAILY, date);
  }

  public List<SupportResistanceDto> getDailySupportResistances(String symbol, LocalDate date) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getDailySupportResistances(ticker, date);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(Ticker ticker, LocalDate date) {
    return getSupportResistances(ticker, Timeframe.WEEKLY, date);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(String symbol, LocalDate date) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getWeeklySupportResistances(ticker, date);
  }

  private SupportResistanceDto toDto(DailySupportResistance dsr) {
    return SupportResistanceDto.builder()
        .priceDate(dsr.getPriceDate())
        .zoneBottom(dsr.getZoneBottom())
        .zoneTop(dsr.getZoneTop())
        .zoneMidpoint(dsr.getZoneMidpoint())
        .levelType(dsr.getLevelType())
        .touchCount(dsr.getTouchCount())
        .build();
  }

  private SupportResistanceDto toDto(WeeklySupportResistance wsr) {
    return SupportResistanceDto.builder()
        .priceDate(wsr.getPriceDate())
        .zoneBottom(wsr.getZoneBottom())
        .zoneTop(wsr.getZoneTop())
        .zoneMidpoint(wsr.getZoneMidpoint())
        .levelType(wsr.getLevelType())
        .touchCount(wsr.getTouchCount())
        .build();
  }
}
