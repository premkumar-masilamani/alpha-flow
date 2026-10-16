package com.alphaflow.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "notification_logs")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "notification_log_id")
  @EqualsAndHashCode.Include
  private Long notificationLogId;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Column(name = "error", columnDefinition = "TEXT")
  private String error;

  @Column(name = "created_at", nullable = false)
  @Builder.Default
  private OffsetDateTime createdAt = OffsetDateTime.now();
}
