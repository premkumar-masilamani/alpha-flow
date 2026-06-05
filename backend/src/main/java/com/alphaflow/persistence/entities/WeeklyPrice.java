package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "weekly_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WeeklyPrice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "weekly_price_id")
  private Long weeklyPriceId;

  @EqualsAndHashCode.Include
  @Column(name = "price_date", nullable = false)
  private LocalDate priceDate;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @Column(name = "price_open", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceOpen;

  @Column(name = "price_high", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceHigh;

  @Column(name = "price_low", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceLow;

  @Column(name = "price_close", nullable = false, precision = 18, scale = 4)
  private BigDecimal priceClose;

  @Column(name = "volume", nullable = false, precision = 18, scale = 4)
  private BigDecimal volume;
}
