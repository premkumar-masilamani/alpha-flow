package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.ChartPatternStatus;
import com.alphaflow.persistence.enums.ChartPatternType;
import com.alphaflow.persistence.enums.PatternSentiment;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "daily_chart_patterns")
@Getter
@Setter
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyChartPattern {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @Enumerated(EnumType.STRING)
  @Column(name = "pattern_type", length = 50, nullable = false)
  private ChartPatternType patternType;

  @Enumerated(EnumType.STRING)
  @Column(name = "sentiment", length = 25, nullable = false)
  private PatternSentiment sentiment;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", length = 20, nullable = false)
  private ChartPatternStatus status;

  @EqualsAndHashCode.Include
  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "breakout_date")
  private LocalDate breakoutDate;

  @Column(name = "neckline_slope", precision = 18, scale = 4)
  private BigDecimal necklineSlope;

  @Column(name = "neckline_price", precision = 18, scale = 4)
  private BigDecimal necklinePrice;

  @Column(name = "target_price", precision = 18, scale = 4)
  private BigDecimal targetPrice;

  @Column(name = "stop_loss_price", precision = 18, scale = 4)
  private BigDecimal stopLossPrice;

  @Column(name = "invalidation_price", precision = 18, scale = 4)
  private BigDecimal invalidationPrice;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "pivot_points", nullable = false)
  private List<ChartPatternPivot> pivotPoints;
}
