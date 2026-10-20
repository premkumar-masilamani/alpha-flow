package com.alphaflow.engine.configs;

import lombok.Data;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
public class ChartPatternConfig {

  private boolean enabled = false;
}
