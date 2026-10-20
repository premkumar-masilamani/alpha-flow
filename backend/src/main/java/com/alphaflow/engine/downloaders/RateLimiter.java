package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimiter {

  private final AngelOneConfig angelOneConfig;
  private final YahooFinanceConfig yahooFinanceConfig;

  private final AtomicLong angelOneLoginNextAllowedTimeMs = new AtomicLong(0);
  private final AtomicLong angelOneCandlesNextAllowedTimeMs = new AtomicLong(0);
  private final AtomicLong angelOneLiveQuoteNextAllowedTimeMs = new AtomicLong(0);
  private final AtomicLong yahooFinanceNextAllowedTimeMs = new AtomicLong(0);

  public void throttleAngelOneLogin() {
    throttle(angelOneLoginNextAllowedTimeMs, angelOneConfig.getDelayMilliseconds());
  }

  public void throttleAngelOneCandles() {
    throttle(angelOneCandlesNextAllowedTimeMs, angelOneConfig.getDelayMilliseconds());
  }

  public void throttleAngelOneLiveQuote() {
    throttle(angelOneLiveQuoteNextAllowedTimeMs, angelOneConfig.getDelayMilliseconds());
  }

  public void throttleYahooFinance() {
    throttle(yahooFinanceNextAllowedTimeMs, yahooFinanceConfig.getDelayMilliseconds());
  }

  public void throttle(AtomicLong nextAllowedTimeMs, long minDelayMs) {
    if (minDelayMs > 0) {
      long now = System.currentTimeMillis();
      long scheduledTime =
          nextAllowedTimeMs.updateAndGet(current -> Math.max(current, now) + minDelayMs);
      long waitMs = scheduledTime - minDelayMs - now;
      if (waitMs <= 0) {
        log.debug("No rate limit throttle required");
      } else {
        try {
          Thread.sleep(waitMs);
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          log.warn("Rate limiter sleep interrupted: {}", exception.getMessage());
        }
      }
    }
  }
}
