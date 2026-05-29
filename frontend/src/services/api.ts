import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

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

export const getCandleData = async (symbol: string): Promise<DailyCandleData[]> => {
    const now = Date.now();
    const cached = candleDataCache.get(symbol);
    if (cached && (now - cached.timestamp < CACHE_DURATION)) {
        // Mark as most-recently-used.
        candleDataCache.delete(symbol);
        candleDataCache.set(symbol, cached);
        return cached.data;
    }

    // Only cache on success; a failed request propagates without evicting/poisoning the cache.
    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`);
    candleDataCache.set(symbol, {data: response.data, timestamp: now});

    // Evict the least-recently-used entries if we exceed the cap.
    while (candleDataCache.size > MAX_CACHE_ENTRIES) {
        const oldestKey = candleDataCache.keys().next().value;
        if (oldestKey === undefined) break;
        candleDataCache.delete(oldestKey);
    }

    return response.data;
};
