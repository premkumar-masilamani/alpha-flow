package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.CandlestickPattern;
import com.alphaflow.persistence.enums.PatternSentiment;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "daily_candlestick_patterns")
@Getter
@Setter
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyCandlestickPattern {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @Column(name = "price_date", nullable = false)
  private LocalDate priceDate;

  @EqualsAndHashCode.Include
  @Enumerated(EnumType.STRING)
  @Column(name = "pattern_name", length = 50, nullable = false)
  private CandlestickPattern pattern;

  @Enumerated(EnumType.STRING)
  @Column(name = "sentiment", length = 10, nullable = false)
  private PatternSentiment sentiment;
}
