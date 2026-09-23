package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AngelOneAuthManager {

  private static final Duration TOKEN_TTL = Duration.ofHours(20);

  private final AngelOneConfig config;
  private final TotpGenerator totpGenerator;
  private final ObjectMapper objectMapper;

  private String jwtToken;
  private String refreshToken;
  private String feedToken;
  private Instant tokenGeneratedAt;

  public AngelOneAuthManager(
      AngelOneConfig config, TotpGenerator totpGenerator, ObjectMapper objectMapper) {
    this.config = config;
    this.totpGenerator = totpGenerator;
    this.objectMapper = objectMapper;
  }

  public synchronized String getValidJwtToken() {
    if (jwtToken != null
        && tokenGeneratedAt != null
        && Duration.between(tokenGeneratedAt, Instant.now()).compareTo(TOKEN_TTL) < 0) {
      return jwtToken;
    }
    login();
    return jwtToken;
  }

  public synchronized void invalidateSession() {
    this.jwtToken = null;
    this.refreshToken = null;
    this.feedToken = null;
    this.tokenGeneratedAt = null;
  }

  public synchronized boolean login() {
    if (config.getApiKey().isBlank()
        || config.getClientCode().isBlank()
        || config.getPassword().isBlank()
        || config.getTotpKey().isBlank()) {
      log.warn("Angel One credentials are incomplete. Skipping authentication.");
      return false;
    }

    try {
      String totp = totpGenerator.generateCurrentTotp(config.getTotpKey());
      String endpoint = config.getBaseUrl() + "/rest/auth/angelbroking/user/v1/loginByPassword";

      Map<String, String> payload = new HashMap<>();
      payload.put("clientcode", config.getClientCode());
      payload.put("password", config.getPassword());
      payload.put("totp", totp);

      byte[] requestBytes = objectMapper.writeValueAsBytes(payload);

      HttpURLConnection conn = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
      conn.setRequestMethod("POST");
      conn.setConnectTimeout(10000);
      conn.setReadTimeout(10000);
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json");
      conn.setRequestProperty("Accept", "application/json");
      conn.setRequestProperty("X-PrivateKey", config.getApiKey());
      conn.setRequestProperty("X-UserType", "USER");
      conn.setRequestProperty("X-SourceID", "WEB");

      try (OutputStream os = conn.getOutputStream()) {
        os.write(requestBytes);
      }

      int statusCode = conn.getResponseCode();
      InputStream is =
          (statusCode >= 200 && statusCode < 300) ? conn.getInputStream() : conn.getErrorStream();

      byte[] responseBytes = (is != null) ? is.readAllBytes() : new byte[0];
      String responseBody = new String(responseBytes, StandardCharsets.UTF_8);

      if (statusCode != 200) {
        log.error("Angel One login failed with status {}: {}", statusCode, responseBody);
        return false;
      }

      JsonNode root = objectMapper.readTree(responseBody);
      boolean status = root.path("status").asBoolean(false);
      if (status) {
        JsonNode data = root.path("data");
        this.jwtToken = data.path("jwtToken").asText(null);
        this.refreshToken = data.path("refreshToken").asText(null);
        this.feedToken = data.path("feedToken").asText(null);
        this.tokenGeneratedAt = Instant.now();
        log.info("Angel One authentication successful for client code: {}", config.getClientCode());
        return true;
      } else {
        log.error(
            "Angel One login response returned failure: message={}, errorcode={}",
            root.path("message").asText(),
            root.path("errorcode").asText());
        return false;
      }
    } catch (java.io.IOException | RuntimeException e) {
      log.error("Exception occurred while authenticating with Angel One SmartAPI", e);
      return false;
    }
  }

  public String getFeedToken() {
    return feedToken;
  }
}
