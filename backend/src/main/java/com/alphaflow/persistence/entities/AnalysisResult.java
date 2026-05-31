package com.alphaflow.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "analysis_results")
@Getter
@Setter
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long analysisId;

    @EqualsAndHashCode.Include
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false, unique = true)
    private Ticker ticker;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "ema_signal", nullable = false, length = 32)
    private String emaSignal;

    @Column(name = "ema_value", nullable = false, length = 255)
    private String emaValue;

    @Column(name = "macd_signal", nullable = false, length = 32)
    private String macdSignal;

    @Column(name = "macd_value", nullable = false, length = 255)
    private String macdValue;

    @Column(name = "stochastic_signal", nullable = false, length = 32)
    private String stochasticSignal;

    @Column(name = "stochastic_value", nullable = false, length = 255)
    private String stochasticValue;

    @Column(name = "rsi_signal", nullable = false, length = 32)
    private String rsiSignal;

    @Column(name = "rsi_value", nullable = false, length = 255)
    private String rsiValue;

    @Column(name = "volume_signal", nullable = false, length = 32)
    private String volumeSignal;

    @Column(name = "volume_value", nullable = false, length = 255)
    private String volumeValue;

    @Column(name = "overall_signal", nullable = false, length = 32)
    private String overallSignal;
}
