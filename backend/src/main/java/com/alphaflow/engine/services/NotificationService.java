package com.alphaflow.engine.services;

@FunctionalInterface
public interface NotificationService {

  void sendNotification(String message);
}
