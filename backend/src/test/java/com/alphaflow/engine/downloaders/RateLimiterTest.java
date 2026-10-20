package com.alphaflow.engine.downloaders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.configs.YahooFinanceConfig;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  @Test
  void testAllEndpointThrottlesWithZeroDelay() {
    AngelOneConfig angelOneConfig = new AngelOneConfig();
    angelOneConfig.setDelayMilliseconds(0);
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(0);

    RateLimiter rateLimiter = new RateLimiter(angelOneConfig, yahooFinanceConfig);

    assertDoesNotThrow(rateLimiter::throttleAngelOneLogin);
    assertDoesNotThrow(rateLimiter::throttleAngelOneCandles);
    assertDoesNotThrow(rateLimiter::throttleAngelOneLiveQuote);
    assertDoesNotThrow(rateLimiter::throttleAngelOneOptionGreeks);
    assertDoesNotThrow(rateLimiter::throttleYahooFinance);
  }

  @Test
  void testThrottlePositiveDelay() {
    AngelOneConfig angelOneConfig = new AngelOneConfig();
    angelOneConfig.setDelayMilliseconds(20);
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(20);

    RateLimiter rateLimiter = new RateLimiter(angelOneConfig, yahooFinanceConfig);

    long start = System.currentTimeMillis();
    rateLimiter.throttleAngelOneCandles();
    rateLimiter.throttleAngelOneCandles();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 15, "Expected elapsed time to be at least ~15ms, but was " + elapsed);
  }

  @Test
  void testThrottleExplicitDelayElapsedBranch() {
    AngelOneConfig angelOneConfig = new AngelOneConfig();
    angelOneConfig.setDelayMilliseconds(0);
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(0);

    RateLimiter rateLimiter = new RateLimiter(angelOneConfig, yahooFinanceConfig);
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
    AngelOneConfig angelOneConfig = new AngelOneConfig();
    angelOneConfig.setDelayMilliseconds(1000);
    YahooFinanceConfig yahooFinanceConfig = new YahooFinanceConfig();
    yahooFinanceConfig.setDelayMilliseconds(1000);

    RateLimiter rateLimiter = new RateLimiter(angelOneConfig, yahooFinanceConfig);
    AtomicLong tracker = new AtomicLong(System.currentTimeMillis() + 500);

    Thread.currentThread().interrupt();
    assertDoesNotThrow(() -> rateLimiter.throttle(tracker, 500));
    assertTrue(Thread.interrupted(), "Expected thread interrupt flag to be preserved");
  }
}
