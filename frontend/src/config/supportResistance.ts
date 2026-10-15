import { type DailyCandleData, type SupportResistanceData } from '../services/api';

export const SUPPORT_RESISTANCE_RANGE_PCT = 0.25;

/**
 * Calculates the Typical Price for a candlestick bar:
 * Typical Price (TP) = (High + Low + Close) / 3
 */
export const calculateTypicalPrice = (candle: Pick<DailyCandleData, 'high' | 'low' | 'close'>): number => {
    return (candle.high + candle.low + candle.close) / 3;
};

/**
 * Calculates midpoint boundary window around the Typical Price using a range percentage.
 * minMidpoint = TP * (1 - rangePct)
 * maxMidpoint = TP * (1 + rangePct)
 */
export const calculateMidpointBounds = (
    candle: Pick<DailyCandleData, 'high' | 'low' | 'close'>,
    rangePct: number = SUPPORT_RESISTANCE_RANGE_PCT
): { minMidpoint: number; maxMidpoint: number } => {
    const typicalPrice = calculateTypicalPrice(candle);
    return {
        minMidpoint: typicalPrice * (1 - rangePct),
        maxMidpoint: typicalPrice * (1 + rangePct),
    };
};

/**
 * Filters support and resistance levels whose zoneMidpoint falls within the
 * percentage range window of the reference candle's Typical Price.
 * If reference candle is absent or levels are empty, returns an empty array to prevent
 * unwindowed levels from flashing or rendering.
 */
export const filterSupportResistances = (
    levels: SupportResistanceData[],
    latestCandle?: DailyCandleData | null,
    rangePct: number = SUPPORT_RESISTANCE_RANGE_PCT
): SupportResistanceData[] => {
    if (!latestCandle || !levels || levels.length === 0) {
        return [];
    }

    const { minMidpoint, maxMidpoint } = calculateMidpointBounds(latestCandle, rangePct);

    return levels.filter((level) => {
        const midpoint = Number(level.zoneMidpoint);
        return midpoint >= minMidpoint && midpoint <= maxMidpoint;
    });
};
