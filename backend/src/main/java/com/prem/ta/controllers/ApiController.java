package com.prem.ta.controllers;

import jakarta.servlet.http.HttpServletRequest;
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

    @RequestMapping("/api")
    public Map<String, Object> index() {
        return Map.of(
                "name", "Technical Analysis Application API",
                "version", "v1",
                "docs", "/swagger-ui/index.html",
                "openapi", "/v3/api-docs"
        );
    }

    @RequestMapping("/error")
    public Map<String, Object> handleError(HttpServletRequest request) {

        Integer status = (Integer) request.getAttribute("jakarta.servlet.error.status_code");
        Throwable exception = (Throwable) request.getAttribute("jakarta.servlet.error.exception");
        String path = (String) request.getAttribute("jakarta.servlet.error.request_uri");

        HttpStatus httpStatus = valueOf(ofNullable(status)
                .orElse(INTERNAL_SERVER_ERROR.value()));

        String message = ofNullable(exception).map(Throwable::getMessage)
                .orElse(httpStatus.getReasonPhrase());

        return Map.of(
                "status", httpStatus.value(),
                "error", httpStatus.getReasonPhrase(),
                "message", message, "path", path,
                "timestamp", Instant.now().toString()
        );
    }
}
