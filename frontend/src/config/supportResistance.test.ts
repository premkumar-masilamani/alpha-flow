import { describe, it, expect } from 'vitest';
import {
    SUPPORT_RESISTANCE_RANGE_PCT,
    calculateTypicalPrice,
    calculateMidpointBounds,
    filterSupportResistances,
} from './supportResistance';
import { type DailyCandleData, type SupportResistanceData } from '../services/api';

describe('supportResistance config and filtering', () => {
    const mockCandle: DailyCandleData = {
        date: '2026-05-29',
        open: 98,
        high: 105,
        low: 95,
        close: 100,
        vol: 1000,
    };

    it('exports SUPPORT_RESISTANCE_RANGE_PCT as 0.25', () => {
        expect(SUPPORT_RESISTANCE_RANGE_PCT).toBe(0.25);
    });

    it('calculates typical price correctly ((high + low + close) / 3)', () => {
        const tp = calculateTypicalPrice(mockCandle);
        expect(tp).toBe(100);
    });

    it('calculates midpoint bounds with default rangePct', () => {
        const bounds = calculateMidpointBounds(mockCandle);
        expect(bounds.minMidpoint).toBeCloseTo(75);
        expect(bounds.maxMidpoint).toBeCloseTo(125);
    });

    it('calculates midpoint bounds with custom rangePct', () => {
        const bounds = calculateMidpointBounds(mockCandle, 0.10);
        expect(bounds.minMidpoint).toBeCloseTo(90);
        expect(bounds.maxMidpoint).toBeCloseTo(110);
    });

    describe('filterSupportResistances', () => {
        const mockLevels: SupportResistanceData[] = [
            {
                priceDate: '2026-05-29',
                firstTouchDate: '2026-01-10',
                lastTouchDate: '2026-05-20',
                zoneBottom: 100,
                zoneTop: 105,
                zoneMidpoint: 102.5,
                touchCount: 4,
                levelType: 'SUPPORT',
            },
            {
                priceDate: '2026-05-29',
                zoneBottom: 73,
                zoneTop: 77,
                zoneMidpoint: 75, // Exact lower boundary
                touchCount: 2,
                levelType: 'SUPPORT',
            },
            {
                priceDate: '2026-05-29',
                zoneBottom: 123,
                zoneTop: 127,
                zoneMidpoint: 125, // Exact upper boundary
                touchCount: 3,
                levelType: 'RESISTANCE',
            },
            {
                priceDate: '2026-05-29',
                zoneBottom: 70,
                zoneTop: 74,
                zoneMidpoint: 74.99, // Below lower boundary
                touchCount: 1,
                levelType: 'SUPPORT',
            },
            {
                priceDate: '2026-05-29',
                zoneBottom: 124,
                zoneTop: 128,
                zoneMidpoint: 125.01, // Above upper boundary
                touchCount: 1,
                levelType: 'RESISTANCE',
            },
        ];

        it('filters levels within Typical Price window and includes exact boundaries', () => {
            const filtered = filterSupportResistances(mockLevels, mockCandle);
            expect(filtered).toHaveLength(3);
            expect(filtered.map((l) => l.zoneMidpoint)).toEqual([102.5, 75, 125]);
        });

        it('returns empty array if latestCandle is null or undefined', () => {
            expect(filterSupportResistances(mockLevels, undefined)).toEqual([]);
            expect(filterSupportResistances(mockLevels, null)).toEqual([]);
        });

        it('returns empty array if levels is empty', () => {
            expect(filterSupportResistances([], mockCandle)).toEqual([]);
        });

        it('supports custom rangePct parameter', () => {
            const filtered = filterSupportResistances(mockLevels, mockCandle, 0.05); // [95, 105]
            expect(filtered).toHaveLength(1);
            expect(filtered[0].zoneMidpoint).toBe(102.5);
        });
    });
});
