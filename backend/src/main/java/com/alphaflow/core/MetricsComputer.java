package com.alphaflow.core;

import com.alphaflow.domain.model.CapitalProfileMetrics;
import com.alphaflow.domain.model.OHLCVMetrics;
import com.alphaflow.domain.model.OrderFlowMetrics;
import org.springframework.stereotype.Component;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

import static com.alphaflow.infrastructure.config.Constants.*;
import static java.math.BigDecimal.valueOf;
import static java.util.Comparator.comparing;

@Component
public class MetricsComputer {

    public OHLCVMetrics computeOHLCV(Table table) {

        StringColumn priceColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn quantityColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQuantityColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);

        int rows = table.rowCount();
        if (rows == 0) {
            return new OHLCVMetrics(
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
        }

        BigDecimal open = new BigDecimal(priceColumn.get(0));
        BigDecimal close = new BigDecimal(priceColumn.get(rows - 1));

        BigDecimal high = open;
        BigDecimal low = open;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal quoteVolume = BigDecimal.ZERO;

        for (int i = 0; i < rows; i++) {
            BigDecimal price = new BigDecimal(priceColumn.get(i));
            BigDecimal qty = new BigDecimal(quantityColumn.get(i));
            BigDecimal quote = new BigDecimal(quoteQuantityColumn.get(i));

            if (price.compareTo(high) > 0) high = price;
            if (price.compareTo(low) < 0) low = price;

            volume = volume.add(qty, DB_MATH_CONTEXT);
            quoteVolume = quoteVolume.add(quote, DB_MATH_CONTEXT);
        }

        BigDecimal vwap = volume.signum() > 0
                ? quoteVolume.divide(volume, DB_MATH_CONTEXT)
                : BigDecimal.ZERO;

        BigDecimal vwapOHLC4 = open.add(high, DB_MATH_CONTEXT)
                .add(low, DB_MATH_CONTEXT)
                .add(close, DB_MATH_CONTEXT)
                .divide(BigDecimal.valueOf(4), DB_MATH_CONTEXT);

        BigDecimal vwapHLC3 = high.add(low, DB_MATH_CONTEXT)
                .add(close, DB_MATH_CONTEXT)
                .divide(BigDecimal.valueOf(3), DB_MATH_CONTEXT);

        return new OHLCVMetrics(open, high, low, close, volume, vwap, vwapOHLC4, vwapHLC3);
    }

    public OrderFlowMetrics computeOrderFlow(Table table) {

        StringColumn quoteQuantityColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);
        BooleanColumn isBuyerMakerColumn = table.booleanColumn(BINANCE_TICK_DATA_COLUMN_INDEX_IS_BUYER_THE_MAKER);

        BigDecimal totalCapital = BigDecimal.ZERO;
        BigDecimal buyerCapital = BigDecimal.ZERO;

        int rows = table.rowCount();
        for (int i = 0; i < rows; i++) {
            BigDecimal quote = new BigDecimal(quoteQuantityColumn.get(i));
            totalCapital = totalCapital.add(quote, DB_MATH_CONTEXT);

            // In Binance tick data, 'isBuyerMaker' = true means the buyer was the passive side (limit order).
            // Therefore, 'isBuyerMaker' = false means the buyer was the aggressive side (market order).
            if (!isBuyerMakerColumn.get(i)) {
                buyerCapital = buyerCapital.add(quote, DB_MATH_CONTEXT);
            }
        }

