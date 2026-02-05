import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
    id: number;
    symbol: string;
    name: string;
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

export interface BacktestTrade {
    backtestTradeId: number;
    strategyName: string;
    side: 'LONG' | 'SHORT' | 'NONE';
    entryDate: string;
    entryPrice: number;
    exitDate: string;
    exitPrice: number;
    quantity: number;
    pnl: number;
    pnlPct: number;
    holdingBars: number;
}

export const getTickers = async (): Promise<Ticker[]> => {
    const response = await axios.get(`${API_BASE_URL}/tickers`);
    return response.data;
};

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const marketDataCache: { [symbol: string]: { data: MarketData[]; timestamp: number } } = {};
const renkoDataCache: { [symbol: string]: { data: RenkoData; timestamp: number } } = {};
const tradeDataCache: { [symbol: string]: { data: BacktestTrade[]; timestamp: number } } = {};

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

export const getBacktestTrades = async (symbol: string): Promise<BacktestTrade[]> => {
    const now = Date.now();
    if (tradeDataCache[symbol] && (now - tradeDataCache[symbol].timestamp < CACHE_DURATION)) {
        return tradeDataCache[symbol].data;
    }

    const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/trades`);
    tradeDataCache[symbol] = {data: response.data, timestamp: now};
    return response.data;
};
