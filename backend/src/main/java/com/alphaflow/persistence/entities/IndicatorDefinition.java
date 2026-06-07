package com.alphaflow.persistence.entities;

import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** JPA Entity representing a technical indicator configuration definition. */
@Entity
@Table(name = "indicator_definitions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class IndicatorDefinition {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "indicator_id")
  private Long indicatorId;

  @EqualsAndHashCode.Include
  @Enumerated(EnumType.STRING)
  @Column(name = "indicator_type", nullable = false, length = 32)
  private IndicatorType indicatorType;

  @EqualsAndHashCode.Include
  @Enumerated(EnumType.STRING)
  @Column(name = "source", nullable = false, length = 16)
  private PriceSource source;

  @EqualsAndHashCode.Include
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "params", nullable = false)
  private Map<String, Integer> params;

  /**
   * Helper alias method to return the indicator type, maintaining compatibility with config usage.
   *
   * @return the technical indicator type
   */
  public IndicatorType getType() {
    return indicatorType;
  }
}
