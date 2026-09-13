import { describe, it, expect } from 'vitest';
import { CHART_PATTERNS_CONFIG, getChartPatternColor } from './chartPatterns';

describe('chartPatterns config', () => {
    it('contains all 12 classical chart patterns', () => {
        expect(Object.keys(CHART_PATTERNS_CONFIG)).toHaveLength(12);
        const expectedPatterns = [
            'DOUBLE_TOP',
            'DOUBLE_BOTTOM',
            'TRIPLE_TOP',
            'TRIPLE_BOTTOM',
            'HEAD_AND_SHOULDERS',
            'INVERSE_HEAD_AND_SHOULDERS',
            'RISING_WEDGE',
            'FALLING_WEDGE',
            'ASCENDING_TRIANGLE',
            'DESCENDING_TRIANGLE',
            'SYMMETRICAL_TRIANGLE',
            'CUP_AND_HANDLE'
        ];
        for (const key of expectedPatterns) {
            expect(CHART_PATTERNS_CONFIG[key]).toBeDefined();
            expect(CHART_PATTERNS_CONFIG[key].shortName).toBeTruthy();
            expect(CHART_PATTERNS_CONFIG[key].shortName).toHaveLength(3);
            expect(CHART_PATTERNS_CONFIG[key].displayName).toBeTruthy();
            expect(CHART_PATTERNS_CONFIG[key].description).toBeTruthy();
        }
    });

    it('returns configured color or fallback sentiment color', () => {
        expect(getChartPatternColor('DOUBLE_TOP', 'BEARISH_REVERSAL')).toBe('#ef4444');
        expect(getChartPatternColor('DOUBLE_BOTTOM', 'BULLISH_REVERSAL')).toBe('#22c55e');
        expect(getChartPatternColor('UNKNOWN', 'BULLISH_CONTINUATION')).toBe('#22c55e');
        expect(getChartPatternColor('UNKNOWN', 'BEARISH_CONTINUATION')).toBe('#ef4444');
    });
});
