package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.Country;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "market_holidays")
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketHoliday {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "market_holiday_id")
  private Long marketHolidayId;

  @EqualsAndHashCode.Include
  @Enumerated(EnumType.STRING)
  @Column(name = "country", nullable = false, length = 10)
  private Country country;

  @EqualsAndHashCode.Include
  @Column(name = "holiday_date", nullable = false)
  private LocalDate holidayDate;

  @Column(name = "name", nullable = false, length = 255)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private Instant createdAt = Instant.now();
}
