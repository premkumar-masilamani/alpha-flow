import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
    type: 'CRYPTO' | 'STOCKS' | 'COMMODITY';
}

export interface CandleData {
    date: string;
    open: number;
    high: number;
    low: number;
    close: number;
    vol: number;
    vwap: number;
    capital_poc: number;
    capital_vah: number;
    capital_val: number;
    buyer_capital: number;
    total_capital: number;
}

export interface RenkoData {
    date: string;
    low: number;
    high: number;
    direction: 'up' | 'down';
    trend: number;
    zone: number;
}

export interface RenkoDataResponse {
    bricks: RenkoData[];
    current_price: number;
    stop_loss_price: number;
    brick_size: number;
}

export interface BacktestSignal {
    date: string;
    action: 'ENTER_LONG' | 'ENTER_SHORT' | 'EXIT';
}

export const getTickers = async (): Promise<Ticker[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers`);
    return response.data;
};

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const candleDataCache: { [symbol: string]: { data: CandleData[]; timestamp: number } } = {};
const renkoDataCache: { [symbol: string]: { data: RenkoDataResponse; timestamp: number } } = {};
const signalCache: { [symbol: string]: { data: Record<string, BacktestSignal[]>; timestamp: number } } = {};

export const getCandleData = async (symbol: string): Promise<CandleData[]> => {
    const now = Date.now();
    if (candleDataCache[symbol] && (now - candleDataCache[symbol].timestamp < CACHE_DURATION)) {
        return candleDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`);
    candleDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getRenkoData = async (symbol: string): Promise<RenkoDataResponse> => {
    const now = Date.now();
    if (renkoDataCache[symbol] && (now - renkoDataCache[symbol].timestamp < CACHE_DURATION)) {
        return renkoDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/renko`);
    renkoDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getBacktestSignal = async (symbol: string): Promise<Record<string, BacktestSignal[]>> => {
    const now = Date.now();
    if (signalCache[symbol] && (now - signalCache[symbol].timestamp < CACHE_DURATION)) {
        return signalCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/signals`);
    signalCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};
