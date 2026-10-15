import { type DailyCandleData, type SupportResistanceData } from '../services/api';

export const SUPPORT_RESISTANCE_RANGE_PCT = 0.25;

/**
 * Filters support and resistance levels whose zoneMidpoint falls within the
 * percentage range window of the reference candle's Typical Price ((High + Low + Close) / 3).
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

    const typicalPrice = (latestCandle.high + latestCandle.low + latestCandle.close) / 3;
    const minMidpoint = typicalPrice * (1 - rangePct);
    const maxMidpoint = typicalPrice * (1 + rangePct);

    return levels.filter((level) => {
        const midpoint = Number(level.zoneMidpoint);
        return midpoint >= minMidpoint && midpoint <= maxMidpoint;
    });
};