        return new OrderFlowMetrics(buyerCapital, totalCapital);
    }

    public CapitalProfileMetrics computeCapitalProfile(Table table, OHLCVMetrics ohlcv) {

        BigDecimal binSize = deriveBinSize(ohlcv);

        // Flat day → everything collapses to VWAP
        if (binSize.signum() == 0) {
            return new CapitalProfileMetrics(
                    ohlcv.vwap(),
                    ohlcv.high(),
                    ohlcv.low()
            );
        }

        StringColumn priceColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn quantityColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQuantityColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);

        BigDecimal anchor = ohlcv.low();
        Map<Integer, CapitalBucket> buckets = new HashMap<>();

        int rows = table.rowCount();
        for (int i = 0; i < rows; i++) {

            BigDecimal price = new BigDecimal(priceColumn.get(i));
            BigDecimal quantity = new BigDecimal(quantityColumn.get(i));
            BigDecimal quoteQuantity = new BigDecimal(quoteQuantityColumn.get(i));

            int idx = price
                    .subtract(anchor, DB_MATH_CONTEXT)
                    .divide(binSize, 0, RoundingMode.FLOOR)
                    .intValueExact();

            CapitalBucket bucket = buckets.computeIfAbsent(idx, k -> new CapitalBucket());
            bucket.volume = bucket.volume.add(quantity, DB_MATH_CONTEXT);
            bucket.capital = bucket.capital.add(quoteQuantity, DB_MATH_CONTEXT);
        }

        return deriveCapitalMetrics(buckets);
    }

    private BigDecimal deriveBinSize(OHLCVMetrics ohlcv) {

        // Price Scale = Average of all 4 prices
        BigDecimal priceScale = ohlcv.open()
                .add(ohlcv.high(), DB_MATH_CONTEXT)
                .add(ohlcv.low(), DB_MATH_CONTEXT)
                .add(ohlcv.close(), DB_MATH_CONTEXT)
                .divide(valueOf(4), DB_MATH_CONTEXT);

        // Daily Range = high - low;
        // Daily Range as % = (high - low) / priceScale
        BigDecimal rangePercent = ohlcv.high()
                .subtract(ohlcv.low(), DB_MATH_CONTEXT)
                .divide(priceScale, DB_MATH_CONTEXT);

        // If the Daily Range % less than threshold %, default to 1 bin
        // (i.e.) bin size = 0
        if (rangePercent.compareTo(valueOf(CAPITAL_PROFILE_RANGE_BIN_PERCENT)) < 0) {
            return BigDecimal.ZERO;
        }

        // Get the threshold % of the price scale as bin size
        return priceScale.multiply(valueOf(CAPITAL_PROFILE_RANGE_BIN_PERCENT), DB_MATH_CONTEXT)
                .stripTrailingZeros();
    }

    private CapitalProfileMetrics deriveCapitalMetrics(Map<Integer, CapitalBucket> buckets) {

        // --- Capital POC (max capital) ---
        int pocIdx = buckets.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue(
                        comparing(b -> b.capital)
                ))
                .map(Map.Entry::getKey)
                .orElse(0);

        CapitalBucket pocBucket = buckets.get(pocIdx);

        BigDecimal pocPrice = pocBucket.capital
                .divide(pocBucket.volume, DB_MATH_CONTEXT);

        // --- Capital Value Area ---
        BigDecimal totalCapital = buckets.values()
                .stream()
                .map(b -> b.capital)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetCapital =
                totalCapital.multiply(
                        valueOf(CAPITAL_PROFILE_VALUE_AREA_PERCENT),
                        DB_MATH_CONTEXT
                );

        BigDecimal cumulative = pocBucket.capital;

        int lowIdx = pocIdx;
        int highIdx = pocIdx;

        while (cumulative.compareTo(targetCapital) < 0) {

            CapitalBucket lower = buckets.get(lowIdx - 1);
            CapitalBucket upper = buckets.get(highIdx + 1);

            BigDecimal lowerCap = lower != null ? lower.capital : BigDecimal.ZERO;
            BigDecimal upperCap = upper != null ? upper.capital : BigDecimal.ZERO;

            if (upperCap.compareTo(lowerCap) >= 0) {
                highIdx++;
                cumulative = cumulative.add(upperCap, DB_MATH_CONTEXT);
            } else {
                lowIdx--;
                cumulative = cumulative.add(lowerCap, DB_MATH_CONTEXT);
            }
        }

        // --- VWAP of VAH / VAL bins ---
        CapitalBucket valBucket = buckets.get(lowIdx);
        CapitalBucket vahBucket = buckets.get(highIdx);

        BigDecimal valPrice = valBucket.capital
                .divide(valBucket.volume, DB_MATH_CONTEXT);

        BigDecimal vahPrice = vahBucket.capital
                .divide(vahBucket.volume, DB_MATH_CONTEXT);

        return new CapitalProfileMetrics(
                pocPrice,
                vahPrice,
                valPrice
        );
    }


    private static final class CapitalBucket {
        BigDecimal volume = BigDecimal.ZERO;   // qty
        BigDecimal capital = BigDecimal.ZERO;   // quoteQty
    }
}
