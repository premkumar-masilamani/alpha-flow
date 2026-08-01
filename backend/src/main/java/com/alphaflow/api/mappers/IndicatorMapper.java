package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.IndicatorConfigDto;
import com.alphaflow.api.dtos.IndicatorPointDto;
import com.alphaflow.api.dtos.IndicatorSeriesDto;
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

public class IndicatorMapper {

  private IndicatorMapper() {
    throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
  }

  public static IndicatorConfigDto toConfigDto(
      Timeframe timeframe, IndicatorDefinition definition) {
    IndicatorParams params = IndicatorParams.of(definition.getParams());
    return IndicatorConfigDto.builder()
        .timeframe(timeframe.name())
        .type(definition.getType().name())
        .source(definition.getSource().name())
        .params(params.canonical())
        .label(label(definition.getType(), definition.getSource(), params))
        .build();
  }

  public static List<IndicatorSeriesDto> toSeries(List<? extends Indicator> rows) {

    Map<String, List<Indicator>> byCombo = new LinkedHashMap<>();
    for (Indicator row : rows) {
      byCombo.computeIfAbsent(comboKey(row), k -> new ArrayList<>()).add(row);
    }

    List<IndicatorSeriesDto> series = new ArrayList<>();
    for (List<Indicator> combo : byCombo.values()) {
      Indicator first = combo.getFirst();
      IndicatorParams params = IndicatorParams.parse(first.getParams());
      List<IndicatorPointDto> points = new ArrayList<>();
      for (Indicator row : combo) {
        points.add(
            IndicatorPointDto.builder().date(row.getPriceDate()).values(row.getValues()).build());
      }
      series.add(
          IndicatorSeriesDto.builder()
              .type(first.getIndicatorType().name())
              .source(first.getSource().name())
              .params(first.getParams())
              .label(label(first.getIndicatorType(), first.getSource(), params))
              .points(points)
              .build());
    }
    return series;
  }

  @SuppressWarnings("PMD.ExhaustiveSwitchHasDefault")
  static String label(IndicatorType type, PriceSource source, IndicatorParams params) {
    String base;
    switch (type) {
      case EMA:
        base = "EMA (" + params.getInt("period") + ")";
        break;
      case SMA:
        if (source == PriceSource.VOLUME) {
          base = "Vol (" + params.getInt("period") + ")";
        } else {
          base = "SMA (" + params.getInt("period") + ")";
        }
        break;
      case RSI:
        base = "RSI (" + params.getInt("period") + ")";
        break;
      case BB:
        base = "BB (" + params.getInt("period") + ")";
        break;
      case MACD:
        base =
            "MACD ("
                + params.getInt("fast")
                + ","
                + params.getInt("slow")
                + ","
                + params.getInt("signal", 9)
                + ")";
        break;
      case STOCHASTIC:
        base =
            "Stoch ("
                + params.getInt("k")
                + ","
                + params.getInt("kSmooth")
                + ","
                + params.getInt("dSmooth")
                + ")";
        break;
      default:
        base = type.name();
        break;
    }

    return source == PriceSource.CLOSE
            || (type == IndicatorType.SMA && source == PriceSource.VOLUME)
        ? base
        : base + " " + source.name();
  }

  private static String comboKey(Indicator row) {
    return (row.getIndicatorType() + "|" + row.getSource() + "|" + row.getParams());
  }
}
