package com.alphaflow.api.services;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.common.constants.MarketConstants;
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

  public QuoteService(AngelOneClient angelOneClient) {
    this.angelOneClient = angelOneClient;
  }

  public QuoteDto getQuote(Ticker ticker) {
    if (ticker.getDataProvider() == DataProvider.ANGEL_ONE) {
      if (!angelOneClient.hasValidCredentials()) {
        return QuoteDto.builder().currentPrice(BigDecimal.ZERO).timestamp("").build();
      }
      String token = AngelOneClient.resolveToken(ticker.getTickerSymbol());
      Optional<AngelOneQuote> quoteOpt =
          angelOneClient.getMarketQuote(MarketConstants.EXCHANGE_NSE, token);
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
