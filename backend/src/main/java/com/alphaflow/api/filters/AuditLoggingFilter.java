package com.alphaflow.api.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest httpServletRequest,
            @NonNull HttpServletResponse httpServletResponse,
            FilterChain filterChain
    ) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper req = new ContentCachingRequestWrapper(httpServletRequest);
        ContentCachingResponseWrapper res = new ContentCachingResponseWrapper(httpServletResponse);

        try {
            filterChain.doFilter(req, res);
        } finally {

            long duration = System.currentTimeMillis() - startTime;

            int requestSize = req.getContentAsByteArray().length;
            int responseSize = res.getContentAsByteArray().length;

            log.info(
                    "{} {} | status={} | duration={} ms | reqSize={} B | resSize={} B | ip={}",
                    httpServletRequest.getMethod(),
                    httpServletRequest.getRequestURI(),
                    httpServletResponse.getStatus(),
                    duration,
                    requestSize,
                    responseSize,
                    httpServletRequest.getRemoteAddr()
            );

            res.copyBodyToResponse();
        }
    }
}
