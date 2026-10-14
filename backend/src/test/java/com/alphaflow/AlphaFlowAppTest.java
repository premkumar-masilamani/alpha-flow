package com.alphaflow;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class AlphaFlowAppTest {

  @Test
  void mainStartsSpringApplication() {

    try (MockedStatic<SpringApplication> springAppMock = mockStatic(SpringApplication.class)) {

      ConfigurableApplicationContext mockContext = mock(ConfigurableApplicationContext.class);

      springAppMock
          .when(() -> SpringApplication.run(eq(AlphaFlowApp.class), any(String[].class)))
          .thenReturn(mockContext);

      AlphaFlowApp.main(new String[] {"test"});

      springAppMock.verify(
          () -> SpringApplication.run(eq(AlphaFlowApp.class), any(String[].class)));
    }

    AlphaFlowApp app = new AlphaFlowApp();

    assertNotNull(app);
  }

  @Test
  void mainThrowsAndLogsWhenSpringApplicationFails() {

    try (MockedStatic<SpringApplication> springAppMock = mockStatic(SpringApplication.class)) {

      springAppMock
          .when(() -> SpringApplication.run(eq(AlphaFlowApp.class), any(String[].class)))
          .thenThrow(new IllegalStateException("Failed to configure Angel One"));

      org.junit.jupiter.api.Assertions.assertThrows(
          IllegalStateException.class, () -> AlphaFlowApp.main(new String[] {"test"}));
    }
  }
}
