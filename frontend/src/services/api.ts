import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export const CHART_WINDOW = 250;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
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
const CACHE_DURATION = 60 * 60 * 1000; // 1 hour
const MAX_CACHE_ENTRIES = 50;
// Map preserves insertion order, which we use as a simple LRU to bound memory growth.
const candleDataCache = new Map<string, { data: DailyCandleData[]; timestamp: number }>();

export const getCandleData = async (
    symbol: string,
    timeframe: Timeframe = 'DAILY',
    page: number = 0,
    size: number = CHART_WINDOW
): Promise<DailyCandleData[]> => {
    const now = Date.now();
    const cacheKey = `${symbol}:${timeframe}:${page}:${size ?? 'default'}`;
    const cached = candleDataCache.get(cacheKey);
    if (cached && (now - cached.timestamp < CACHE_DURATION)) {
        // Mark as most-recently-used.
        candleDataCache.delete(cacheKey);
        candleDataCache.set(cacheKey, cached);
        return cached.data;
    }

    const url = `${API_BASE_URL}/tickers/${symbol}/data`;
    const response = await axios.get(url, { params: { timeframe: timeframe.toLowerCase(), page, size } });
    candleDataCache.set(cacheKey, {data: response.data, timestamp: now});

    // Evict the least-recently-used entries if we exceed the cap.
    while (candleDataCache.size > MAX_CACHE_ENTRIES) {
        const oldestKey = candleDataCache.keys().next().value;
        if (oldestKey === undefined) break;
        candleDataCache.delete(oldestKey);
    }

    return response.data;
};

export type Timeframe = 'DAILY' | 'WEEKLY' | 'MONTHLY';

// One configured (indicator, source, params) combo from the discovery endpoint.
export interface IndicatorConfig {
    timeframe: Timeframe;
    type: string;
    source: string;
    params: string;
    label: string;
    upperBound?: number;
    lowerBound?: number;
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

export interface SRTouchPoint {
    date: string;
    price: number;
}

export interface SupportResistanceLine {
    currentType: 'SUPPORT' | 'RESISTANCE';
    importance: number;
    touchPoints: {date: string; price: number}[];
    timeframe?: 'DAILY' | 'WEEKLY' | 'MONTHLY';
}

// Stable key identifying a combo across the config and series endpoints.
export const indicatorKey = (i: {type: string; source: string; params: string}): string =>
    `${i.type}|${i.source}|${i.params}`;

export const getIndicatorConfigs = async (): Promise<IndicatorConfig[]> => {
    const response = await axios.get(`${API_BASE_URL}/indicators`);
    return response.data.map((config: IndicatorConfig) => {
        if (config.type === 'RSI') {
            return {
                ...config,
                upperBound: 70,
                lowerBound: 30,
            };
        }
        if (config.type === 'STOCHASTIC') {
            return {
                ...config,
                upperBound: 80,
                lowerBound: 20,
            };
        }
        return config;
    });
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

export const getSupportResistance = async (
    symbol: string,
    timeframe: Timeframe
): Promise<SupportResistanceLine[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/sr`, {
        params: { timeframe: timeframe.toLowerCase() }
    });
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
