package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single published indicator plot value for one bar.
 * <p>
 * The table is long/tall: an indicator that emits multiple plots (MACD -> macd/signal/histogram,
 * Stochastic -> k/d) produces one row per plot, distinguished by {@code outputName}. Rows are only
 * written once the indicator is fully defined, so {@code value} is never null.
 */
@Entity
@Table(name = "indicator_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IndicatorValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_value_id")
    private Long indicatorValueId;

    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "timeframe", nullable = false, length = 16)
    private Timeframe timeframe;

    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false, length = 32)
    private IndicatorType indicatorType;

    @EqualsAndHashCode.Include
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private PriceSource source;

    @EqualsAndHashCode.Include
    @Column(name = "params", nullable = false, length = 128)
    private String params;

    @EqualsAndHashCode.Include
    @Column(name = "output_name", nullable = false, length = 32)
    private String outputName;

    @EqualsAndHashCode.Include
    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "value", nullable = false, precision = 18, scale = 4)
    private BigDecimal value;
}
