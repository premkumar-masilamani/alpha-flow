import { render, screen, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import App from './App';
import * as api from './services/api';
import type { DailyCandleData } from './services/api';

vi.mock('./services/api', async (importOriginal) => {
    const actual = await importOriginal<typeof import('./services/api')>();
    return {
        ...actual,
        getTickers: vi.fn(),
        getIndicatorConfigs: vi.fn(),
        getIndicatorSeries: vi.fn(),
        getTechnicalAnalysis: vi.fn(),
        getCandlestickPatterns: vi.fn(),
        getChartPatterns: vi.fn(),
        getSupportResistances: vi.fn(),
        getCandleData: vi.fn(),
        getCachedMarketHolidays: vi.fn(),
    };
});

vi.mock('./components/Chart', () => ({
    default: ({ data }: { data: DailyCandleData[] }) => {
        return (
            <div data-testid="mock-chart" data-candle-count={data.length}>
                Mock Chart ({data.length} candles)
            </div>
        );
    },
}));

describe('App Component', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=overview');

        vi.mocked(api.getTickers).mockResolvedValue([
            {
                id: 1,
                symbol: 'NIFTY50',
                name: 'NIFTY 50',
                type: 'INDEX',
                country: 'IN',
            },
        ]);
        vi.mocked(api.getIndicatorConfigs).mockResolvedValue([]);
        vi.mocked(api.getIndicatorSeries).mockResolvedValue([]);
        vi.mocked(api.getTechnicalAnalysis).mockResolvedValue({
            symbol: 'NIFTY50',
            candle: null,
            dailyIndicators: [],
            weeklyIndicators: [],
        });
        vi.mocked(api.getCandlestickPatterns).mockResolvedValue([]);
        vi.mocked(api.getChartPatterns).mockResolvedValue([]);
        vi.mocked(api.getSupportResistances).mockResolvedValue([]);
        vi.mocked(api.getCachedMarketHolidays).mockResolvedValue([
            {
                id: 1,
                country: 'IN',
                holidayDate: '2026-11-24',
                name: 'Prakash Gurpurb Sri Guru Nanak Dev',
            },
        ]);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('renders only Technical Analysis and Technical Chart tabs in navigation header, and does not render Intra-Day tab', async () => {
        render(<App />);

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /Technical Analysis/i })).toBeInTheDocument();
        });
        expect(screen.getByRole('button', { name: /Technical Chart/i })).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /Intra-Day/i })).not.toBeInTheDocument();
    });

    it('renders PriceSummaryBanner in Technical Analysis tab with static date, separate Support & Resistance box, and no timeframe badge', async () => {
        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=overview');
        vi.mocked(api.getTechnicalAnalysis).mockResolvedValue({
            symbol: 'NIFTY50',
            candle: {
                date: '2026-10-16',
                open: 22600.0,
                high: 22750.0,
                low: 22550.0,
                close: 22700.0,
                vol: 5000000,
            },
            dailyIndicators: [],
            weeklyIndicators: [],
        });
        vi.mocked(api.getSupportResistances).mockResolvedValue([
            {
                priceDate: '2026-10-16',
                levelType: 'SUPPORT',
                zoneBottom: 22500,
                zoneTop: 22550,
                zoneMidpoint: 22525,
                touchCount: 3,
                firstTouchDate: '2026-09-01',
                lastTouchDate: '2026-10-10',
            },
        ]);
        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === 'DAILY') {
                return [
                    {
                        date: '2026-10-16',
                        open: 22600.0,
                        high: 22750.0,
                        low: 22550.0,
                        close: 22700.0,
                        vol: 5000000,
                    },
                ];
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('22,700.00')).toBeInTheDocument();
        });

        // PriceSummaryBanner values
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22600.00');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:22750.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:22550.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22700.00');
        expect(screen.getByText('Vol:').parentElement).toHaveTextContent('Vol:5,000,000');

        // Static date badge, no navigation buttons
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(1);
        expect(screen.queryByRole('button', { name: /previous day/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /next day/i })).not.toBeInTheDocument();
        expect(screen.queryByText('15m')).not.toBeInTheDocument();

        // Support & Resistance in its own separate box
        await waitFor(() => {
            expect(screen.getByText('Support & Resistance Zones')).toBeInTheDocument();
            expect(screen.getByText('22500.00 - 22550.00')).toBeInTheDocument();
        });
    });

    it('renders PriceSummaryBanner in Technical Chart tab with static date and no timeframe badge', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));
        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=charts');
        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === 'DAILY') {
                return [
                    {
                        date: '2026-10-15',
                        open: 22500.0,
                        high: 22650.0,
                        low: 22450.0,
                        close: 22600.0,
                        vol: 4000000,
                    },
                    {
                        date: '2026-10-16',
                        open: 22600.0,
                        high: 22750.0,
                        low: 22550.0,
                        close: 22700.0,
                        vol: 5000000,
                    },
                ];
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('22,700.00')).toBeInTheDocument();
        });

        // PriceSummaryBanner values
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22600.00');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:22750.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:22550.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22700.00');
        expect(screen.getByText('Vol:').parentElement).toHaveTextContent('Vol:5,000,000');

        // Static date badge, no navigation buttons
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(1);
        expect(screen.queryByRole('button', { name: /previous day/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /next day/i })).not.toBeInTheDocument();
        expect(screen.queryByText('15m')).not.toBeInTheDocument();
    });

    it('renders PriceSummaryBanner with dashes and styled amber card on Technical Analysis tab when positional data is unavailable', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=overview');

        vi.mocked(api.getTickers).mockResolvedValue([
            {
                id: 1,
                symbol: 'NIFTY50',
                name: 'NIFTY 50',
                type: 'INDEX',
                country: 'IN',
            },
        ]);
        vi.mocked(api.getTechnicalAnalysis).mockResolvedValue({
            symbol: 'NIFTY50',
            candle: null,
            dailyIndicators: [],
            weeklyIndicators: [],
        });
        vi.mocked(api.getCandleData).mockResolvedValue([]);

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Positional Data Unavailable')).toBeInTheDocument();
        });

        // PriceSummaryBanner renders dashes
        expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(5);

        // Amber card displays the message with symbol and ticker name
        const message = screen.getByText(/Daily and weekly price data is not available for/i);
        expect(message).toHaveTextContent('Daily and weekly price data is not available for NIFTY50 (NIFTY 50).');
        expect(screen.getAllByText('NIFTY50').length).toBeGreaterThanOrEqual(3);
    });

    it('renders PriceSummaryBanner with dashes and styled amber card on Technical Chart tab when daily/weekly data is unavailable', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=charts');

        vi.mocked(api.getTickers).mockResolvedValue([
            {
                id: 1,
                symbol: 'NIFTY50',
                name: 'NIFTY 50',
                type: 'INDEX',
                country: 'IN',
            },
        ]);
        vi.mocked(api.getCandleData).mockResolvedValue([]);

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Daily & Weekly Price Data Unavailable')).toBeInTheDocument();
        });

        // PriceSummaryBanner renders dashes
        expect(screen.getAllByText('—').length).toBeGreaterThanOrEqual(5);

        // Indicator controls bar should NOT be rendered when there are no chart candles
        expect(screen.queryByText('Patterns')).not.toBeInTheDocument();
        expect(screen.queryByText('S/R')).not.toBeInTheDocument();

        // Amber card displays the message with symbol and ticker name
        const message = screen.getByText(/Daily and weekly price data is not available for/i);
        expect(message).toHaveTextContent('Daily and weekly price data is not available for NIFTY50 (NIFTY 50).');
        expect(screen.getAllByText('NIFTY50').length).toBeGreaterThanOrEqual(3);
    });
});
