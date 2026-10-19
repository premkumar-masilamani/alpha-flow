package com.alphaflow.engine.services;

@FunctionalInterface
public interface NotificationService {

  void sendNotification(String message);

  default void sendNotification(String message, String apiToken, String chatId) {
    sendNotification(message);
  }
}
