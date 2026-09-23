package com.alphaflow.engine.schedulers;

import com.alphaflow.engine.configs.AngelOneConfig;
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

  private final AngelOneConfig angelOneConfig;
  private final AngelOneDownloader angelOneDownloader;
  private final AtomicBoolean running = new AtomicBoolean(false);

  public AngelOneScheduler(AngelOneConfig angelOneConfig, AngelOneDownloader angelOneDownloader) {
    this.angelOneConfig = angelOneConfig;
    this.angelOneDownloader = angelOneDownloader;
  }

  @Scheduled(cron = "5 0,15,30,45 9-15 * * MON-FRI", zone = "Asia/Kolkata")
  public void runScheduledIntradayUpdate() {
    log.info("Starting scheduled Angel One 15-minute intraday update cycle...");
    run();
  }

  @Async
  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    if (!angelOneConfig.isEnabled()) {
      log.info("Angel One downloader is disabled on startup.");
      return;
    }
    log.info("Starting initial Angel One intraday sync upon startup...");
    run();
  }

  public void run() {
    if (!angelOneConfig.isEnabled()) {
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
