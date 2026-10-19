package com.alphaflow.engine.downloaders.yahoofinance;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import org.junit.jupiter.api.Test;

class YahooFinanceRateLimiterTest {

  @Test
  void testThrottleZeroDelay() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDelayMilliseconds(0);
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);

    assertDoesNotThrow(() -> rateLimiter.throttle());
  }

  @Test
  void testThrottlePositiveDelay() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDelayMilliseconds(20);
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);

    long start = System.currentTimeMillis();
    rateLimiter.throttle();
    rateLimiter.throttle();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 15, "Expected elapsed time to be at least ~15ms, but was " + elapsed);
  }

  @Test
  void testThrottleExplicitDelayElapsedBranch() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDelayMilliseconds(0);
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);

    rateLimiter.throttle(5);
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
      // Ignored
    }
    assertDoesNotThrow(() -> rateLimiter.throttle(5));
  }

  @Test
  void testThrottleInterrupted() {
    YahooFinanceConfig config = new YahooFinanceConfig();
    config.setDelayMilliseconds(1000);
    YahooFinanceRateLimiter rateLimiter = new YahooFinanceRateLimiter(config);

    Thread.currentThread().interrupt();
    assertDoesNotThrow(() -> rateLimiter.throttle(500));
    assertTrue(Thread.interrupted(), "Expected thread interrupt flag to be preserved");
  }
}
