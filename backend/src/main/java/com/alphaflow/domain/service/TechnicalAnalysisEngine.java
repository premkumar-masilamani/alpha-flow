package com.alphaflow.domain.service;

import com.alphaflow.domain.model.OHLCVMetrics;
import com.alphaflow.domain.model.OrderFlowMetrics;
import com.alphaflow.domain.model.VolumeProfileMetrics;
import org.springframework.stereotype.Component;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphaflow.infrastructure.config.Constants.*;
import static java.math.BigDecimal.valueOf;
import static java.util.Comparator.comparing;

@Component
public class TechnicalAnalysisEngine {

    /**
     * Computes Open, High, Low, Close, Volume, and VWAP (Volume Weighted Average Price)
     * from a table of tick data.
     *
     * @param table The table containing tick data.
     * @return Calculated OHLCV metrics.
     */
    public OHLCVMetrics computeOHLCV(Table table) {
        StringColumn priceCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn qtyCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQtyCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);

        int rows = table.rowCount();
        if (rows == 0) return new OHLCVMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        BigDecimal open = new BigDecimal(priceCol.get(0));
        BigDecimal close = new BigDecimal(priceCol.get(rows - 1));

        BigDecimal high = open;
        BigDecimal low = open;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal quoteVolume = BigDecimal.ZERO;

        for (int i = 0; i < rows; i++) {
            BigDecimal price = new BigDecimal(priceCol.get(i));
            BigDecimal qty = new BigDecimal(qtyCol.get(i));
            BigDecimal quote = new BigDecimal(quoteQtyCol.get(i));

            if (price.compareTo(high) > 0) high = price;
            if (price.compareTo(low) < 0) low = price;

            volume = volume.add(qty, DB_MATH_CONTEXT);
            quoteVolume = quoteVolume.add(quote, DB_MATH_CONTEXT);
        }

        // VWAP = Total Quote Volume / Total Base Volume
        BigDecimal vwap = volume.signum() > 0 ? quoteVolume.divide(volume, DB_MATH_CONTEXT) : BigDecimal.ZERO;

        return new OHLCVMetrics(open, high, low, close, volume, vwap);
    }

    /**
     * Computes Order Flow metrics, specifically the share of volume and capital
     * attributed to aggressive buyers (market orders).
     *
     * @param table The table containing tick data.
     * @return Calculated Order Flow metrics.
     */
    public OrderFlowMetrics computeOrderFlow(Table table) {
        StringColumn qtyCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQtyCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);
        BooleanColumn isBuyerMakerColumn = table.booleanColumn(BINANCE_TICK_DATA_COLUMN_INDEX_IS_BUYER_THE_MAKER);

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalQuoteQty = BigDecimal.ZERO;
        BigDecimal buyerVolume = BigDecimal.ZERO;
        BigDecimal buyerCapital = BigDecimal.ZERO;

        int rowCount = table.rowCount();

        for (int i = 0; i < rowCount; i++) {
            BigDecimal qty = new BigDecimal(qtyCol.get(i));
            BigDecimal quoteQty = new BigDecimal(quoteQtyCol.get(i));

            totalVolume = totalVolume.add(qty, DB_MATH_CONTEXT);
            totalQuoteQty = totalQuoteQty.add(quoteQty, DB_MATH_CONTEXT);

            // In Binance tick data, 'isBuyerMaker' = true means the buyer was the passive side (limit order).
            // Therefore, 'isBuyerMaker' = false means the buyer was the aggressive side (market order).
            if (!isBuyerMakerColumn.get(i)) {
                buyerVolume = buyerVolume.add(qty, DB_MATH_CONTEXT);
                buyerCapital = buyerCapital.add(quoteQty, DB_MATH_CONTEXT);
            }
        }

        BigDecimal buyerVolumeShare = totalVolume.signum() > 0 ? buyerVolume.divide(totalVolume, DB_MATH_CONTEXT) : BigDecimal.ZERO;
        BigDecimal buyerCapitalShare = totalQuoteQty.signum() > 0 ? buyerCapital.divide(totalQuoteQty, DB_MATH_CONTEXT) : BigDecimal.ZERO;

        return new OrderFlowMetrics(buyerVolumeShare, buyerCapitalShare);
    }

    /**
     * Computes Volume Profile metrics including Point of Control (POC) and Value Area (VAH/VAL).
     * This involves binning trades into price zones and identifying where the most volume occurred.
     *
     * @param table The table containing tick data.
     * @param ohlcv Pre-computed OHLCV metrics used for bin size derivation.
     * @return Calculated Volume Profile metrics.
     */
    public VolumeProfileMetrics computeVolumeProfile(Table table, OHLCVMetrics ohlcv) {
        BigDecimal binSize = deriveBinSize(ohlcv);
        StringColumn priceCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn qtyCol = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);

        // Map {price_zone_low → total_volume_traded_in_that_zone}
        Map<BigDecimal, BigDecimal> volumeAtPrice = new HashMap<>();
        boolean singleBin = binSize.signum() == 0;
        BigDecimal anchorPrice = ohlcv.open();

        int rowCount = table.rowCount();
        for (int i = 0; i < rowCount; i++) {
            BigDecimal p = new BigDecimal(priceCol.get(i));
            BigDecimal q = new BigDecimal(qtyCol.get(i));

            // If single bin (low volatility), put all volume into a single bucket.
            // Otherwise, group prices into bins of 'binSize'.
            BigDecimal bucket = singleBin ? anchorPrice : p.divide(binSize, 0, RoundingMode.FLOOR).multiply(binSize);

            // Accumulate volume for each price bucket
            volumeAtPrice.merge(bucket, q, BigDecimal::add);
        }

        return deriveValueArea(volumeAtPrice);
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
        if (rangePercent.compareTo(valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT)) < 0) {
            return BigDecimal.ZERO;
        }

        // Get the threshold % of the price scale as bin size
        return priceScale.multiply(valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT), DB_MATH_CONTEXT)
                .stripTrailingZeros();
    }

    private VolumeProfileMetrics deriveValueArea(Map<BigDecimal, BigDecimal> volumeAtPrice) {

        // The bucket with maximum volume is the Point of Control
        // (i.e.) The price zone where the market did the most business
        BigDecimal poc = volumeAtPrice.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        // Take all price bins and order them by how close they are to the POC
        // ABS (Price - PoC). ABS is important.
        // If Price > PoC -> Positive number
        // If Price < PoC -> Negative. ABS(Negative) -> Positive number
        // This is a sorted list of how far the prices are away from PoC
        List<Map.Entry<BigDecimal, BigDecimal>> sortedDistanceFromPoCList = volumeAtPrice.entrySet()
                .stream()
                .sorted(comparing(e -> e.getKey().subtract(poc).abs()))
                .toList();

        // Target Volume calculation, to stop the iteration once reached the limit
        BigDecimal totalVolume = volumeAtPrice.values()
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetVolume = totalVolume.multiply(valueOf(VOLUME_PROFILE_VALUE_AREA_PERCENT), DB_MATH_CONTEXT);

        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal vah = poc;
        BigDecimal val = poc;

        for (var e : sortedDistanceFromPoCList) {
            vah = vah.max(e.getKey());
            val = val.min(e.getKey());
            cumulative = cumulative.add(e.getValue(), DB_MATH_CONTEXT);
            if (cumulative.compareTo(targetVolume) >= 0) break;
        }

        // These values represent the lower bounds of the zones
        return new VolumeProfileMetrics(poc, vah, val);
    }
}
