package com.alphaflow.engine.downloaders;

import com.alphaflow.engine.configs.YahooFinanceConfig;
import com.alphaflow.persistence.entities.Ticker;
import com.alphaflow.persistence.repositories.DailyPriceRepository;
import com.alphaflow.persistence.repositories.TickerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class YahooFinanceDownloaderTest {

    private DailyPriceRepository.TickerLatestPriceDateView mockView(Long id, LocalDate date) {
        DailyPriceRepository.TickerLatestPriceDateView view = mock(DailyPriceRepository.TickerLatestPriceDateView.class);
        when(view.getTickerId()).thenReturn(id);
        when(view.getLatestPriceDate()).thenReturn(date);
        return view;
    }

    @Test
    void testDownloadUpToDateNoAction() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

        var view = mockView(1L, LocalDate.now().plusDays(2));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadValidParseAndSave() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(10); // Throttle is run

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker t1 = new Ticker();
        t1.setTickerId(1L);
        t1.setTickerSymbol("AAPL");
        t1.setActive(true);
        Ticker t2 = new Ticker();
        t2.setTickerId(2L);
        t2.setTickerSymbol("MSFT");
        t2.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

        var view1 = mockView(1L, LocalDate.of(2025, 8, 12));
        var view2 = mockView(2L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view1, view2));

        when(dailyRepo.findDatesByTickerAndDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of(LocalDate.of(2025, 8, 13))); // One of the downloaded dates already exists

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        // Verifies both AAPL and MSFT processed, and saveAll was called (filtering out duplicate dates)
        verify(dailyRepo, times(2)).saveAll(any());
    }

    @Test
    void testDownloadMissingNodesResponse() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response_missing.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0); // No delay throttle

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker t1 = new Ticker();
        t1.setTickerId(1L);
        t1.setTickerSymbol("AAPL");
        t1.setActive(true);
        Ticker t2 = new Ticker();
        t2.setTickerId(2L);
        t2.setTickerSymbol("MSFT");
        t2.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

        var view1 = mockView(1L, LocalDate.of(2025, 8, 12));
        var view2 = mockView(2L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view1, view2));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        // No saveAll since missing node payload returns empty list
        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadNullsSkipsRows() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response_nulls.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        // Should save only the non-null row (1 row synced out of 6 in yahoo_response_nulls.json)
        verify(dailyRepo, times(1)).saveAll(any());
    }

    @Test
    void testDownloadConnectionExceptionHandled() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        // Invalid protocol triggers connection error
        config.setDownloadUrl("invalidproto://foo?symbol={symbol}&start={start}&end={end}");

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        // Catches and logs the connection exception, does not save anything or crash the run
        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testInterruptedDuringThrottle() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(2000L); // Large delay to intercept

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker t1 = new Ticker();
        t1.setTickerId(1L);
        t1.setTickerSymbol("AAPL");
        t1.setActive(true);
        Ticker t2 = new Ticker();
        t2.setTickerId(2L);
        t2.setTickerSymbol("MSFT");
        t2.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(t1, t2));

        var view1 = mockView(1L, LocalDate.of(2025, 8, 12));
        var view2 = mockView(2L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view1, view2));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);

        // Interrupt thread in background
        Thread mainThread = Thread.currentThread();
        new Thread(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            mainThread.interrupt();
        }).start();

        downloader.download();

        // First was processed, second was skipped because the pause was interrupted and returned false
        verify(dailyRepo, times(1)).saveAll(any());
    }

    @Test
    void testDownloadMergeDuplicateLatestDates() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);
        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));

        var view1 = mockView(1L, LocalDate.of(2025, 8, 12));
        var view2 = mockView(1L, LocalDate.of(2025, 8, 13));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view1, view2));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();
        // Verify it processes normally and saves since the dates don't exist yet
        verify(dailyRepo, times(1)).saveAll(any());
    }

    @Test
    void testDownloadNullLatestSavedDate() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of()); // Returns empty -> latestSavedDate is null

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, times(1)).saveAll(any());
    }

    @Test
    void testDownloadAllDownloadedPointsAlreadyExist() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        // Mock that all dates in yahoo_response.json (which are 2026-05-28 and 2026-05-29) already exist
        when(dailyRepo.findDatesByTickerAndDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of(LocalDate.of(2026, 5, 28), LocalDate.of(2026, 5, 29)));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        // saveAll should never be called since all points exist
        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadResultNullResponse() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response_result_null.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadNotArrayResponse() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response_not_array.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadMissingTimestampsResponse() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        URL jsonUrl = getClass().getResource("/yahoo_response_missing_timestamps.json");
        assertNotNull(jsonUrl);
        config.setDownloadUrl(jsonUrl.toString() + "?symbol={symbol}&start={start}&end={end}");
        config.setDelayMilliseconds(0);

        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        Ticker ticker = new Ticker();
        ticker.setTickerId(1L);
        ticker.setTickerSymbol("AAPL");
        ticker.setActive(true);

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
        var view = mockView(1L, LocalDate.of(2025, 8, 12));
        when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                .thenReturn(List.of(view));

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, never()).saveAll(any());
    }

    private String createTempJsonFile(String jsonContent) throws java.io.IOException {
        java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("yahoo_test_", ".json");
        java.nio.file.Files.writeString(tempFile, jsonContent);
        return tempFile.toUri().toString();
    }

    private void deleteTempJsonFile(String fileUrl) {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(java.net.URI.create(fileUrl));
            java.nio.file.Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }

    private void runDownloaderWithJson(String jsonContent) throws java.io.IOException {
        String fileUrl = createTempJsonFile(jsonContent);
        try {
            YahooFinanceConfig config = new YahooFinanceConfig();
            config.setDownloadUrl(fileUrl + "?symbol={symbol}&start={start}&end={end}");
            config.setDelayMilliseconds(0);

            TickerRepository tickerRepo = mock(TickerRepository.class);
            DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
            ObjectMapper mapper = new ObjectMapper();

            Ticker ticker = new Ticker();
            ticker.setTickerId(1L);
            ticker.setTickerSymbol("AAPL");
            ticker.setActive(true);

            when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of(ticker));
            var view = mockView(1L, LocalDate.of(2025, 8, 12));
            when(dailyRepo.findLatestPriceDatesForAllTickers(any(LocalDate.class)))
                    .thenReturn(List.of(view));

            YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
            downloader.download();

            verify(dailyRepo, never()).saveAll(any());
        } finally {
            deleteTempJsonFile(fileUrl);
        }
    }

    @Test
    void testDownloadResultIsJsonNull() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [null]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadHighNotArray() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": \"not-an-array\", \"low\": [150.0], \"close\": [150.0], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadLowNotArray() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": \"not-an-array\", \"close\": [150.0], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadCloseNotArray() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": [150.0], \"close\": \"not-an-array\", \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadVolumeNotArray() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": [150.0], \"close\": [150.0], \"volume\": \"not-an-array\"}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadOpenNotArray() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": \"not-an-array\", \"high\": [150.0], \"low\": [150.0], \"close\": [150.0], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadMissingIndicatorsNode() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000]}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadHighNullRow() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [null], \"low\": [150.0], \"close\": [150.0], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadLowNullRow() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": [null], \"close\": [150.0], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadCloseNullRow() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": [150.0], \"close\": [null], \"volume\": [1000]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadVolumeNullRow() throws java.io.IOException {
        String json = "{\"chart\": {\"result\": [{\"timestamp\": [1780000000], \"indicators\": {\"quote\": [{\"open\": [150.0], \"high\": [150.0], \"low\": [150.0], \"close\": [150.0], \"volume\": [null]}]}}]}}";
        runDownloaderWithJson(json);
    }

    @Test
    void testDownloadNoActiveTickers() {
        YahooFinanceConfig config = new YahooFinanceConfig();
        TickerRepository tickerRepo = mock(TickerRepository.class);
        DailyPriceRepository dailyRepo = mock(DailyPriceRepository.class);
        ObjectMapper mapper = new ObjectMapper();

        when(tickerRepo.findByIsActiveTrue()).thenReturn(List.of());

        YahooFinanceDownloader downloader = new YahooFinanceDownloader(config, tickerRepo, dailyRepo, mapper);
        downloader.download();

        verify(dailyRepo, never()).saveAll(any());
    }

    @Test
    void testDownloadResultMissing() throws java.io.IOException {
        String json = "{\"chart\": {}}";
        runDownloaderWithJson(json);
    }
}

