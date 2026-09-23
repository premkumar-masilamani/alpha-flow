package com.alphaflow.api.services;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.persistence.entities.DailyPrice;
import com.alphaflow.persistence.entities.IntradayPrice;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.IntradayPriceRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class QuoteService {

  private final AngelOneClient angelOneClient;
  private final AngelOneConfig angelOneConfig;
  private final IntradayPriceRepository intradayPriceRepository;
  private final DailyPriceRepository dailyPriceRepository;

  public QuoteService(
      AngelOneClient angelOneClient,
      AngelOneConfig angelOneConfig,
      IntradayPriceRepository intradayPriceRepository,
      DailyPriceRepository dailyPriceRepository) {
    this.angelOneClient = angelOneClient;
    this.angelOneConfig = angelOneConfig;
    this.intradayPriceRepository = intradayPriceRepository;
    this.dailyPriceRepository = dailyPriceRepository;
  }

  public QuoteDto getQuote(Ticker ticker) {
    if (ticker.getDataProvider() == DataProvider.ANGEL_ONE) {
      if (angelOneConfig.isEnabled()) {
        String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());
        Optional<AngelOneQuote> quoteOpt = angelOneClient.getMarketQuote("NSE", token);
        if (quoteOpt.isPresent()) {
          AngelOneQuote q = quoteOpt.get();
          return QuoteDto.builder()
              .symbol(ticker.getTickerSymbol())
              .name(ticker.getTickerName())
              .lastPrice(q.lastPrice())
              .change(q.change())
              .changePercent(q.changePercent())
              .open(q.open())
              .high(q.high())
              .low(q.low())
              .close(q.close())
              .volume(q.volume())
              .timestamp(q.timestamp())
              .build();
        }
      }

      Optional<IntradayPrice> latestIntraday =
          intradayPriceRepository.findTopByTickerAndTimeframeOrderByPriceTimeDesc(
              ticker, "FIFTEEN_MINUTE");
      if (latestIntraday.isPresent()) {
        IntradayPrice ip = latestIntraday.get();
        BigDecimal change = ip.getPriceClose().subtract(ip.getPriceOpen());
        BigDecimal changePct = BigDecimal.ZERO;
        if (ip.getPriceOpen().compareTo(BigDecimal.ZERO) > 0) {
          changePct =
              change
                  .divide(ip.getPriceOpen(), 4, RoundingMode.HALF_UP)
                  .multiply(BigDecimal.valueOf(100));
        }
        return QuoteDto.builder()
            .symbol(ticker.getTickerSymbol())
            .name(ticker.getTickerName())
            .lastPrice(ip.getPriceClose())
            .change(change)
            .changePercent(changePct)
            .open(ip.getPriceOpen())
            .high(ip.getPriceHigh())
            .low(ip.getPriceLow())
            .close(ip.getPriceClose())
            .volume(ip.getVolume())
            .timestamp(ip.getPriceTime().toString())
            .build();
      }
    } else {
      Optional<DailyPrice> latestDaily =
          dailyPriceRepository.findTopByTickerOrderByPriceDateDesc(ticker);
      if (latestDaily.isPresent()) {
        DailyPrice dp = latestDaily.get();
        BigDecimal change = dp.getPriceClose().subtract(dp.getPriceOpen());
        BigDecimal changePct = BigDecimal.ZERO;
        if (dp.getPriceOpen().compareTo(BigDecimal.ZERO) > 0) {
          changePct =
              change
                  .divide(dp.getPriceOpen(), 4, RoundingMode.HALF_UP)
                  .multiply(BigDecimal.valueOf(100));
        }
        return QuoteDto.builder()
            .symbol(ticker.getTickerSymbol())
            .name(ticker.getTickerName())
            .lastPrice(dp.getPriceClose())
            .change(change)
            .changePercent(changePct)
            .open(dp.getPriceOpen())
            .high(dp.getPriceHigh())
            .low(dp.getPriceLow())
            .close(dp.getPriceClose())
            .volume(dp.getVolume())
            .timestamp(dp.getPriceDate().toString())
            .build();
      }
    }

    return QuoteDto.builder()
        .symbol(ticker.getTickerSymbol())
        .name(ticker.getTickerName())
        .lastPrice(BigDecimal.ZERO)
        .change(BigDecimal.ZERO)
        .changePercent(BigDecimal.ZERO)
        .open(BigDecimal.ZERO)
        .high(BigDecimal.ZERO)
        .low(BigDecimal.ZERO)
        .close(BigDecimal.ZERO)
        .volume(BigDecimal.ZERO)
        .timestamp("")
        .build();
  }
}
