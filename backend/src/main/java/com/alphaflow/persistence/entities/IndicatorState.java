package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * Resume checkpoint for one (ticker, timeframe, indicator, source, params) combo, enabling
 * incremental computation without replaying full price history.
 * <p>
 * Every combo has exactly one row — including windowed indicators (SMA, Stochastic) whose
 * {@code internals} stay null — so cold-start detection and the contiguity check are uniform.
 * {@code lastPriceDate} always lags the latest published bar by at least one finalized bar
 * (the in-progress bar is never checkpointed). {@code internals} stores recursive running state
 * (prev EMA, Wilder avg gain/loss, etc.) as a JSON object of string-encoded decimals so resume
 * is bit-exact and never round-trips through {@code double}.
 */
@Entity
@Table(name = "indicator_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IndicatorState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "indicator_state_id")
    private Long indicatorStateId;

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

    @Column(name = "last_price_date", nullable = false)
    private LocalDate lastPriceDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internals", columnDefinition = "jsonb")
    private String internals;
}
