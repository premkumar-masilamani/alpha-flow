package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "daily_support_resistances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DailySupportResistance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @Column(name = "price_date", nullable = false)
  private LocalDate priceDate;

  @EqualsAndHashCode.Include
  @Column(name = "zone_bottom", nullable = false, precision = 18, scale = 4)
  private BigDecimal zoneBottom;

  @EqualsAndHashCode.Include
  @Column(name = "zone_top", nullable = false, precision = 18, scale = 4)
  private BigDecimal zoneTop;

  @Column(name = "zone_midpoint", nullable = false, precision = 18, scale = 4)
  private BigDecimal zoneMidpoint;

  @Column(name = "touch_count", nullable = false)
  private Integer touchCount;

  @Column(name = "first_touch_date")
  private LocalDate firstTouchDate;

  @Column(name = "last_touch_date")
  private LocalDate lastTouchDate;

  @Column(name = "level_type", nullable = false, length = 20)
  private String levelType;
}
