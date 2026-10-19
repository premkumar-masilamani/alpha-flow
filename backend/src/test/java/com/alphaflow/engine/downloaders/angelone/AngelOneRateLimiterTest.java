package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.alphaflow.engine.configs.AngelOneConfig;
import org.junit.jupiter.api.Test;

class AngelOneRateLimiterTest {

  @Test
  void testThrottleZeroDelay() {
    AngelOneConfig config = new AngelOneConfig();
    config.setDelayMilliseconds(0);
    AngelOneRateLimiter rateLimiter = new AngelOneRateLimiter(config);

    assertDoesNotThrow(() -> rateLimiter.throttle());
  }

  @Test
  void testThrottlePositiveDelay() {
    AngelOneConfig config = new AngelOneConfig();
    config.setDelayMilliseconds(20);
    AngelOneRateLimiter rateLimiter = new AngelOneRateLimiter(config);

    long start = System.currentTimeMillis();
    rateLimiter.throttle();
    rateLimiter.throttle();
    long elapsed = System.currentTimeMillis() - start;

    assertTrue(elapsed >= 15, "Expected elapsed time to be at least ~15ms, but was " + elapsed);
  }

  @Test
  void testThrottleExplicitDelayElapsedBranch() {
    AngelOneConfig config = new AngelOneConfig();
    config.setDelayMilliseconds(0);
    AngelOneRateLimiter rateLimiter = new AngelOneRateLimiter(config);

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
    AngelOneConfig config = new AngelOneConfig();
    config.setDelayMilliseconds(1000);
    AngelOneRateLimiter rateLimiter = new AngelOneRateLimiter(config);

    Thread.currentThread().interrupt();
    assertDoesNotThrow(() -> rateLimiter.throttle(500));
    assertTrue(Thread.interrupted(), "Expected thread interrupt flag to be preserved");
  }
}
