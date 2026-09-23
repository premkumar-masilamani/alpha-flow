package com.alphaflow.engine.downloaders.angelone;

import static org.junit.jupiter.api.Assertions.*;

import com.alphaflow.engine.configs.AngelOneConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AngelOneAuthManagerTest {

  private HttpServer server;
  private AngelOneConfig config;
  private TotpGenerator totpGenerator;
  private ObjectMapper objectMapper;
  private AngelOneAuthManager authManager;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.start();

    config = new AngelOneConfig();
    config.setBaseUrl("http://localhost:" + server.getAddress().getPort());
    config.setApiKey("test-api-key");
    config.setClientCode("test-client-code");
    config.setPassword("test-mpin");
    config.setTotpKey("JBSWY3DPEHPK3PXP");

    totpGenerator = new TotpGenerator();
    objectMapper = new ObjectMapper();
    authManager = new AngelOneAuthManager(config, totpGenerator, objectMapper);
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void testLoginSuccessAndCache() {
    String successResponse =
        """
        {
          "status": true,
          "message": "SUCCESS",
          "errorcode": "",
          "data": {
            "jwtToken": "mock-jwt-token-12345",
            "refreshToken": "mock-refresh-token",
            "feedToken": "mock-feed-token"
          }
        }
        """;

    server.createContext(
        "/rest/auth/angelbroking/user/v1/loginByPassword",
        exchange -> {
          byte[] bytes = successResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    boolean loggedIn = authManager.login();
    assertTrue(loggedIn);
    assertEquals("mock-jwt-token-12345", authManager.getValidJwtToken());
    assertEquals("mock-feed-token", authManager.getFeedToken());

    // Second call should return cached token without server hit
    assertEquals("mock-jwt-token-12345", authManager.getValidJwtToken());

    // Invalidate
    authManager.invalidateSession();
    // After invalidation, calling getValidJwtToken logs in again
    assertEquals("mock-jwt-token-12345", authManager.getValidJwtToken());
  }

  @Test
  void testLoginIncompleteCredentials() {
    config.setApiKey("");
    assertFalse(authManager.login());
    assertNull(authManager.getValidJwtToken());
  }

  @Test
  void testLoginFailureStatusFalse() {
    String failResponse =
        """
        {
          "status": false,
          "message": "Invalid credentials",
          "errorcode": "AB1001",
          "data": null
        }
        """;

    server.createContext(
        "/rest/auth/angelbroking/user/v1/loginByPassword",
        exchange -> {
          byte[] bytes = failResponse.getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    assertFalse(authManager.login());
    assertNull(authManager.getValidJwtToken());
  }

  @Test
  void testLoginHttpErrorStatus() {
    server.createContext(
        "/rest/auth/angelbroking/user/v1/loginByPassword",
        exchange -> {
          byte[] bytes = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(500, bytes.length);
          try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
          }
        });

    assertFalse(authManager.login());
    assertNull(authManager.getValidJwtToken());
  }

  @Test
  void testLoginConnectionRefused() {
    server.stop(0);
    assertFalse(authManager.login());
    assertNull(authManager.getValidJwtToken());
  }
}
