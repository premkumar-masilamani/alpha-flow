import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
    type: 'CRYPTO' | 'US-EQUITY' | 'IN-EQUITY' | 'COMMODITY';
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

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const candleDataCache: { [key: string]: { data: DailyCandleData[]; timestamp: number } } = {};

export const getCandleData = async (symbol: string, timeframe: 'daily' | 'weekly' = 'daily'): Promise<DailyCandleData[]> => {
    const now = Date.now();
    const cacheKey = `${symbol}_${timeframe}`;
    if (candleDataCache[cacheKey] && (now - candleDataCache[cacheKey].timestamp < CACHE_DURATION)) {
        return candleDataCache[cacheKey].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`, {
        params: { timeframe }
    });
    candleDataCache[cacheKey] = {data: response.data, timestamp: now};
    return response.data;
};
