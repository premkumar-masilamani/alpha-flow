package com.alphaflow.api.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiControllerTest {

    @Test
    void testIndex() {
        ApiController controller = new ApiController();
        Map<String, Object> res = controller.index();
        assertEquals("Alpha Flow Application", res.get("name"));
        assertEquals("0.1.0", res.get("version"));
    }

    @Test
    void testHandleErrorWithException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(404);
        when(request.getAttribute("jakarta.servlet.error.exception")).thenReturn(new RuntimeException("Oops"));
        when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn("/api/unknown");

        ApiController controller = new ApiController();
        Map<String, Object> res = controller.handleError(request);

        assertEquals(404, res.get("status"));
        assertEquals("Not Found", res.get("error"));
        assertEquals("Not Found", res.get("message"));
        assertEquals("/api/unknown", res.get("path"));
        assertNotNull(res.get("timestamp"));
    }

    @Test
    void testHandleErrorDefaults() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("jakarta.servlet.error.status_code")).thenReturn(null);
        when(request.getAttribute("jakarta.servlet.error.exception")).thenReturn(null);
        when(request.getAttribute("jakarta.servlet.error.request_uri")).thenReturn(null);

        ApiController controller = new ApiController();
        Map<String, Object> res = controller.handleError(request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), res.get("status"));
        assertEquals("Internal Server Error", res.get("error"));
        assertEquals("", res.get("path"));
    }
}
