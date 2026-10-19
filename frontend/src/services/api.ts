import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export const CHART_WINDOW = 250;

export type TickerType = 'STOCK' | 'INDEX';
export type Country = 'US' | 'IN';

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
    type?: TickerType;
    country?: Country;
}

export interface DailyCandleData {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    vol: number;
}

export const getTickers = async (): Promise<Ticker[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers`);
    return response.data;
};

// The backend re-syncs prices hourly, so cache for at most an hour to avoid serving stale data.
// For intraday 15m data, cache for at most 30 seconds.
const CACHE_DURATION = 60 * 60 * 1000; // 1 hour
const INTRADAY_CACHE_DURATION = 30 * 1000; // 30 seconds
const MAX_CACHE_ENTRIES = 50;
// Map preserves insertion order, which we use as a simple LRU to bound memory growth.
const candleDataCache = new Map<string, { data: DailyCandleData[]; timestamp: number }>();

export type Timeframe = 'DAILY' | 'WEEKLY' | '15M';

export const getCandleData = async (
    symbol: string,
    timeframe: Timeframe = 'DAILY',
    page: number = 0,
    size: number = CHART_WINDOW,
    forceFresh = false,
    endDate?: string
): Promise<DailyCandleData[]> => {
    const now = Date.now();
    const cacheKey = `${symbol}:${timeframe}:${page}:${size ?? 'default'}:${endDate ?? 'latest'}`;
    const cached = candleDataCache.get(cacheKey);
    const ttl = timeframe === '15M' ? INTRADAY_CACHE_DURATION : CACHE_DURATION;
    if (!forceFresh && cached && (now - cached.timestamp < ttl)) {
        // Mark as most-recently-used.
        candleDataCache.delete(cacheKey);
        candleDataCache.set(cacheKey, cached);
        return cached.data;
    }

    const isIntraday = timeframe === '15M';
    const url = isIntraday
        ? `${API_BASE_URL}/tickers/${symbol}/intraday`
        : `${API_BASE_URL}/tickers/${symbol}/data`;
    const params: Record<string, string | number> = { timeframe: timeframe.toLowerCase(), page, size };
    if (endDate && isIntraday) {
        params.endDate = endDate;
    }
    const response = await axios.get(url, { params });
    candleDataCache.set(cacheKey, {data: response.data, timestamp: now});

    // Evict the least-recently-used entries if we exceed the cap.
    while (candleDataCache.size > MAX_CACHE_ENTRIES) {
        const oldestKey = candleDataCache.keys().next().value;
        if (oldestKey === undefined) break;
        candleDataCache.delete(oldestKey);
    }

    return response.data;
};

// One configured (indicator, source, params) combo from the discovery endpoint.
export interface IndicatorConfig {
    timeframe: Timeframe;
    type: string;
    source: string;
    params: string;
    label: string;
    bounds?: {
        lower?: number;
        upper?: number;
    };
}

// One bar's reading; `values` is keyed by output name (e.g. MACD -> macd/signal/histogram).
export interface IndicatorPoint {
    date: string;
    values: Record<string, number>;
}

export interface IndicatorSeries {
    type: string;
    source: string;
    params: string;
    label: string;
    points: IndicatorPoint[];
}

// Stable key identifying a combo across the config and series endpoints.
export const indicatorKey = (i: {type: string; source: string; params: string}): string =>
    `${i.type}|${i.source}|${i.params}`;

export const getIndicatorConfigs = async (): Promise<IndicatorConfig[]> => {
    const response = await axios.get(`${API_BASE_URL}/indicator-definitions`);
    return response.data;
};

// Cache indicator series per symbol+timeframe, mirroring the candle cache (backend re-syncs hourly).
const indicatorCache = new Map<string, {data: IndicatorSeries[]; timestamp: number}>();

export const getIndicatorSeries = async (
    symbol: string,
    timeframe: Timeframe,
    page: number = 0,
    size: number = CHART_WINDOW
): Promise<IndicatorSeries[]> => {
    const now = Date.now();
    const cacheKey = `${symbol}:${timeframe}:${page}:${size ?? 'default'}`;
    const cached = indicatorCache.get(cacheKey);
    if (cached && (now - cached.timestamp < CACHE_DURATION)) {
        indicatorCache.delete(cacheKey);
        indicatorCache.set(cacheKey, cached);
        return cached.data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/indicators`, {
        params: { timeframe: timeframe.toLowerCase(), page, size }
    });
    indicatorCache.set(cacheKey, {data: response.data, timestamp: now});

    while (indicatorCache.size > MAX_CACHE_ENTRIES) {
        const oldestKey = indicatorCache.keys().next().value;
        if (oldestKey === undefined) break;
        indicatorCache.delete(oldestKey);
    }

    return response.data;
};

export interface TechnicalAnalysisData {
    symbol: string;
    candle: DailyCandleData | null;
    dailyIndicators: IndicatorSeries[];
    weeklyIndicators: IndicatorSeries[];
}

export const getTechnicalAnalysis = async (symbol: string): Promise<TechnicalAnalysisData> => {
    const [candles, dailyInds, weeklyInds] = await Promise.all([
        getCandleData(symbol, 'DAILY', 0, 1),
        getIndicatorSeries(symbol, 'DAILY', 0, 1),
        getIndicatorSeries(symbol, 'WEEKLY', 0, 1)
    ]);

    return {
        symbol,
        candle: candles.length > 0 ? candles[0] : null,
        dailyIndicators: dailyInds,
        weeklyIndicators: weeklyInds
    };
};

