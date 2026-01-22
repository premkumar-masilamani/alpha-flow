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

export const getTickers = async (): Promise<Ticker[]> => {
  const response = await axios.get(`${API_BASE_URL}/tickers`);
  return response.data;
};

export const getMarketData = async (symbol: string): Promise<MarketData[]> => {
  const response = await axios.get(`${API_BASE_URL}/tickers/${symbol}/data`);
  return response.data;
};
