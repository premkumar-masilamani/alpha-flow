package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Table(name = "intraday_prices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IntradayPrice {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "intraday_price_id")
  private Long intradayPriceId;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @Column(name = "timeframe", nullable = false, length = 20)
  private String timeframe;

  @EqualsAndHashCode.Include
  @Column(name = "price_time", nullable = false)
  private OffsetDateTime priceTime;

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
