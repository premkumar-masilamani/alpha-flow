import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import {
    getTickers,
    getCandleData,
    getIndicatorConfigs,
    getIndicatorSeries,
    getTechnicalAnalysis,
    indicatorKey,
    type Ticker,
    type DailyCandleData,
    type IndicatorConfig,
    type IndicatorSeries
} from './api';

// Mock axios completely
vi.mock('axios');
const mockedAxios = vi.mocked(axios, true);

describe('API Service Layer Tests', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    describe('getTickers', () => {
        it('should successfully fetch list of active tickers', async () => {
            const mockTickers: Ticker[] = [
                { id: 1, symbol: 'BTC-USD', name: 'Bitcoin USD' },
                { id: 2, symbol: 'AAPL', name: 'Apple Inc.' }
            ];

            mockedAxios.get.mockResolvedValueOnce({ data: mockTickers });

            const result = await getTickers();
            expect(result).toEqual(mockTickers);
            expect(mockedAxios.get).toHaveBeenCalledWith(expect.stringContaining('/tickers'));
        });
    });

    describe('getCandleData', () => {
        it('should fetch and cache daily candle data', async () => {
            const mockCandles: DailyCandleData[] = [
                { date: '2026-06-01', open: 100, high: 110, low: 90, close: 105, vol: 5000 }
            ];

            mockedAxios.get.mockResolvedValueOnce({ data: mockCandles });

            // First call - should trigger network request
            const result1 = await getCandleData('AAPL', 'DAILY', 0);
            expect(result1).toEqual(mockCandles);
            expect(mockedAxios.get).toHaveBeenCalledTimes(1);

            // Second call - should serve from cache
            const result2 = await getCandleData('AAPL', 'DAILY', 0);
            expect(result2).toEqual(mockCandles);
            expect(mockedAxios.get).toHaveBeenCalledTimes(1); // Call count should still be 1
        });

        it('should request weekly data endpoint when timeframe is WEEKLY', async () => {
            const mockCandles: DailyCandleData[] = [];
            mockedAxios.get.mockResolvedValueOnce({ data: mockCandles });

            await getCandleData('BTC-USD', 'WEEKLY', 0);
            expect(mockedAxios.get).toHaveBeenCalledWith(
                expect.stringContaining('/tickers/BTC-USD/data'),
                expect.objectContaining({ params: expect.objectContaining({ timeframe: 'weekly' }) })
            );
        });
    });

    describe('getIndicatorConfigs', () => {
        it('should fetch list of active indicator configurations', async () => {
            const mockConfigs: IndicatorConfig[] = [
                { timeframe: 'DAILY', type: 'SMA', source: 'CLOSE', params: 'period=10', label: 'SMA(10)' }
            ];

            mockedAxios.get.mockResolvedValueOnce({ data: mockConfigs });

            const result = await getIndicatorConfigs();
            expect(result).toEqual(mockConfigs);
            expect(mockedAxios.get).toHaveBeenCalledWith(expect.stringContaining('/indicator-definitions'));
        });

        it('should enrich RSI and STOCHASTIC indicator configs with bounds', async () => {
            const mockConfigs = [
                { timeframe: 'DAILY' as const, type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI(14)' },
                { timeframe: 'DAILY' as const, type: 'STOCHASTIC', source: 'CLOSE', params: 'k=14,kSmooth=3,dSmooth=3', label: 'Stoch(14,3,3)' }
            ];

            mockedAxios.get.mockResolvedValueOnce({ data: mockConfigs });

            const result = await getIndicatorConfigs();
            expect(result).toEqual([
                { timeframe: 'DAILY', type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI(14)', upperBound: 70, lowerBound: 30 },
                { timeframe: 'DAILY', type: 'STOCHASTIC', source: 'CLOSE', params: 'k=14,kSmooth=3,dSmooth=3', label: 'Stoch(14,3,3)', upperBound: 80, lowerBound: 20 }
            ]);
        });
    });

    describe('getIndicatorSeries', () => {
        it('should fetch and cache indicator series data', async () => {
            const mockSeries: IndicatorSeries[] = [
                {
                    type: 'RSI',
                    source: 'CLOSE',
                    params: 'period=14',
                    label: 'RSI(14)',
                    points: [{ date: '2026-06-01', values: { value: 50 } }]
                }
            ];

            mockedAxios.get.mockResolvedValueOnce({ data: mockSeries });

            const result1 = await getIndicatorSeries('AAPL', 'DAILY', 0);
            expect(result1).toEqual(mockSeries);
            expect(mockedAxios.get).toHaveBeenCalledTimes(1);

            // Call again, should load from cache
            const result2 = await getIndicatorSeries('AAPL', 'DAILY', 0);
            expect(result2).toEqual(mockSeries);
            expect(mockedAxios.get).toHaveBeenCalledTimes(1);
        });
    });

    describe('getTechnicalAnalysis', () => {
        it('should fetch today snapshot aggregating candles and indicators', async () => {
            const mockCandles = [{ date: '2026-06-01', open: 100, high: 110, low: 90, close: 105, vol: 5000 }];
            const mockDaily = [{ type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI', points: [{ date: '2026-06-01', values: { value: 50 } }] }];
            const mockWeekly = [{ type: 'MACD', source: 'CLOSE', params: 'fast=12', label: 'MACD', points: [{ date: '2026-05-31', values: { value: 1 } }] }];
            
            // Promise.all calls getCandleData, getIndicatorSeries(DAILY), getIndicatorSeries(WEEKLY)
            // But getCandleData and getIndicatorSeries are mocked? No, we are testing the API service which CALLS axios.
            // Let's mock the axios responses in order or by URL.
            mockedAxios.get.mockImplementation((url, config) => {
                if (url.includes('/data')) return Promise.resolve({ data: mockCandles });
                if (url.includes('/indicators') && config?.params?.timeframe === 'daily') return Promise.resolve({ data: mockDaily });
                if (url.includes('/indicators') && config?.params?.timeframe === 'weekly') return Promise.resolve({ data: mockWeekly });
                return Promise.resolve({ data: [] });
            });

            const result = await getTechnicalAnalysis('AAPL');
            expect(result).toEqual({
                symbol: 'AAPL',
                candle: mockCandles[0],
                dailyIndicators: mockDaily,
                weeklyIndicators: mockWeekly,
                srLines: []
            });
            expect(mockedAxios.get).toHaveBeenCalledTimes(4);
        });
    });

    describe('indicatorKey utility', () => {
        it('should generate stable identifier string from configs', () => {
            const config = { type: 'EMA', source: 'CLOSE', params: 'period=50' };
            expect(indicatorKey(config)).toBe('EMA|CLOSE|period=50');
        });
    });
});
