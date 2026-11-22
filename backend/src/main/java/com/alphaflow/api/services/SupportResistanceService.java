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

  private final DailySupportResistanceRepository dailySupportResistanceRepository;
  private final WeeklySupportResistanceRepository weeklySupportResistanceRepository;

  public SupportResistanceService(
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository) {
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
  }

  public List<SupportResistanceDto> getSupportResistances(
      Ticker ticker, Timeframe timeframe, LocalDate date) {
    log.debug(
        "Fetching {} S&R zones for ticker {} on date {}",
        timeframe,
        ticker.getTickerSymbol(),
        date);
    if (timeframe == Timeframe.DAILY) {
      List<SupportResistanceDto> zones =
          dailySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
              .map(this::toDto)
              .toList();
      log.debug(
          "Retrieved {} DAILY S&R zones for ticker {} on {}",
          zones.size(),
          ticker.getTickerSymbol(),
          date);
      return zones;
    } else if (timeframe == Timeframe.WEEKLY) {
      List<SupportResistanceDto> zones =
          weeklySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
              .map(this::toDto)
              .toList();
      log.debug(
          "Retrieved {} WEEKLY S&R zones for ticker {} on {}",
          zones.size(),
          ticker.getTickerSymbol(),
          date);
      return zones;
    } else {
      log.error("Unsupported timeframe for support resistance: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  private SupportResistanceDto toDto(DailySupportResistance dailySupportResistance) {
    return SupportResistanceDto.builder()
        .priceDate(dailySupportResistance.getPriceDate())
        .firstTouchDate(dailySupportResistance.getFirstTouchDate())
        .lastTouchDate(dailySupportResistance.getLastTouchDate())
        .zoneBottom(dailySupportResistance.getZoneBottom())
        .zoneTop(dailySupportResistance.getZoneTop())
        .zoneMidpoint(dailySupportResistance.getZoneMidpoint())
        .levelType(dailySupportResistance.getLevelType())
        .touchCount(dailySupportResistance.getTouchCount())
        .build();
  }

  private SupportResistanceDto toDto(WeeklySupportResistance weeklySupportResistance) {
    return SupportResistanceDto.builder()
        .priceDate(weeklySupportResistance.getPriceDate())
        .firstTouchDate(weeklySupportResistance.getFirstTouchDate())
        .lastTouchDate(weeklySupportResistance.getLastTouchDate())
        .zoneBottom(weeklySupportResistance.getZoneBottom())
        .zoneTop(weeklySupportResistance.getZoneTop())
        .zoneMidpoint(weeklySupportResistance.getZoneMidpoint())
        .levelType(weeklySupportResistance.getLevelType())
        .touchCount(weeklySupportResistance.getTouchCount())
        .build();
  }
}
