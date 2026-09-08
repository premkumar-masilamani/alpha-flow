import { describe, it, expect } from 'vitest';
import { CANDLESTICK_PATTERNS, getCandlestickPatternDetails } from './candlestickPatterns';

describe('candlestickPatterns', () => {
    it('contains all 68 patterns', () => {
        expect(CANDLESTICK_PATTERNS).toHaveLength(68);
    });

    it('has valid structure, psychology, outcome, and svgMarkup for every pattern', () => {
        for (const pattern of CANDLESTICK_PATTERNS) {
            expect(pattern.id).toBeTruthy();
            expect(pattern.title).toBeTruthy();
            expect(pattern.bars).toBeGreaterThanOrEqual(1);
            expect(pattern.structure).toBeTruthy();
            expect(pattern.psychology).toBeTruthy();
            expect(pattern.outcome).toBeTruthy();
            expect(pattern.svgMarkup).toContain('<line');
        }
    });

    it('looks up patterns by backend longName', () => {
        const tiu = getCandlestickPatternDetails({ longName: 'Three Inside Up' });
        expect(tiu).not.toBeNull();
        expect(tiu?.id).toBe('three-inside-up');
        expect(tiu?.bars).toBe(3);
        expect(tiu?.structure).toContain('Bullish Harami');

        const hammer = getCandlestickPatternDetails({ longName: 'Hammer' });
        expect(hammer).not.toBeNull();
        expect(hammer?.id).toBe('hammer');
        expect(hammer?.bars).toBe(1);

        const eveningStar = getCandlestickPatternDetails({ longName: 'Evening Star' });
        expect(eveningStar).not.toBeNull();
        expect(eveningStar?.id).toBe('evening-star');
        expect(eveningStar?.bars).toBe(3);
    });

    it('returns null for empty or non-matching pattern', () => {
        expect(getCandlestickPatternDetails(null)).toBeNull();
        expect(getCandlestickPatternDetails(undefined)).toBeNull();
        expect(getCandlestickPatternDetails({ longName: 'NonExistentPattern' })).toBeNull();
    });
});
