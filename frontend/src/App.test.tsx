import { render, screen, waitFor, fireEvent, act } from '@testing-library/react';
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
        getQuote: vi.fn(),
        getCachedMarketHolidays: vi.fn(),
    };
});

let lastRenderedChartData: DailyCandleData[] = [];

vi.mock('./components/Chart', () => ({
    default: ({ data }: { data: DailyCandleData[] }) => {
        lastRenderedChartData = data;
        return (
            <div data-testid="mock-chart" data-candle-count={data.length}>
                Mock Chart ({data.length} candles)
            </div>
        );
    },
}));

describe('App Intra-Day Tab - Issue #232', () => {
    const yesterdayCandles: DailyCandleData[] = [
        {
            date: '2026-10-15T09:15:00+05:30',
            open: 22671.3,
            high: 22800.0,
            low: 22637.4,
            close: 22750.0,
            vol: 1000,
        },
        {
            date: '2026-10-15T15:15:00+05:30',
            open: 22750.0,
            high: 22850.0,
            low: 22700.0,
            close: 22622.7,
            vol: 1200,
        },
    ];

    beforeEach(() => {
        vi.clearAllMocks();
        lastRenderedChartData = [];
        window.history.replaceState({}, '', '/?ticker=NIFTY50&tab=intraday');

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

    it('defaults to today (FRI, OCT 16, 2026), displays Awaiting Intraday Data, live LTP/Change, and "—" OHLC when today has no candles', async () => {
        // Friday, Oct 16, 2026 at 09:31:15 AM IST (04:01:15 UTC)
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 22958.5,
            timestamp: '2026-10-16T09:31:15+05:30',
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        // Date navigator shows FRI, OCT 16, 2026
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(1);

        // Live LTP & Change vs yesterday's close (22958.50 - 22622.70 = +335.80, +1.48%)
        expect(screen.getByText('22,958.50')).toBeInTheDocument();
        expect(screen.getByText(/\+335\.80 \(\+1\.48%\)/)).toBeInTheDocument();

        // OHLC pills show "—"
        const openPill = screen.getByText('Open:').parentElement;
        const highPill = screen.getByText('High:').parentElement;
        const lowPill = screen.getByText('Low:').parentElement;
        const closePill = screen.getByText('Close:').parentElement;
        expect(openPill).toHaveTextContent('Open:—');
        expect(highPill).toHaveTextContent('High:—');
        expect(lowPill).toHaveTextContent('Low:—');
        expect(closePill).toHaveTextContent('Close:—');
    });

    it('switches to THU, OCT 15, 2026 on clicking Previous day and renders yesterday candles without mutating them with today live quote', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 22958.5,
            timestamp: '2026-10-16T09:31:15+05:30',
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        const prevDayBtn = screen.getByRole('button', { name: /Previous day/i });
        fireEvent.click(prevDayBtn);

        await waitFor(() => {
            expect(screen.getByTestId('mock-chart')).toBeInTheDocument();
        });

        // Date navigator shows THU, OCT 15, 2026
        expect(screen.getByText(/Thu, Oct 15, 2026/i)).toBeInTheDocument();

        // Verify yesterday's last candle was NOT mutated by today's live quote (22958.50)
        expect(lastRenderedChartData).toHaveLength(2);
        expect(lastRenderedChartData[1].close).toBe(22622.7);
        expect(lastRenderedChartData[1].high).toBe(22850.0);

        // Verify summary pills show yesterday's genuine values
        expect(screen.getByText('22,622.70')).toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22671.30');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:22850.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:22637.40');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22622.70');
    });

    it('replaces Awaiting Intraday Data card with the 15m chart when a subsequent 30-second poll returns candles for today', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        let pollCount = 0;
        const todayCandle: DailyCandleData = {
            date: '2026-10-16T09:15:00+05:30',
            open: 22900.0,
            high: 22980.0,
            low: 22890.0,
            close: 22950.0,
            vol: 1500,
        };

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                pollCount += 1;
                if (pollCount === 1) {
                    return yesterdayCandles;
                }
                return [...yesterdayCandles, todayCandle];
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 22960.0,
            timestamp: '2026-10-16T09:31:45+05:30',
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        await act(async () => {
            await vi.advanceTimersByTimeAsync(30_000);
        });

        await waitFor(() => {
            expect(screen.getByTestId('mock-chart')).toBeInTheDocument();
        });
        expect(screen.queryByText('Awaiting Intraday Data')).not.toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22900.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22960.00');
    });

    it('mentions Weekend when today is Saturday/Sunday and allows traversing back through every single calendar day', async () => {
        // Sunday, Oct 18, 2026 at 10:00 AM IST (04:30 UTC), while latest candles are from Thursday Oct 15
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-18T04:30:00Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 22622.7,
            timestamp: '2026-10-15T15:30:00+05:30',
        });

        render(<App />);

        // Starts on Sunday, Oct 18, 2026 -> Market Closed — Weekend (Sunday)
        await waitFor(() => {
            expect(screen.getByText('Market Closed — Weekend')).toBeInTheDocument();
        });
        expect(screen.getByText(/market is closed for the weekend \(Sunday\)/i)).toBeInTheDocument();

        const prevDayBtn = screen.getByRole('button', { name: /Previous day/i });

        // Step back 1 day -> Saturday, Oct 17, 2026 -> Market Closed — Weekend (Saturday)
        fireEvent.click(prevDayBtn);
        await waitFor(() => {
            expect(screen.getByText(/market is closed for the weekend \(Saturday\)/i)).toBeInTheDocument();
        });

        // Step back 1 day -> Friday, Oct 16, 2026 -> Awaiting Intraday Data (trading day with no candles)
        fireEvent.click(prevDayBtn);
        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        // Step back 1 day -> Thursday, Oct 15, 2026 -> Renders chart!
        fireEvent.click(prevDayBtn);
        await waitFor(() => {
            expect(screen.getByTestId('mock-chart')).toBeInTheDocument();
        });
        expect(screen.getByText(/Thu, Oct 15, 2026/i)).toBeInTheDocument();
    });

    it('mentions the specific holiday name when viewing a market holiday with no intraday candles', async () => {
        // Tuesday, Nov 24, 2026 (Prakash Gurpurb Sri Guru Nanak Dev) at 10:30 AM IST (05:00 UTC)
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-11-24T05:00:00Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 22622.7,
            timestamp: '2026-11-23T15:30:00+05:30',
        });

        render(<App />);

        await waitFor(() => {
            expect(
                screen.getByText('Market Closed — Prakash Gurpurb Sri Guru Nanak Dev')
            ).toBeInTheDocument();
        });
        expect(
            screen.getByText(/because the market is closed for/i)
        ).toHaveTextContent('Prakash Gurpurb Sri Guru Nanak Dev');
    });

    it('renders "—" for LTP and Change when awaiting today candles and quote returns 0', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });
        vi.mocked(api.getQuote).mockResolvedValue({
            currentPrice: 0,
            timestamp: '',
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        const ltpContainer = screen.getByText('Last Traded Price').parentElement;
        expect(ltpContainer).toHaveTextContent('Last Traded Price—');

        const changeContainer = screen.getByText(/\(vs last day close\)/i).parentElement?.parentElement;
        expect(changeContainer).toHaveTextContent('—');
    });
});
