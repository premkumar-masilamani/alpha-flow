package com.prem.ta.core;

import com.prem.ta.models.OHLCVMetrics;
import com.prem.ta.models.OrderFlowMetrics;
import com.prem.ta.models.VolumeProfileMetrics;
import org.springframework.stereotype.Component;
import tech.tablesaw.api.BooleanColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.prem.ta.configs.Constants.*;
import static java.math.BigDecimal.valueOf;

@Component
public class TechnicalAnalysisEngine {

    public OHLCVMetrics computeOHLCV(Table table) {
        StringColumn priceColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn qtyColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQtyColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);

        int rows = table.rowCount();

        BigDecimal open = new BigDecimal(priceColumn.get(0));
        BigDecimal close = new BigDecimal(priceColumn.get(rows - 1));

        BigDecimal high = open;
        BigDecimal low = open;
        BigDecimal volume = BigDecimal.ZERO;
        BigDecimal quoteVolume = BigDecimal.ZERO;

        for (int i = 0; i < rows; i++) {
            BigDecimal price = new BigDecimal(priceColumn.get(i));
            BigDecimal qty = new BigDecimal(qtyColumn.get(i));
            BigDecimal quote = new BigDecimal(quoteQtyColumn.get(i));

            if (price.compareTo(high) > 0) high = price;
            if (price.compareTo(low) < 0) low = price;

            volume = volume.add(qty);
            quoteVolume = quoteVolume.add(quote);
        }

        BigDecimal vwap = volume.signum() > 0
                ? quoteVolume.divide(volume, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OHLCVMetrics(open, high, low, close, volume, vwap);
    }

    public OrderFlowMetrics computeOrderFlow(Table table) {

        StringColumn qtyColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);
        StringColumn quoteQtyColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUOTE_QUANTITY);
        BooleanColumn isBuyerMakerColumn = table.booleanColumn(BINANCE_TICK_DATA_COLUMN_INDEX_IS_BUYER_THE_MAKER);

        BigDecimal totalVolume = BigDecimal.ZERO;
        BigDecimal totalQuoteQty = BigDecimal.ZERO;
        BigDecimal buyerVolume = BigDecimal.ZERO;
        BigDecimal buyerCapital = BigDecimal.ZERO;

        int rowCount = table.rowCount();

        for (int i = 0; i < rowCount; i++) {
            BigDecimal qty = new BigDecimal(qtyColumn.get(i));
            BigDecimal quoteQty = new BigDecimal(quoteQtyColumn.get(i));

            totalVolume = totalVolume.add(qty);
            totalQuoteQty = totalQuoteQty.add(quoteQty);

            // Aggressive buyer = buyer is NOT the maker
            if (!isBuyerMakerColumn.get(i)) {
                buyerVolume = buyerVolume.add(qty);
                buyerCapital = buyerCapital.add(quoteQty);
            }
        }

        BigDecimal buyerVolumeShare = totalVolume.signum() > 0
                ? buyerVolume.divide(totalVolume, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal buyerCapitalShare = totalQuoteQty.signum() > 0
                ? buyerCapital.divide(totalQuoteQty, 8, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new OrderFlowMetrics(buyerVolumeShare, buyerCapitalShare);
    }

    public VolumeProfileMetrics computeVolumeProfile(Table table, OHLCVMetrics ohlcv) {

        BigDecimal binSize = deriveBinSize(ohlcv);
        StringColumn priceColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_PRICE);
        StringColumn qtyColumn = table.stringColumn(BINANCE_TICK_DATA_COLUMN_INDEX_QUANTITY);

        // Map {price_zone_low → total_volume_traded_in_that_zone}
        Map<BigDecimal, BigDecimal> volumeAtPrice = new HashMap<>();
        boolean singleBin = binSize.signum() == 0;
        BigDecimal anchorPrice = ohlcv.open();

        for (int i = 0; i < table.rowCount(); i++) {
            BigDecimal p = new BigDecimal(priceColumn.get(i));
            BigDecimal q = new BigDecimal(qtyColumn.get(i));

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

        // Price Scale = Average of all 4 prices
        BigDecimal priceScale = ohlcv.open()
                .add(ohlcv.high())
                .add(ohlcv.low())
                .add(ohlcv.close())
                .divide(valueOf(4), 18, RoundingMode.HALF_UP);

        // Daily Range = high - low;
        // Daily Range as % = (high - low) / priceScale
        BigDecimal rangePercent = ohlcv.high()
                .subtract(ohlcv.low())
                .divide(priceScale, 18, RoundingMode.HALF_UP);

        // If the Daily Range % less than threshold %, default to 1 bin
        // (i.e.) bin size = 0
        if (rangePercent.compareTo(valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT)) < 0) {
            return BigDecimal.ZERO;
        }

        // Get the threshold % of the price scale as bin size
        return priceScale.multiply(valueOf(VOLUME_PROFILE_RANGE_BIN_PERCENT))
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
                        valueOf(VOLUME_PROFILE_VALUE_AREA_PERCENT)
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
