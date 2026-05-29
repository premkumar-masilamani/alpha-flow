package com.alphaflow.api.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.valueOf;

@RestController
public class ApiController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @RequestMapping("/api")
    public Map<String, Object> index() {
        return Map.of(
                "name", "Alpha Flow Application",
                "version", "0.1.0",
                "docs", "/swagger-ui/index.html",
                "openapi", "/v3/api-docs"
        );
    }

    @RequestMapping("/error")
    public Map<String, Object> handleError(HttpServletRequest request) {

        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");

        log.error("Handling error status {} for path {}. Exception: {}", status, path, exception != null ? exception.getMessage() : "None");

        HttpStatus httpStatus = valueOf(ofNullable(status)
                .orElse(INTERNAL_SERVER_ERROR.value()));

        // Do not leak internal exception details to clients; return the generic status reason only.
        // Full detail is captured in the server-side log above.
        return Map.of(
                "status", httpStatus.value(),
                "error", httpStatus.getReasonPhrase(),
                "message", httpStatus.getReasonPhrase(),
                "path", path != null ? path : "",
                "timestamp", Instant.now().toString()
        );
    }
}
