package com.prem.ta.core;

import com.prem.ta.models.OHLCVMetrics;
import com.prem.ta.models.OrderFlowMetrics;
import com.prem.ta.models.VolumeProfileMetrics;
import org.springframework.stereotype.Component;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.selection.Selection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prem.ta.configs.Constants.*;

@Component
public class TechnicalAnalysisEngine {

    public OHLCVMetrics computeOHLCV(Table table) {
        DoubleColumn price = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_PRICE);
        DoubleColumn qty = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_QUANTITY);
        DoubleColumn quoteQty = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_QUOTE_QUANTITY);

        int rows = table.rowCount();

        double open = price.get(0);
        double close = price.get(rows - 1);

        double high = price.max();
        double low = price.min();

        double volume = qty.sum();
        double quoteVolume = quoteQty.sum();
        double vwap = volume > 0 ? quoteVolume / volume : 0.0;

        return new OHLCVMetrics(open, high, low, close, volume, vwap);
    }

    public OrderFlowMetrics computeOrderFlow(Table table) {

        DoubleColumn qty = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_QUANTITY);
        DoubleColumn quoteQty = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_QUOTE_QUANTITY);
        BooleanColumn isBuyerMaker = table.booleanColumn(BINANCE_TICK_DATA_COLUMN_IS_BUYER_THE_MAKER);

        double totalVolume = qty.sum();
        double totalQuoteQty = quoteQty.sum();

        Selection aggressiveBuyer = isBuyerMaker.isFalse();

        double buyerVolume = qty.where(aggressiveBuyer).sum();
        double buyerCapital = quoteQty.where(aggressiveBuyer).sum();

        return new OrderFlowMetrics(
                totalVolume > 0 ? buyerVolume / totalVolume : 0.0,
                totalQuoteQty > 0 ? buyerCapital / totalQuoteQty : 0.0
        );
    }

    public VolumeProfileMetrics computeVolumeProfile(Table table, OHLCVMetrics ohlcv) {

        DoubleColumn price = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_PRICE);
        DoubleColumn qty = table.doubleColumn(BINANCE_TICK_DATA_COLUMN_QUANTITY);

        BigDecimal binSize = deriveBinSize(ohlcv);

        // Map {price_zone_low → total_volume_traded_in_that_zone}
        Map<BigDecimal, BigDecimal> volumeAtPrice = new HashMap<>();
        boolean singleBin = binSize.signum() == 0;
        BigDecimal anchorPrice = BigDecimal.valueOf(ohlcv.open());

        for (int i = 0; i < table.rowCount(); i++) {
            BigDecimal p = BigDecimal.valueOf(price.get(i));
            BigDecimal q = BigDecimal.valueOf(qty.get(i));

            // If single bin, put all the volume to open price (anchor)
            // Else, floor the number with binSize to find the respective bins
            BigDecimal bucket = singleBin
                    ? anchorPrice
                    : p.divide(binSize, 0, RoundingMode.FLOOR).multiply(binSize);

            // Add the volume to the bucket
            volumeAtPrice.merge(bucket, q, BigDecimal::add);
        }

        return deriveValueArea(volumeAtPrice);
    }

    private BigDecimal deriveBinSize(OHLCVMetrics ohlcv) {
        BigDecimal open = BigDecimal.valueOf(ohlcv.open());
        BigDecimal high = BigDecimal.valueOf(ohlcv.high());
        BigDecimal low = BigDecimal.valueOf(ohlcv.low());
        BigDecimal close = BigDecimal.valueOf(ohlcv.close());

        // Price Scale = Average of all 4 prices
        BigDecimal priceScale = open.add(high).add(low).add(close).divide(BigDecimal.valueOf(4), 18, RoundingMode.HALF_UP);

        // Daily Range = high - low;
        // Daily Range as % = (high - low) / priceScale
        BigDecimal rangePercent = high.subtract(low).divide(priceScale, 18, RoundingMode.HALF_UP);

        // If the Daily Range % less than threshold %, default to 1 bin
        // (i.e.) bin size = 0
        if (rangePercent.compareTo(BigDecimal.valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT)) < 0) {
            return BigDecimal.ZERO;
        }

        // Get the threshold % of the price scale as bin size
        return priceScale.multiply(BigDecimal.valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT))
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
                .sorted(Comparator.comparing(e -> e.getKey().subtract(poc).abs()))
                .toList();

        // Target Volume calculation, to stop the iteration once reached the limit
        BigDecimal totalVolume = volumeAtPrice.values()
                .stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal targetVolume =
                totalVolume.multiply(
                        BigDecimal.valueOf(VOLUME_PROFILE_VALUE_AREA_PERCENT)
                );

        BigDecimal cumulative = BigDecimal.ZERO;
        BigDecimal vah = poc;
        BigDecimal val = poc;

        for (var e : sortedDistanceFromPoCList) {
            vah = vah.max(e.getKey());
            val = val.min(e.getKey());
            cumulative = cumulative.add(e.getValue());
            if (cumulative.compareTo(targetVolume) >= 0) break;
        }

        // These values represent the lower bounds of the zones
        return new VolumeProfileMetrics(poc, vah, val);
    }
}
