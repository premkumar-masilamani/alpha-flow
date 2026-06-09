package com.alphaflow.api.controllers.generic;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.valueOf;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ApiController implements ErrorController {

  @RequestMapping("/api")
  public Map<String, Object> index() {
    return Map.of(
        "name", "Alpha Flow Application",
        "version", "0.1.0",
        "docs", "/api/swagger-ui/index.html",
        "openapi", "/api/v3/api-docs");
  }

  @RequestMapping("/error")
  public Map<String, Object> handleError(HttpServletRequest request) {
    Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
    Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
    String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");

    log.error(
        "Handling error status {} for path {}. Exception: {}",
        status,
        path,
        exception != null ? exception.getMessage() : "None");

    HttpStatus httpStatus = valueOf(ofNullable(status).orElse(INTERNAL_SERVER_ERROR.value()));
    return Map.of(
        "status", httpStatus.value(),
        "error", httpStatus.getReasonPhrase(),
        "message", httpStatus.getReasonPhrase(),
        "path", path != null ? path : "",
        "timestamp", Instant.now().toString());
  }
}
