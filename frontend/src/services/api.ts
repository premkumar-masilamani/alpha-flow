import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
    type: 'CRYPTO' | 'STOCKS' | 'COMMODITY';
}

export interface Candle {
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

export interface RenkoBrick {
    date: string;
    low: number;
    high: number;
    direction: 'up' | 'down';
    trend: number;
    zone: number;
}

export interface RenkoData {
    bricks: RenkoBrick[];
    current_price: number;
    stop_loss_price: number;
    brick_size: number;
}

export interface BacktestSignalss {
    strategy: string;
    date: string;
    action: 'ENTER_LONG' | 'ENTER_SHORT' | 'EXIT';
}

export const getTickers = async (): Promise<Ticker[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers`);
    return response.data;
};

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const candleCache: { [symbol: string]: { data: Candle[]; timestamp: number } } = {};
const renkoCache: { [symbol: string]: { data: RenkoData; timestamp: number } } = {};
const signalsCache: { [symbol: string]: { data: Record<string, BacktestSignalss[]>; timestamp: number } } = {};

export const getCandles = async (symbol: string): Promise<Candle[]> => {
    const now = Date.now();
    if (candleCache[symbol] && (now - candleCache[symbol].timestamp < CACHE_DURATION)) {
        return candleCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/candles`);
    candleCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getRenkoData = async (symbol: string): Promise<RenkoData> => {
    const now = Date.now();
    if (renkoCache[symbol] && (now - renkoCache[symbol].timestamp < CACHE_DURATION)) {
        return renkoCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/renko`);
    renkoCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getBacktestSignals = async (symbol: string): Promise<Record<string, BacktestSignalss[]>> => {
    const now = Date.now();
    if (signalsCache[symbol] && (now - signalsCache[symbol].timestamp < CACHE_DURATION)) {
        return signalsCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/signals`);
    signalsCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};
