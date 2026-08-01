export interface IndicatorColorRule {
    default?: string;
    byParams?: Record<string, string>;
    byOutput?: Record<string, string>;
    bySourceAndParams?: Record<string, string>;
}

export type IndicatorColorsConfig = Record<string, IndicatorColorRule>;

/**
 * Hardcoded indicator color mapping configurations.
 * Grouped hierarchically by indicator type and nested by lookup criteria.
 */
export const INDICATOR_COLORS: IndicatorColorsConfig = {
    EMA: {
        byParams: {
            'period=5': '#3b82f6',   // BLUE
            'period=13': '#ef4444',  // RED
            'period=26': '#22c55e',  // GREEN
        }
    },
    SMA: {
        bySourceAndParams: {
            'VOLUME|period=20': '#3b82f6' // BLUE
        }
    },
    MACD: {
        byOutput: {
            macd: '#3b82f6',   // Blue
            signal: '#f97316'  // Orange
        }
    },
    RSI: {
        default: '#a855f7' // Purple
    },
    STOCHASTIC: {
        byOutput: {
            k: '#3b82f6', // Blue
            d: '#f97316'  // Orange
        }
    },
    BB: {
        byOutput: {
            upper: '#ef4444',  // Red
            middle: '#3b82f6', // Blue
            lower: '#22c55e'   // Green
        }
    }
};
