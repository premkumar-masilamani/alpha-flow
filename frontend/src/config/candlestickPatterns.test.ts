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

    it('has accurate definitions and visuals for Bullish Meeting Lines, Concealing Swallow, and Upside Gap Two Crows', () => {
        const meetingLines = CANDLESTICK_PATTERNS.find(p => p.id === 'bullish-meeting-lines');
        expect(meetingLines).toBeDefined();
        expect(meetingLines?.psychology).toContain('Bulls counter-attack the gap-down');
        expect(meetingLines?.psychology).toContain('to close level with Day 1');

        const concealingSwallow = CANDLESTICK_PATTERNS.find(p => p.id === 'concealing-swallow');
        expect(concealingSwallow).toBeDefined();
        expect(concealingSwallow?.structure).toContain('Four red candles');
        expect(concealingSwallow?.structure).toContain('fourth red engulfing candle');
        expect(concealingSwallow?.svgMarkup).not.toContain('#2ecc71');
        expect(concealingSwallow?.svgMarkup).toContain('#e74c3c');

        const upsideGapTwoCrows = CANDLESTICK_PATTERNS.find(p => p.id === 'upside-gap-two-crows');
        expect(upsideGapTwoCrows).toBeDefined();
        // Day 1 has green rect at y=45 (close=45). Day 3 has red rect at y=18 height=24 (close=42), closing above Day 1 close.
        expect(upsideGapTwoCrows?.svgMarkup).toContain('height="24"');
    });

    it('avoids the usage of black and white candles in descriptions, using green and red instead', () => {
        for (const pattern of CANDLESTICK_PATTERNS) {
            expect(pattern.structure.toLowerCase()).not.toMatch(/\b(black|white)\s+(candle|body|marubozu)/);
            expect(pattern.psychology.toLowerCase()).not.toMatch(/\b(black|white)\s+(candle|body|marubozu)/);
            expect(pattern.outcome.toLowerCase()).not.toMatch(/\b(black|white)\s+(candle|body|marubozu)/);
        }
    });
});
