package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  @Test
  void testEndpointThrottleWithZeroDelay() {
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(0);

    RateLimiter rateLimiter = new RateLimiter(yahooFinanceConfig);

    assertDoesNotThrow(rateLimiter::throttleYahooFinance);
  }

  @Test
  void testThrottlePositiveDelay() {
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(20);

    RateLimiter rateLimiter = new RateLimiter(yahooFinanceConfig);

    long start = System.currentTimeMillis();
    rateLimiter.throttleYahooFinance();
    rateLimiter.throttleYahooFinance();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 15, "Expected elapsed time to be at least ~15ms, but was " + elapsed);
  }

  @Test
  void testThrottleExplicitDelayElapsedBranch() {
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(0);

    RateLimiter rateLimiter = new RateLimiter(yahooFinanceConfig);
    AtomicLong tracker = new AtomicLong(0);

    rateLimiter.throttle(tracker, 5);
    try {
      Thread.sleep(10);
    } catch (InterruptedException ignored) {
      // Ignored
    }
    assertDoesNotThrow(() -> rateLimiter.throttle(tracker, 5));
  }

  @Test
  void testThrottleInterrupted() {
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(1000);

    RateLimiter rateLimiter = new RateLimiter(yahooFinanceConfig);
    AtomicLong tracker = new AtomicLong(System.currentTimeMillis() + 500);

    Thread.currentThread().interrupt();
    assertDoesNotThrow(() -> rateLimiter.throttle(tracker, 500));
    assertTrue(Thread.interrupted(), "Expected thread interrupt flag to be preserved");
  }
}
