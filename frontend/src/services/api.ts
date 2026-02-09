import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
    type: 'CRYPTO' | 'STOCKS' | 'COMMODITY';
}

export interface MarketData {
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

export interface BacktestSignal {
    date: string;
    action: 'ENTER_LONG' | 'ENTER_SHORT' | 'EXIT';
}

export const getTickers = async (): Promise<Ticker[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers`);
    return response.data;
};

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const marketDataCache: { [symbol: string]: { data: MarketData[]; timestamp: number } } = {};
const renkoDataCache: { [symbol: string]: { data: RenkoData; timestamp: number } } = {};
const signalDataCache: { [symbol: string]: { data: Record<string, BacktestSignal[]>; timestamp: number } } = {};

export const getMarketData = async (symbol: string): Promise<MarketData[]> => {
    const now = Date.now();
    if (marketDataCache[symbol] && (now - marketDataCache[symbol].timestamp < CACHE_DURATION)) {
        return marketDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`);
    marketDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getRenkoData = async (symbol: string): Promise<RenkoData> => {
    const now = Date.now();
    if (renkoDataCache[symbol] && (now - renkoDataCache[symbol].timestamp < CACHE_DURATION)) {
        return renkoDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/renko`);
    renkoDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};

export const getBacktestSignals = async (symbol: string): Promise<Record<string, BacktestSignal[]>> => {
    const now = Date.now();
    if (signalDataCache[symbol] && (now - signalDataCache[symbol].timestamp < CACHE_DURATION)) {
        return signalDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/signals`);
    signalDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};
