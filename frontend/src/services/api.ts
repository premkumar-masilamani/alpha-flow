import axios from 'axios';

const API_BASE_URL = `${import.meta.env.NEXT_PUBLIC_API_URL || ''}/api`;

export interface Ticker {
  ticker_id: number;
  symbol: string;
  name: string;
  date: string;
  is_active: boolean;
}

export interface MarketData {
  date: string;
  open: number;
  high: number;
  low: number;
  close: number;
  vol: number;
  vwap: number;
  poc: number;
  vah: number;
  val: number;
  bvs: number;
  bcs: number;
}

export interface RenkoBrick {
  date: string;
  low: number;
  high: number;
  direction: 'up' | 'down';
  trend: number;
}

export interface RenkoData {
  bricks: RenkoBrick[];
  current_price: number;
  sl_price: number;
  renko_brick_size: number;
}

export const getTickers = async (): Promise<Ticker[]> => {
  const response = await axios.get(`${API_BASE_URL}/tickers`);
  return response.data;
};

const CACHE_DURATION = 6 * 60 * 60 * 1000; // 6 hours
const marketDataCache: { [symbol: string]: { data: MarketData[]; timestamp: number } } = {};
const renkoDataCache: { [symbol: string]: { data: RenkoData; timestamp: number } } = {};

export const getMarketData = async (symbol: string): Promise<MarketData[]> => {
  const now = Date.now();
  if (marketDataCache[symbol] && now - marketDataCache[symbol].timestamp < CACHE_DURATION) {
    return marketDataCache[symbol].data;
  }

  const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`);
  const data = response.data;
  marketDataCache[symbol] = { data, timestamp: now };
  return data;
};

export const getRenkoData = async (symbol: string): Promise<RenkoData> => {
  const now = Date.now();
  if (renkoDataCache[symbol] && now - renkoDataCache[symbol].timestamp < CACHE_DURATION) {
    return renkoDataCache[symbol].data;
  }

  const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/renko`);
  const data = response.data;
  renkoDataCache[symbol] = { data, timestamp: now };
  return data;
};
