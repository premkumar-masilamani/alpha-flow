package com.alphaflow.engine.services;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Reusable notification templating engine backed by Apache FreeMarker.
 * Reads templates from the classpath directory {@code templates/}, caches them in memory,
 * and renders messages by substituting variables.
 */
@Component
@Slf4j
public class NotificationTemplateEngine {

  private static final String TEMPLATE_BASE_DIR = "templates/";
  private final Configuration freemarkerConfiguration;

  public NotificationTemplateEngine() {
    this.freemarkerConfiguration = new Configuration(Configuration.VERSION_2_3_34);
    this.freemarkerConfiguration.setClassLoaderForTemplateLoading(
        getClass().getClassLoader(), TEMPLATE_BASE_DIR);
    this.freemarkerConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());
    this.freemarkerConfiguration.setTemplateExceptionHandler(
        TemplateExceptionHandler.RETHROW_HANDLER);
    this.freemarkerConfiguration.setLogTemplateExceptions(false);
    this.freemarkerConfiguration.setWrapUncheckedExceptions(true);
    this.freemarkerConfiguration.setFallbackOnNullLoopVariable(false);
  }

  public String render(String templatePath, Map<String, String> variables) {
    try {
      Template template = freemarkerConfiguration.getTemplate(templatePath);
      StringWriter writer = new StringWriter();
      Map<String, Object> model = new HashMap<>();
      if (variables != null) {
        for (Map.Entry<String, String> entry : variables.entrySet()) {
          model.put(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
      }
      template.process(model, writer);
      return writer.toString();
    } catch (IOException | TemplateException exception) {
      log.error(
          "Failed to render notification template '{}': {}",
          templatePath,
          exception.getMessage(),
          exception);
      throw new IllegalArgumentException(
          "Error rendering template: " + templatePath, exception);
    }
  }

  public void clearCache() {
    freemarkerConfiguration.clearTemplateCache();
  }
}
