package com.alphaflow.api.services;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.angelone.AngelOneClient;
import com.alphaflow.engine.downloaders.angelone.dtos.AngelOneQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.enums.DataProvider;
import java.math.BigDecimal;
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

  public QuoteService(AngelOneClient angelOneClient, AngelOneConfig angelOneConfig) {
    this.angelOneClient = angelOneClient;
    this.angelOneConfig = angelOneConfig;
  }

  public QuoteDto getQuote(Ticker ticker) {
    if (ticker.getDataProvider() == DataProvider.ANGEL_ONE && angelOneConfig.isEnabled()) {
      String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());
      Optional<AngelOneQuote> quoteOpt = angelOneClient.getMarketQuote("NSE", token);
      if (quoteOpt.isPresent()) {
        AngelOneQuote quote = quoteOpt.get();
        return QuoteDto.builder()
            .currentPrice(quote.lastPrice())
            .timestamp(quote.timestamp())
            .build();
      }
    }

    return QuoteDto.builder().currentPrice(BigDecimal.ZERO).timestamp("").build();
  }
}
