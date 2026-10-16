package com.alphaflow.api.services;

import com.alphaflow.api.dtos.QuoteDto;
import com.alphaflow.persistence.entities.LiveQuote;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.LiveQuoteRepository;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class QuoteService {

  private final LiveQuoteRepository liveQuoteRepository;

  public QuoteService(LiveQuoteRepository liveQuoteRepository) {
    this.liveQuoteRepository = liveQuoteRepository;
  }

  public QuoteDto getQuote(Ticker ticker) {
    Optional<LiveQuote> liveQuoteOptional = liveQuoteRepository.findByTicker(ticker);
    if (liveQuoteOptional.isPresent()) {
      LiveQuote liveQuote = liveQuoteOptional.get();
      return QuoteDto.builder()
          .currentPrice(liveQuote.getLastPrice())
          .timestamp(liveQuote.getQuoteTime().toString())
          .build();
    } else {
      log.debug("No live quote found in database for ticker: {}", ticker.getTickerSymbol());
      return QuoteDto.builder().currentPrice(BigDecimal.ZERO).timestamp("").build();
    }
  }
}
