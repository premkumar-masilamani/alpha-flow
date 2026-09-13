export interface ChartPatternConfig {
    patternType: string;
    shortName: string;
    displayName: string;
    category: 'reversal' | 'continuation';
    sentiment: 'bullish' | 'bearish';
    color: string;
    description: string;
}

export const CHART_PATTERNS_CONFIG: Record<string, ChartPatternConfig> = {
    DOUBLE_TOP: {
        patternType: 'DOUBLE_TOP',
        shortName: 'DTP',
        displayName: 'Double Top',
        category: 'reversal',
        sentiment: 'bearish',
        color: '#ef4444',
        description: 'Bearish reversal pattern formed by two consecutive peaks at roughly the same level with an intermediate trough.',
    },
    DOUBLE_BOTTOM: {
        patternType: 'DOUBLE_BOTTOM',
        shortName: 'DBM',
        displayName: 'Double Bottom',
        category: 'reversal',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Bullish reversal pattern formed by two consecutive troughs at roughly the same level with an intermediate peak.',
    },
    TRIPLE_TOP: {
        patternType: 'TRIPLE_TOP',
        shortName: 'TTP',
        displayName: 'Triple Top',
        category: 'reversal',
        sentiment: 'bearish',
        color: '#ef4444',
        description: 'Bearish reversal pattern consisting of three distinct peaks at approximately the same resistance price level.',
    },
    TRIPLE_BOTTOM: {
        patternType: 'TRIPLE_BOTTOM',
        shortName: 'TBM',
        displayName: 'Triple Bottom',
        category: 'reversal',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Bullish reversal pattern consisting of three distinct troughs testing the same support level.',
    },
    HEAD_AND_SHOULDERS: {
        patternType: 'HEAD_AND_SHOULDERS',
        shortName: 'HNS',
        displayName: 'Head and Shoulders',
        category: 'reversal',
        sentiment: 'bearish',
        color: '#ef4444',
        description: 'Major bearish reversal pattern with a higher peak (head) between two roughly equal lower peaks (shoulders).',
    },
    INVERSE_HEAD_AND_SHOULDERS: {
        patternType: 'INVERSE_HEAD_AND_SHOULDERS',
        shortName: 'IHS',
        displayName: 'Inverse Head and Shoulders',
        category: 'reversal',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Major bullish reversal pattern with a lower trough (head) between two roughly equal higher troughs (shoulders).',
    },
    RISING_WEDGE: {
        patternType: 'RISING_WEDGE',
        shortName: 'RWG',
        displayName: 'Rising Wedge',
        category: 'reversal',
        sentiment: 'bearish',
        color: '#ef4444',
        description: 'Bearish pattern where price narrows between upward-sloping converging support and resistance lines.',
    },
    FALLING_WEDGE: {
        patternType: 'FALLING_WEDGE',
        shortName: 'FWG',
        displayName: 'Falling Wedge',
        category: 'reversal',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Bullish pattern where price narrows between downward-sloping converging resistance and support lines.',
    },
    ASCENDING_TRIANGLE: {
        patternType: 'ASCENDING_TRIANGLE',
        shortName: 'AST',
        displayName: 'Ascending Triangle',
        category: 'continuation',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Bullish continuation pattern formed by a flat horizontal resistance line and a rising ascending support line.',
    },
    DESCENDING_TRIANGLE: {
        patternType: 'DESCENDING_TRIANGLE',
        shortName: 'DST',
        displayName: 'Descending Triangle',
        category: 'continuation',
        sentiment: 'bearish',
        color: '#ef4444',
        description: 'Bearish continuation pattern formed by a flat horizontal support line and a falling descending resistance line.',
    },
    SYMMETRICAL_TRIANGLE: {
        patternType: 'SYMMETRICAL_TRIANGLE',
        shortName: 'SYT',
        displayName: 'Symmetrical Triangle',
        category: 'continuation',
        sentiment: 'bullish',
        color: '#3b82f6',
        description: 'Consolidation pattern characterized by converging trendlines connecting lower highs and higher lows.',
    },
    CUP_AND_HANDLE: {
        patternType: 'CUP_AND_HANDLE',
        shortName: 'CPH',
        displayName: 'Cup and Handle',
        category: 'continuation',
        sentiment: 'bullish',
        color: '#22c55e',
        description: 'Bullish continuation pattern showing a rounded U-shaped cup recovery followed by a slight downward drift (handle).',
    },
};

export const getChartPatternColor = (patternType: string, sentiment: string): string => {
    if (CHART_PATTERNS_CONFIG[patternType]) {
        return CHART_PATTERNS_CONFIG[patternType].color;
    }
    return sentiment.startsWith('BULLISH') ? '#22c55e' : '#ef4444';
};
