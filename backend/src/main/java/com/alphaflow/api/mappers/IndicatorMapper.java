package com.alphaflow.api.mappers;

import com.alphaflow.api.dtos.IndicatorConfigDTO;
import com.alphaflow.api.dtos.IndicatorPointDTO;
import com.alphaflow.api.dtos.IndicatorSeriesDTO;
import com.alphaflow.engine.calculators.indicators.IndicatorParams;
import com.alphaflow.engine.configs.IndicatorProperties.IndicatorDefinition;
import com.alphaflow.persistence.entities.IndicatorValue;
import com.alphaflow.persistence.enums.IndicatorType;
import com.alphaflow.persistence.enums.PriceSource;
import com.alphaflow.persistence.enums.Timeframe;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps indicator config and persisted plot values into the API DTOs, including a human-readable
 * label per combo (e.g. {@code "MACD(12,26,9)"}, {@code "SMA(20) VOL"}).
 */
public class IndicatorMapper {

    public static IndicatorConfigDTO toConfigDTO(Timeframe timeframe, IndicatorDefinition definition) {
        IndicatorParams params = IndicatorParams.of(definition.getParams());
        return IndicatorConfigDTO.builder()
                .timeframe(timeframe.name())
                .type(definition.getType().name())
                .source(definition.getSource().name())
                .params(params.canonical())
                .label(label(definition.getType(), definition.getSource(), params))
                .upperBound(definition.getUpperBound())
                .lowerBound(definition.getLowerBound())
                .build();
    }

    /**
     * Groups chronological plot rows into one series per (type, source, params) combo, with each
     * bar's plots collected into a single point. Encounter order is preserved (rows arrive ascending).
     */
    public static List<IndicatorSeriesDTO> toSeries(List<IndicatorValue> rows) {
        Map<String, List<IndicatorValue>> byCombo = new LinkedHashMap<>();
        for (IndicatorValue row : rows) {
            byCombo.computeIfAbsent(comboKey(row), k -> new ArrayList<>()).add(row);
        }

        List<IndicatorSeriesDTO> series = new ArrayList<>();
        for (List<IndicatorValue> combo : byCombo.values()) {
            IndicatorValue first = combo.getFirst();
            IndicatorParams params = IndicatorParams.parse(first.getParams());

            Map<LocalDate, Map<String, BigDecimal>> byDate = new LinkedHashMap<>();
            for (IndicatorValue row : combo) {
                byDate.computeIfAbsent(row.getPriceDate(), d -> new LinkedHashMap<>())
                        .put(row.getOutputName(), row.getValue());
            }

            List<IndicatorPointDTO> points = new ArrayList<>();
            for (Map.Entry<LocalDate, Map<String, BigDecimal>> entry : byDate.entrySet()) {
                points.add(IndicatorPointDTO.builder().date(entry.getKey()).values(entry.getValue()).build());
            }

            series.add(IndicatorSeriesDTO.builder()
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
        String base = switch (type) {
            case EMA -> "EMA(" + params.getInt("period") + ")";
            case SMA -> "SMA(" + params.getInt("period") + ")";
            case RSI -> "RSI(" + params.getInt("period") + ")";
            case MACD -> "MACD(" + params.getInt("fast") + "," + params.getInt("slow") + "," + params.getInt("signal", 9) + ")";
            case STOCHASTIC -> "Stoch(" + params.getInt("k") + "," + params.getInt("kSmooth") + "," + params.getInt("dSmooth") + ")";
        };
        return source == PriceSource.CLOSE ? base : base + " " + source.name();
    }

    private static String comboKey(IndicatorValue row) {
        return row.getIndicatorType() + "|" + row.getSource() + "|" + row.getParams();
    }
}
