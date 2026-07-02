package com.alphaflow.persistence.entities;

import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.persistence.enums.SRCurrentType;
import com.alphaflow.persistence.enums.SRFilterReason;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "support_resistances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "ticker")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SupportResistance {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @EqualsAndHashCode.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @Enumerated(EnumType.STRING)
  @Column(name = "timeframe", nullable = false, length = 20)
  private Timeframe timeframe;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal slope;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal intercept;

  @Column(name = "current_price", nullable = false, precision = 18, scale = 4)
  private BigDecimal currentPrice;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_type", nullable = false)
  private SRCurrentType currentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "filter_reason")
  private SRFilterReason filterReason;

  @Column(name = "break_count", nullable = false)
  @Builder.Default
  private Integer breakCount = 0;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "touch_points", columnDefinition = "jsonb")
  @Builder.Default
  private List<SRTouchPoint> touchPoints = new ArrayList<>();

  @Column(nullable = false)
  @Builder.Default
  private Integer importance = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private ZonedDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = ZonedDateTime.now();
  }
}
