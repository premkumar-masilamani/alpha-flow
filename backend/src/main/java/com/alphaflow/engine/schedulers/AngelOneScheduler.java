package com.alphaflow.engine.schedulers;

import com.alphaflow.common.constants.MarketConstants;
import com.alphaflow.engine.downloaders.AngelOneDownloader;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneScheduler {

  private final AngelOneDownloader angelOneDownloader;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public AngelOneScheduler(AngelOneDownloader angelOneDownloader) {
    this.angelOneDownloader = angelOneDownloader;
  }

  @Scheduled(cron = "5 0,15,30,45 9-15 * * MON-FRI", zone = MarketConstants.TIMEZONE_KOLKATA)
  public void runScheduledIntradayUpdate() {
    if (!angelOneDownloader.hasValidCredentials()) {
      return;
    }
    log.info("Starting scheduled Angel One 15-minute intraday update cycle...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    if (!angelOneDownloader.hasValidCredentials()) {
      log.warn(
          "Angel One credentials are not configured. Intraday sync is disabled. "
              + "Refer to README.md to configure ANGEL_ONE_API_KEY, ANGEL_ONE_CLIENT_CODE, "
              + "ANGEL_ONE_PASSWORD, and ANGEL_ONE_TOTP_KEY.");
      return;
    }
    log.info("Starting initial Angel One intraday sync upon startup...");
    run();
  }

  public void run() {
    if (!angelOneDownloader.hasValidCredentials()) {
      return;
    }
    if (!running.compareAndSet(false, true)) {
      log.warn("Angel One update cycle skipped: a previous run is still in progress.");
      return;
    }

    long start = System.currentTimeMillis();
    try {
      angelOneDownloader.downloadIntradayPrices();
      log.info("Angel One update cycle completed in {} ms.", (System.currentTimeMillis() - start));
    } catch (Exception e) {
      log.error("Error occurred during Angel One update cycle", e);
    } finally {
      running.set(false);
    }
  }
}
