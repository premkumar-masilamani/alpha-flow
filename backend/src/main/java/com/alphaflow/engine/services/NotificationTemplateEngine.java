package com.alphaflow.engine.services;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Lightweight notification templating engine that reads template files from classpath,
 * caches them in memory, and replaces placeholders with dynamic values.
 */
@Component
@Slf4j
public class NotificationTemplateEngine {

  private static final String TEMPLATE_BASE_DIR = "templates/";
  private final ConcurrentMap<String, String> templateCache = new ConcurrentHashMap<>();

  public String render(String templatePath, Map<String, String> variables) {
    String templateContent = getTemplate(templatePath);
    if (variables != null && !variables.isEmpty()) {
      String renderedText = templateContent;
      for (Map.Entry<String, String> entry : variables.entrySet()) {
        String placeholder = "${" + entry.getKey() + "}";
        String replacement = entry.getValue() != null ? entry.getValue() : "";
        renderedText = renderedText.replace(placeholder, replacement);
      }
      return renderedText;
    } else {
      return templateContent;
    }
  }

  public String getTemplate(String templatePath) {
    return templateCache.computeIfAbsent(templatePath, this::loadTemplateFromClasspath);
  }

  private String loadTemplateFromClasspath(String templatePath) {
    String fullResourcePath = TEMPLATE_BASE_DIR + templatePath;
    try {
      ClassPathResource classPathResource = new ClassPathResource(fullResourcePath);
      try (InputStream inputStream = classPathResource.getInputStream()) {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }
    } catch (IOException exception) {
      log.error("Failed to load notification template from classpath: {}", fullResourcePath, exception);
      throw new IllegalArgumentException("Notification template not found: " + fullResourcePath, exception);
    }
  }

  public void clearCache() {
    templateCache.clear();
  }
}
