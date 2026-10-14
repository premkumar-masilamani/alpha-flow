package com.alphaflow.api.services;

import com.alphaflow.api.dtos.SupportResistanceDto;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.DailySupportResistance;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.entities.WeeklyPrice;
import com.alphaflow.persistence.entities.WeeklySupportResistance;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.DailySupportResistanceRepository;
import com.alphaflow.persistence.repositories.WeeklyPriceRepository;
import com.alphaflow.persistence.repositories.WeeklySupportResistanceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
  private final DailyPriceRepository dailyPriceRepository;
  private final WeeklyPriceRepository weeklyPriceRepository;

  public SupportResistanceService(
      TickerService tickerService,
      DailySupportResistanceRepository dailySupportResistanceRepository,
      WeeklySupportResistanceRepository weeklySupportResistanceRepository,
      DailyPriceRepository dailyPriceRepository,
      WeeklyPriceRepository weeklyPriceRepository) {
    this.tickerService = tickerService;
    this.dailySupportResistanceRepository = dailySupportResistanceRepository;
    this.weeklySupportResistanceRepository = weeklySupportResistanceRepository;
    this.dailyPriceRepository = dailyPriceRepository;
    this.weeklyPriceRepository = weeklyPriceRepository;
  }

  public List<SupportResistanceDto> getSupportResistances(
      Ticker ticker, Timeframe timeframe, LocalDate date) {
    return getSupportResistances(ticker, timeframe, date, 0.25);
  }

  public List<SupportResistanceDto> getSupportResistances(
      Ticker ticker, Timeframe timeframe, LocalDate date, Double rangePct) {
    if (rangePct != null
        && (rangePct.isNaN() || rangePct.isInfinite() || rangePct <= 0.0 || rangePct > 5.0)) {
      log.error("Invalid rangePct: {}", rangePct);
      throw new IllegalArgumentException(
          "rangePct must be a positive finite number between 0 and 5.0: " + rangePct);
    }

    if (timeframe == Timeframe.DAILY) {
      if (rangePct != null) {
        Optional<DailyPrice> dailyPriceOptional =
            dailyPriceRepository.findByTickerAndPriceDate(ticker, date);
        if (dailyPriceOptional.isPresent()) {
          DailyPrice dailyPrice = dailyPriceOptional.get();
          BigDecimal[] bounds =
              calculateMidpointBounds(
                  dailyPrice.getPriceHigh(),
                  dailyPrice.getPriceLow(),
                  dailyPrice.getPriceClose(),
                  rangePct);

          return dailySupportResistanceRepository
              .findByTickerAndPriceDateAndZoneMidpointBetween(ticker, date, bounds[0], bounds[1])
              .stream()
              .map(this::toDto)
              .toList();
        } else {
          log.warn(
              "Daily price candle not found for ticker {} on date {}. Returning all levels without window filter.",
              ticker.getTickerSymbol(),
              date);
          return dailySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
              .map(this::toDto)
              .toList();
        }
      } else {
        return dailySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
            .map(this::toDto)
            .toList();
      }
    } else if (timeframe == Timeframe.WEEKLY) {
      if (rangePct != null) {
        Optional<WeeklyPrice> weeklyPriceOptional =
            weeklyPriceRepository.findByTickerAndPriceDate(ticker, date);
        if (weeklyPriceOptional.isPresent()) {
          WeeklyPrice weeklyPrice = weeklyPriceOptional.get();
          BigDecimal[] bounds =
              calculateMidpointBounds(
                  weeklyPrice.getPriceHigh(),
                  weeklyPrice.getPriceLow(),
                  weeklyPrice.getPriceClose(),
                  rangePct);

          return weeklySupportResistanceRepository
              .findByTickerAndPriceDateAndZoneMidpointBetween(ticker, date, bounds[0], bounds[1])
              .stream()
              .map(this::toDto)
              .toList();
        } else {
          log.warn(
              "Weekly price candle not found for ticker {} on date {}. Returning all levels without window filter.",
              ticker.getTickerSymbol(),
              date);
          return weeklySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
              .map(this::toDto)
              .toList();
        }
      } else {
        return weeklySupportResistanceRepository.findByTickerAndPriceDate(ticker, date).stream()
            .map(this::toDto)
            .toList();
      }
    } else {
      log.error("Unsupported timeframe for support resistance: {}", timeframe);
      throw new IllegalArgumentException("Unsupported timeframe: " + timeframe);
    }
  }

  public List<SupportResistanceDto> getDailySupportResistances(Ticker ticker, LocalDate date) {
    return getSupportResistances(ticker, Timeframe.DAILY, date);
  }

  public List<SupportResistanceDto> getDailySupportResistances(
      Ticker ticker, LocalDate date, Double rangePct) {
    return getSupportResistances(ticker, Timeframe.DAILY, date, rangePct);
  }

  public List<SupportResistanceDto> getDailySupportResistances(String symbol, LocalDate date) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getDailySupportResistances(ticker, date);
  }

  public List<SupportResistanceDto> getDailySupportResistances(
      String symbol, LocalDate date, Double rangePct) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getDailySupportResistances(ticker, date, rangePct);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(Ticker ticker, LocalDate date) {
    return getSupportResistances(ticker, Timeframe.WEEKLY, date);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(
      Ticker ticker, LocalDate date, Double rangePct) {
    return getSupportResistances(ticker, Timeframe.WEEKLY, date, rangePct);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(String symbol, LocalDate date) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getWeeklySupportResistances(ticker, date);
  }

  public List<SupportResistanceDto> getWeeklySupportResistances(
      String symbol, LocalDate date, Double rangePct) {
    Ticker ticker = tickerService.getTicker(symbol);
    return getWeeklySupportResistances(ticker, date, rangePct);
  }

  private BigDecimal[] calculateMidpointBounds(
      BigDecimal priceHigh, BigDecimal priceLow, BigDecimal priceClose, Double rangePct) {
    BigDecimal typicalPrice =
        priceHigh
            .add(priceLow)
            .add(priceClose)
            .divide(BigDecimal.valueOf(3), 12, RoundingMode.HALF_UP);
    BigDecimal rangeMultiplier = BigDecimal.valueOf(rangePct);
    BigDecimal minMidpoint = typicalPrice.multiply(BigDecimal.ONE.subtract(rangeMultiplier));
    BigDecimal maxMidpoint = typicalPrice.multiply(BigDecimal.ONE.add(rangeMultiplier));
    return new BigDecimal[] {minMidpoint, maxMidpoint};
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
