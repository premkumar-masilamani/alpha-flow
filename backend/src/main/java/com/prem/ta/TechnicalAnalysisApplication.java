package com.prem.ta.backend;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(BinanceSyncProperties.class)
public class TechnicalAnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(TechnicalAnalysisApplication.class, args);
    }

    @Bean
    CommandLineRunner runner(BinanceSyncProperties props) {
        return args -> {
            BinanceSyncService service = new BinanceSyncService(props);
            service.run();
        };
    }
}

@ConfigurationProperties(prefix = "binance.sync")
class BinanceSyncProperties {

    /** Comma separated list of tickers, e.g. BTCUSDT,ETHUSDT */
    private List<String> tickers;
    /** Start date in ISO format yyyy-MM-dd */
    private String startDate;
    /** Local directory where files will be saved */
    private String outputDir = "./downloads";

    public List<String> getTickers() {
        return tickers;
    }

    public void setTickers(List<String> tickers) {
        this.tickers = tickers;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }
}

class BinanceSyncService {

    private final BinanceSyncProperties props;
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(java.time.Duration.ofSeconds(30))
        .build();
    private static final DateTimeFormatter Y = DateTimeFormatter.ofPattern(
        "yyyy",
        Locale.ROOT
    );
    private static final DateTimeFormatter M = DateTimeFormatter.ofPattern(
        "MM",
        Locale.ROOT
    );
    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern(
        "dd",
        Locale.ROOT
    );

    BinanceSyncService(BinanceSyncProperties props) {
        this.props = props;
    }

    public void run() throws Exception {
        if (props.getTickers() == null || props.getTickers().isEmpty()) {
            throw new IllegalStateException(
                "No tickers configured. Set binance.sync.tickers in application.properties"
            );
        }
        if (props.getStartDate() == null) {
            throw new IllegalStateException(
                "No start date configured. Set binance.sync.start-date in application.properties"
            );
        }

        LocalDate start = LocalDate.parse(props.getStartDate());
        LocalDate today = LocalDate.now();

        for (String ticker : props.getTickers()) {
            System.out.printf(
                "Starting downloads for %s from %s to %s%n",
                ticker,
                start,
                today
            );
            Path tickerDir = Path.of(
                props.getOutputDir(),
                ticker.toUpperCase(Locale.ROOT)
            );
            Files.createDirectories(tickerDir);

            for (
                LocalDate date = start;
                !date.isAfter(today);
                date = date.plusDays(1)
            ) {
                String yyyy = date.format(Y);
                String mm = date.format(M);
                String dd = date.format(D);

                String baseName = String.format(
                    "%s-trades-%s-%s-%s.zip",
                    ticker,
                    yyyy,
                    mm,
                    dd
                );
                String zipUrl = String.format(
                    "https://data.binance.vision/data/spot/daily/trades/%s/%s",
                    ticker,
                    baseName
                );
                String checksumUrl = zipUrl + ".CHECKSUM";

                Path targetZip = tickerDir.resolve(baseName);
                Path targetChecksum = tickerDir.resolve(baseName + ".CHECKSUM");

                // Idempotency: if zip exists and checksum matches, skip.
                if (Files.exists(targetZip) && Files.exists(targetChecksum)) {
                    Optional<String> expected = readFirstToken(targetChecksum);
                    if (expected.isPresent()) {
                        String expectedHash = expected.get();
                        String actualHash = computeSHA256Hex(targetZip);
                        if (expectedHash.equalsIgnoreCase(actualHash)) {
                            System.out.printf(
                                "Skipping %s: already downloaded and checksum OK.%n",
                                baseName
                            );
                            continue;
                        } else {
                            System.out.printf(
                                "Checksum mismatch for existing %s. Will re-download.%n",
                                baseName
                            );
                            Files.delete(targetZip);
                        }
                    }
                }

                // Download checksum first. If not present (404), skip this date.
                HttpResponse<InputStream> checksumResp = sendRequestForStream(
                    checksumUrl
                );
                if (checksumResp.statusCode() == 404) {
                    System.out.printf(
                        "No checksum found for %s (404) — skipping date %s.%n",
                        baseName,
                        date
                    );
                    // continue to next date. This keeps loop simple and sequential.
                    continue;
                }
                if (checksumResp.statusCode() >= 400) {
                    System.out.printf(
                        "Failed to fetch checksum for %s: %d. Skipping.%n",
                        baseName,
                        checksumResp.statusCode()
                    );
                    continue;
                }

                // Save checksum file
                try (InputStream is = checksumResp.body()) {
                    Files.copy(
                        is,
                        targetChecksum,
                        StandardCopyOption.REPLACE_EXISTING
                    );
                }

                // Parse expected hash
                Optional<String> expectedHashOpt = readFirstToken(
                    targetChecksum
                );
                if (expectedHashOpt.isEmpty()) {
                    System.out.printf(
                        "Checksum file for %s didn't contain a usable hash. Skipping.%n",
                        baseName
                    );
                    Files.deleteIfExists(targetChecksum);
                    continue;
                }
                String expectedHash = expectedHashOpt.get();

                // Download zip sequentially
                HttpResponse<InputStream> zipResp = sendRequestForStream(
                    zipUrl
                );
                if (zipResp.statusCode() == 404) {
                    System.out.printf(
                        "Zip not found for %s (404). Cleaning up checksum and skipping.%n",
                        baseName
                    );
                    Files.deleteIfExists(targetChecksum);
                    continue;
                }
                if (zipResp.statusCode() >= 400) {
                    System.out.printf(
                        "Failed to fetch zip for %s: %d. Skipping.%n",
                        baseName,
                        zipResp.statusCode()
                    );
                    Files.deleteIfExists(targetChecksum);
                    continue;
                }

                // Stream to a temp file while computing hash
                Path tmp = tickerDir.resolve(baseName + ".part");
                try (InputStream is = zipResp.body()) {
                    Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                }

                // Compute hash
                String actualHash = computeSHA256Hex(tmp);
                if (!actualHash.equalsIgnoreCase(expectedHash)) {
                    System.out.printf(
                        "Hash mismatch for %s. expected=%s actual=%s. Deleting and skipping.%n",
                        baseName,
                        expectedHash,
                        actualHash
                    );
                    Files.deleteIfExists(tmp);
                    Files.deleteIfExists(targetChecksum);
                    continue;
                }

                // Move temp -> final atomically
                Files.move(
                    tmp,
                    targetZip,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
                );
                System.out.printf("Downloaded and verified %s%n", baseName);

                // Respectful pause to avoid hammering
                try {
                    TimeUnit.MILLISECONDS.sleep(300);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private HttpResponse<InputStream> sendRequestForStream(String url)
        throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build();
        return http.send(req, HttpResponse.BodyHandlers.ofInputStream());
    }

    private Optional<String> readFirstToken(Path checksumFile) {
        try {
            List<String> lines = Files.readAllLines(checksumFile);
            if (lines.isEmpty()) return Optional.empty();
            String first = lines.get(0).trim();
            if (first.isEmpty()) return Optional.empty();
            // Binance .CHECKSUM files often look like: <hexhash>  filename
            String[] parts = first.split("\\s+");
            return Optional.of(parts[0]);
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private String computeSHA256Hex(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (
            InputStream is = Files.newInputStream(file);
            DigestInputStream dis = new DigestInputStream(is, md)
        ) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                /* digest updates */
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
