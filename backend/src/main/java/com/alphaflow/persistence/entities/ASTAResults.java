package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.TradeAction;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.*;

@Entity
@Table(name = "asta_results")
@Getter
@Setter
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ASTAResults {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "analysis_id")
  private Long analysisId;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @Column(name = "price_date", nullable = false)
  private LocalDate priceDate;

  @Column(name = "ema_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction emaSignal;

  @Column(name = "ema_value", nullable = false, length = 255)
  private String emaValue;

  @Column(name = "macd_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction macdSignal;

  @Column(name = "macd_value", nullable = false, length = 255)
  private String macdValue;

  @Column(name = "stochastic_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction stochasticSignal;

  @Column(name = "stochastic_value", nullable = false, length = 255)
  private String stochasticValue;

  @Column(name = "rsi_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction rsiSignal;

  @Column(name = "rsi_value", nullable = false, length = 255)
  private String rsiValue;

  @Column(name = "volume_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction volumeSignal;

  @Column(name = "volume_value", nullable = false, length = 255)
  private String volumeValue;

  @Column(name = "overall_signal", nullable = false, length = 32)
  @Enumerated(EnumType.STRING)
  private TradeAction overallSignal;
}
