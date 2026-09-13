import { describe, it, expect } from 'vitest';
import {
    CHART_PATTERNS,
    CHART_PATTERN_GROUPS,
    CHART_PATTERNS_CONFIG,
    getChartPatternColor,
    getChartPatternDetails,
} from './chartPatterns';

describe('chartPatterns config & encyclopedia library', () => {
    it('contains all 12 classical chart patterns in CHART_PATTERNS_CONFIG', () => {
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
            'CUP_AND_HANDLE',
        ];
        for (const key of expectedPatterns) {
            expect(CHART_PATTERNS_CONFIG[key]).toBeDefined();
            expect(CHART_PATTERNS_CONFIG[key].shortName).toBeTruthy();
            expect(CHART_PATTERNS_CONFIG[key].shortName).toHaveLength(3);
            expect(CHART_PATTERNS_CONFIG[key].displayName).toBeTruthy();
            expect(CHART_PATTERNS_CONFIG[key].description).toBeTruthy();
        }
    });

    it('contains all 12 detailed patterns with valid metadata and SVG markup in CHART_PATTERNS', () => {
        expect(CHART_PATTERNS).toHaveLength(12);
        for (const pattern of CHART_PATTERNS) {
            expect(pattern.id).toBeTruthy();
            expect(pattern.patternType).toBeTruthy();
            expect(pattern.title).toBeTruthy();
            expect(pattern.shortName).toHaveLength(3);
            expect(['reversal', 'continuation']).toContain(pattern.category);
            expect(['bullish', 'bearish']).toContain(pattern.sentiment);
            expect([
                'bullish-reversals',
                'bearish-reversals',
                'bullish-continuations',
                'bearish-continuations',
            ]).toContain(pattern.group);
            expect(pattern.groupName).toBeTruthy();
            expect(pattern.color).toMatch(/^#[0-9a-fA-F]{6}$/);
            expect(pattern.description.length).toBeGreaterThan(10);
            expect(pattern.structure.length).toBeGreaterThan(10);
            expect(pattern.psychology.length).toBeGreaterThan(10);
            expect(pattern.outcome.length).toBeGreaterThan(10);
            expect(pattern.targetRule.length).toBeGreaterThan(10);
            expect(pattern.svgMarkup).toBeTruthy();
            expect(pattern.svgMarkup.length).toBeGreaterThan(20);
        }
    });

    it('defines 4 canonical pattern groups with badge classes', () => {
        expect(CHART_PATTERN_GROUPS).toHaveLength(4);
        const groupIds = CHART_PATTERN_GROUPS.map((g) => g.id);
        expect(groupIds).toEqual([
            'bullish-reversals',
            'bearish-reversals',
            'bullish-continuations',
            'bearish-continuations',
        ]);
        for (const group of CHART_PATTERN_GROUPS) {
            expect(group.title).toBeTruthy();
            expect(group.subtitle).toBeTruthy();
            expect(group.badgeClass).toBeTruthy();
        }
    });

    it('resolves pattern details via getChartPatternDetails across various inputs', () => {
        // By patternType string
        const doubleTopByType = getChartPatternDetails('DOUBLE_TOP');
        expect(doubleTopByType).toBeDefined();
        expect(doubleTopByType?.id).toBe('double-top');
        expect(doubleTopByType?.shortName).toBe('DTP');

        // By ID string
        const doubleBottomById = getChartPatternDetails('double-bottom');
        expect(doubleBottomById).toBeDefined();
        expect(doubleBottomById?.patternType).toBe('DOUBLE_BOTTOM');

        // By displayName string
        const hnsByName = getChartPatternDetails('Head and Shoulders');
        expect(hnsByName).toBeDefined();
        expect(hnsByName?.id).toBe('head-and-shoulders');

        // By shortName string
        const cupByShort = getChartPatternDetails('CPH');
        expect(cupByShort).toBeDefined();
        expect(cupByShort?.patternType).toBe('CUP_AND_HANDLE');

        // By object with patternType
        const objPattern = getChartPatternDetails({
            patternType: 'ASCENDING_TRIANGLE',
            displayName: 'Ascending Triangle',
        });
        expect(objPattern).toBeDefined();
        expect(objPattern?.shortName).toBe('AST');

        // Non-existent or null
        expect(getChartPatternDetails('UNKNOWN_PATTERN')).toBeNull();
        expect(getChartPatternDetails(null)).toBeNull();
        expect(getChartPatternDetails(undefined)).toBeNull();
    });

    it('returns configured color or fallback sentiment color', () => {
        expect(getChartPatternColor('DOUBLE_TOP', 'BEARISH_REVERSAL')).toBe('#ef4444');
        expect(getChartPatternColor('DOUBLE_BOTTOM', 'BULLISH_REVERSAL')).toBe('#22c55e');
        expect(getChartPatternColor('UNKNOWN', 'BULLISH_CONTINUATION')).toBe('#22c55e');
        expect(getChartPatternColor('UNKNOWN', 'BEARISH_CONTINUATION')).toBe('#ef4444');
    });
});
