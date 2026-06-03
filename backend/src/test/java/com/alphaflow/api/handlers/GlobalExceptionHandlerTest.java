package com.alphaflow.api.handlers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.alphaflow.persistence.exceptions.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  @Test
  void testHandleNotFound() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/tickers/INVALID");

    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    ResponseEntity<Map<String, Object>> response =
        handler.handleNotFound(new ResourceNotFoundException("Not found message"), request);

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals(404, body.get("status"));
    assertEquals("Not Found", body.get("error"));
    assertEquals("Not found message", body.get("message"));
    assertEquals("/api/tickers/INVALID", body.get("path"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleBadRequest() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/tickers/AAPL/data");

    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    ResponseEntity<Map<String, Object>> response =
        handler.handleBadRequest(new IllegalArgumentException("Bad request message"), request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals(400, body.get("status"));
    assertEquals("Bad Request", body.get("error"));
    assertEquals("Bad request message", body.get("message"));
    assertEquals("/api/tickers/AAPL/data", body.get("path"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  void testHandleBadRequestNullMessage() {
    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getRequestURI()).thenReturn("/api/tickers/AAPL/data");

    GlobalExceptionHandler handler = new GlobalExceptionHandler();
    ResponseEntity<Map<String, Object>> response =
        handler.handleBadRequest(new IllegalArgumentException((String) null), request);

    assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("Bad Request", body.get("message"));
  }
}
