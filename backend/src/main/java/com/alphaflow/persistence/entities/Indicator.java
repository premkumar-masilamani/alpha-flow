package com.alphaflow.persistence.entities;

import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A shared base class for daily and weekly technical indicator values.
 *
 * <p>All calculated outputs for a specific ticker, indicator definition, and date are stored
 * together in a single row within the {@code values} JSONB column.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"ticker", "indicatorDefinition"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
public abstract class Indicator {

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ticker_id", nullable = false)
  private Ticker ticker;

  @EqualsAndHashCode.Include
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "indicator_id", nullable = false)
  private IndicatorDefinition indicatorDefinition;

  @EqualsAndHashCode.Include
  @Column(name = "price_date", nullable = false)
  private LocalDate priceDate;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "values", nullable = false)
  private Map<String, BigDecimal> values;

  /**
   * Delegates indicator type access to the associated indicator definition.
   *
   * @return the indicator type
   */
  public IndicatorType getIndicatorType() {
    return indicatorDefinition.getIndicatorType();
  }

  /**
   * Delegates price source access to the associated indicator definition.
   *
   * @return the price source of the indicator
   */
  public PriceSource getSource() {
    return indicatorDefinition.getSource();
  }

  /**
   * Serializes definition parameter map to canonical string representation.
   *
   * @return canonical parameter string
   */
  public String getParams() {
    return IndicatorParams.of(indicatorDefinition.getParams()).canonical();
  }
}
