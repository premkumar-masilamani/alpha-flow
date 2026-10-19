package com.alphaflow.engine.downloaders.yahoofinance;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class YahooFinanceRateLimiter {

  private final YahooFinanceConfig config;
  private final AtomicLong nextAllowedTimeMs = new AtomicLong(0);

  public void throttle() {
    throttle(config.getDelayMilliseconds());
  }

  public void throttle(long minDelayMs) {
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
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          log.warn("Rate limiter sleep interrupted: {}", e.getMessage());
        }
      }
    }
  }
}
