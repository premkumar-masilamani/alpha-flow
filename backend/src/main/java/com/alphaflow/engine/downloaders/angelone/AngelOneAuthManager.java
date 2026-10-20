package com.alphaflow.engine.downloaders.angelone;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.alphaflow.engine.downloaders.RateLimiter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class AngelOneAuthManager {

  private final AngelOneConfig config;
  private final TotpGenerator totpGenerator;
  private final ObjectMapper objectMapper;
  private final AngelOneNetworkHelper networkHelper;
  private final RateLimiter rateLimiter;
  private final RestClient restClient;

  private String jwtToken;
  private String refreshToken;
  private String feedToken;
  private Instant tokenGeneratedAt;

  public AngelOneAuthManager(
      AngelOneConfig config,
      TotpGenerator totpGenerator,
      ObjectMapper objectMapper,
      AngelOneNetworkHelper networkHelper,
      RateLimiter rateLimiter,
      @org.springframework.beans.factory.annotation.Autowired(required = false)
          RestClient.Builder restClientBuilder) {
    this.config = config;
    this.totpGenerator = totpGenerator;
    this.objectMapper = objectMapper;
    this.networkHelper = networkHelper;
    this.rateLimiter = rateLimiter;
    this.restClient =
        (restClientBuilder != null ? restClientBuilder : RestClient.builder()).build();
  }

  public synchronized String getValidJwtToken() {
    if (jwtToken != null
        && tokenGeneratedAt != null
        && Duration.between(tokenGeneratedAt, Instant.now()).compareTo(config.getTokenTtl()) < 0) {
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
    try {
      rateLimiter.throttleAngelOneLogin();
      String totp = totpGenerator.generateCurrentTotp(config.getTotpKey());
      Map<String, String> payload = new HashMap<>();
      payload.put("clientcode", config.getClientCode());
      payload.put("password", config.getPassword());
      payload.put("totp", totp);

      ResponseEntity<String> response =
          restClient
              .post()
              .uri(config.getLoginUrl())
              .headers(headers -> networkHelper.applyHeaders(headers, config.getApiKey()))
              .body(payload)
              .retrieve()
              .toEntity(String.class);

      String responseBody = response.getBody();
      if (responseBody != null) {
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
      } else {
        log.error("Angel One login returned empty response body.");
        return false;
      }
    } catch (RestClientException | JsonProcessingException e) {
      log.error(
          "Exception occurred while authenticating with Angel One SmartAPI: {}", e.getMessage());
      return false;
    }
  }

  public String getFeedToken() {
    return feedToken;
  }
}
