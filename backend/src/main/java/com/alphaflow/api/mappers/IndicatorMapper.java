package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.common.enums.Timeframe;
import com.alphaflow.engine.indicators.dtos.IndicatorParams;
import com.alphaflow.persistence.entities.Indicator;
import com.alphaflow.persistence.entities.IndicatorDefinition;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps indicator config and persisted plot values into the API DTOs, including a human-readable
 *
 * <p>label per combo (e.g. {@code "MACD(12,26,9)"}, {@code "SMA(20) VOL"}).
 */
public class IndicatorMapper {

  private IndicatorMapper() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static IndicatorConfigDTO toConfigDTO(
      Timeframe timeframe, IndicatorDefinition definition) {
    IndicatorParams params = IndicatorParams.of(definition.getParams());
    return IndicatorConfigDTO.builder()
        .timeframe(timeframe.name())
        .type(definition.getType().name())
        .source(definition.getSource().name())
        .params(params.canonical())
        .label(label(definition.getType(), definition.getSource(), params))
        .build();
  }

  /**
   * Groups chronological plot rows into one series per (type, source, params) combo, with each
   *
   * <p>bar's plots collected into a single point. Encounter order is preserved (rows arrive
   * ascending).
   */
  public static List<IndicatorSeriesDTO> toSeries(List<? extends Indicator> rows) {

    Map<String, List<Indicator>> byCombo = new LinkedHashMap<>();
    for (Indicator row : rows) {
      byCombo.computeIfAbsent(comboKey(row), k -> new ArrayList<>()).add(row);
    }

    List<IndicatorSeriesDTO> series = new ArrayList<>();
    for (List<Indicator> combo : byCombo.values()) {
      Indicator first = combo.getFirst();
      IndicatorParams params = IndicatorParams.parse(first.getParams());
      List<IndicatorPointDTO> points = new ArrayList<>();
      for (Indicator row : combo) {
        points.add(
            IndicatorPointDTO.builder().date(row.getPriceDate()).values(row.getValues()).build());
      }
      series.add(
          IndicatorSeriesDTO.builder()
              .type(first.getIndicatorType().name())
              .source(first.getSource().name())
              .params(first.getParams())
              .label(label(first.getIndicatorType(), first.getSource(), params))
              .points(points)
              .build());
    }
    return series;
  }

  static String label(IndicatorType type, PriceSource source, IndicatorParams params) {
    String base =
        switch (type) {
          case EMA -> "EMA (" + params.getInt("period") + ")";
          case SMA -> {
            if (source == PriceSource.VOLUME) {
              yield "Vol (" + params.getInt("period") + ")";
            } else {
              yield "SMA (" + params.getInt("period") + ")";
            }
          }
          case RSI -> "RSI (" + params.getInt("period") + ")";
          case MACD ->
              "MACD ("
                  + params.getInt("fast")
                  + ","
                  + params.getInt("slow")
                  + ","
                  + params.getInt("signal", 9)
                  + ")";
          case STOCHASTIC ->
              "Stoch ("
                  + params.getInt("k")
                  + ","
                  + params.getInt("kSmooth")
                  + ","
                  + params.getInt("dSmooth")
                  + ")";
        };

    return source == PriceSource.CLOSE
            || (type == IndicatorType.SMA && source == PriceSource.VOLUME)
        ? base
        : base + " " + source.name();
  }

  private static String comboKey(Indicator row) {
    return (row.getIndicatorType() + "|" + row.getSource() + "|" + row.getParams());
  }
}
