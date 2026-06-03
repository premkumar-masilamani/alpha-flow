package com.alphaflow.api.configs;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConfigurationProperties(prefix = "alphaflow.cors")
@Data
public class CORSConfig implements WebMvcConfigurer {

  private List<String> allowedOrigins;

  @Override
  public void addCorsMappings(@NonNull CorsRegistry registry) {

    if (allowedOrigins == null || allowedOrigins.isEmpty()) {
      throw new IllegalStateException(
          "alphaflow.cors.allowed-origins is not configured. Set the CORS_ALLOWED_ORIGINS environment variable.");
    }

    registry
        .addMapping("/api/**")
        .allowedOrigins(allowedOrigins.toArray(String[]::new))
        .allowedMethods("GET", "OPTIONS")
        .allowedHeaders("Content-Type", "Accept");
  }
}
