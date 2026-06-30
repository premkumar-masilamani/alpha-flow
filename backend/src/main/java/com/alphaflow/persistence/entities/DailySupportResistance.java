package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.SRCurrentType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "daily_sr")
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
  @EqualsAndHashCode.Include
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal slope;

  @Column(nullable = false, precision = 18, scale = 4)
  private BigDecimal intercept;

  @Enumerated(EnumType.STRING)
  @Column(name = "current_type", nullable = false)
  private SRCurrentType currentType;

  @Enumerated(EnumType.STRING)
  @Column(name = "filter_reason")
  private com.alphaflow.persistence.enums.SRFilterReason filterReason;

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

  @Column(name = "updated_at", nullable = false)
  private ZonedDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = ZonedDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = ZonedDateTime.now();
  }
}
