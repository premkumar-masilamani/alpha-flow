package com.alphaflow.api.configs;


import org.junit.jupiter.api.Test;

import org.springframework.web.servlet.config.annotation.CorsRegistration;

import org.springframework.web.servlet.config.annotation.CorsRegistry;


import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;


class CorsConfigTest {


  @Test

  void testAddCorsMappingsThrowsExceptionIfNull() {

    CorsConfig config = new CorsConfig();

    config.setAllowedOrigins(null);


    assertThrows(IllegalStateException.class, () -> config.addCorsMappings(mock(CorsRegistry.class)));

  }


  @Test

  void testAddCorsMappingsThrowsExceptionIfEmpty() {

    CorsConfig config = new CorsConfig();

    config.setAllowedOrigins(List.of());


    assertThrows(IllegalStateException.class, () -> config.addCorsMappings(mock(CorsRegistry.class)));

  }


  @Test

  void testAddCorsMappingsRegistersCorrectly() {

    CorsConfig config = new CorsConfig();

    List<String> origins = List.of("http://localhost:3000", "https://app.example.com");

    config.setAllowedOrigins(origins);

    assertEquals(origins, config.getAllowedOrigins());


    CorsRegistry registry = mock(CorsRegistry.class);

    CorsRegistration registration = mock(CorsRegistration.class);

    when(registry.addMapping("/api/**")).thenReturn(registration);

    when(registration.allowedOrigins(any(String[].class))).thenReturn(registration);

    when(registration.allowedMethods(any(String[].class))).thenReturn(registration);

    when(registration.allowedHeaders(any(String[].class))).thenReturn(registration);


    config.addCorsMappings(registry);


    verify(registry).addMapping("/api/**");

    verify(registration).allowedOrigins("http://localhost:3000", "https://app.example.com");

    verify(registration).allowedMethods("GET", "OPTIONS");

    verify(registration).allowedHeaders("Content-Type", "Accept");

  }

}