export const SENTIMENT_TYPES = {
    BULLISH_REVERSAL: 'BULLISH_REVERSAL',
    BEARISH_REVERSAL: 'BEARISH_REVERSAL',
    BULLISH_CONTINUATION: 'BULLISH_CONTINUATION',
    BEARISH_CONTINUATION: 'BEARISH_CONTINUATION',
} as const;

export type SentimentType = typeof SENTIMENT_TYPES[keyof typeof SENTIMENT_TYPES];

export interface CandlestickPatternData {
    date: string;
    shortName: string;
    longName: string;
    sentiment: SentimentType;
}

export const getCandlestickPatterns = async (
    symbol: string,
    timeframe: Timeframe = 'DAILY',
    page: number = 0
): Promise<CandlestickPatternData[]> => {
    const url = `${API_BASE_URL}/tickers/${symbol}/candlestick-patterns`;
    const response = await axios.get(url, { params: { timeframe: timeframe.toLowerCase(), page } });
    return response.data;
};

export const LEVEL_TYPES = {
    SUPPORT: 'SUPPORT',
    RESISTANCE: 'RESISTANCE',
} as const;

export type LevelType = typeof LEVEL_TYPES[keyof typeof LEVEL_TYPES];

export interface SupportResistanceData {
    priceDate: string;
    firstTouchDate?: string;
    lastTouchDate?: string;
    zoneBottom: number;
    zoneTop: number;
    zoneMidpoint: number;
    levelType: LevelType;
    touchCount: number;
}

export const getSupportResistances = async (
    symbol: string,
    timeframe: Timeframe = 'DAILY',
    date: string
): Promise<SupportResistanceData[]> => {
    const normalizedDate = date.length >= 10 ? date.substring(0, 10) : date;
    const url = `${API_BASE_URL}/tickers/${symbol}/support-resistances`;
    const response = await axios.get(url, { params: { timeframe: timeframe.toLowerCase(), date: normalizedDate } });
    return response.data;
};

export const CHART_PATTERN_STATUSES = {
    IN_PROGRESS: 'IN_PROGRESS',
    COMPLETED: 'COMPLETED',
    TARGET_REACHED: 'TARGET_REACHED',
    INVALIDATED: 'INVALIDATED',
} as const;

export type ChartPatternStatus = typeof CHART_PATTERN_STATUSES[keyof typeof CHART_PATTERN_STATUSES];

export interface ChartPatternPivotData {
    date: string;
    price: number;
    type: 'HIGH' | 'LOW';
    role: string;
}

export interface ChartPatternData {
    id: number;
    patternType: string;
    shortName: string;
    displayName: string;
    sentiment: SentimentType;
    status: ChartPatternStatus;
    startDate: string;
    endDate: string;
    breakoutDate?: string | null;
    necklineSlope?: number | null;
    necklinePrice?: number | null;
    targetPrice?: number | null;
    stopLossPrice?: number | null;
    invalidationPrice?: number | null;
    pivotPoints: ChartPatternPivotData[];
}

export const getChartPatterns = async (
    symbol: string,
    timeframe: Timeframe = 'DAILY',
    statuses?: ChartPatternStatus | ChartPatternStatus[],
    page: number = 0
): Promise<ChartPatternData[]> => {
    const url = `${API_BASE_URL}/tickers/${symbol}/chart-patterns`;
    const params: Record<string, string | number> = { timeframe: timeframe.toLowerCase(), page };
    if (statuses) {
        params.statuses = Array.isArray(statuses) ? statuses.join(',') : statuses;
    }
    const response = await axios.get(url, { params });
    return response.data;
};

export interface MarketHoliday {
    id: number;
    country: 'IN' | 'US';
    holidayDate: string;
    name: string;
}

export const STORAGE_KEY_MARKET_HOLIDAYS = 'alphaflow_market_holidays';
export const MARKET_HOLIDAYS_TTL_MS = 24 * 60 * 60 * 1000;

export interface CachedMarketHolidays {
    timestamp: number;
    expiresAt: number;
    holidays: MarketHoliday[];
}

export const getMarketHolidays = async (): Promise<MarketHoliday[]> => {
    const url = `${API_BASE_URL}/markets/holidays`;
    const response = await axios.get(url);
    return response.data;
};

let inFlightHolidaysPromise: Promise<MarketHoliday[]> | null = null;

export const getCachedMarketHolidays = async (): Promise<MarketHoliday[]> => {
    if (typeof window !== 'undefined') {
        try {
            const raw = localStorage.getItem(STORAGE_KEY_MARKET_HOLIDAYS);
            if (raw) {
                const parsed: CachedMarketHolidays = JSON.parse(raw);
                if (
                    parsed &&
                    typeof parsed.expiresAt === 'number' &&
                    Date.now() < parsed.expiresAt &&
                    Array.isArray(parsed.holidays) &&
                    parsed.holidays.length > 0
                ) {
                    return parsed.holidays;
                }
            }
        } catch (error) {
            console.warn('Failed to read market holidays from localStorage:', error);
        }
    }

    if (!inFlightHolidaysPromise) {
        inFlightHolidaysPromise = getMarketHolidays()
            .then((holidays) => {
                if (typeof window !== 'undefined' && Array.isArray(holidays) && holidays.length > 0) {
                    try {
                        const cachePayload: CachedMarketHolidays = {
                            timestamp: Date.now(),
                            expiresAt: Date.now() + MARKET_HOLIDAYS_TTL_MS,
                            holidays,
                        };
                        localStorage.setItem(STORAGE_KEY_MARKET_HOLIDAYS, JSON.stringify(cachePayload));
                    } catch (error) {
                        console.warn('Failed to save market holidays to localStorage:', error);
                    }
                }
                return holidays;
            })
            .finally(() => {
                inFlightHolidaysPromise = null;
            });
    }

    return inFlightHolidaysPromise;
};



