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

    it('defaults to today (FRI, OCT 16, 2026), displays Awaiting Intraday Data, and renders "—" for LTP, Change, and OHLC when today has no candles', async () => {
        // Friday, Oct 16, 2026 at 09:31:15 AM IST (04:01:15 UTC)
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        // Date navigator shows FRI, OCT 16, 2026
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(1);

        // LTP & Change show "—" when today has no candles
        const ltpContainer = screen.getByText('Last Traded Price').parentElement;
        expect(ltpContainer).toHaveTextContent('Last Traded Price—');

        const changeContainer = screen.getByText(/\(vs last day close\)/i).parentElement?.parentElement;
        expect(changeContainer).toHaveTextContent('—');

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

    it('switches to THU, OCT 15, 2026 on clicking Previous day and renders yesterday candles', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                return yesterdayCandles;
            }
            return [];
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

    it('replaces Awaiting Intraday Data card with the 15m chart when a subsequent 1-minute poll returns candles for today while market is open', async () => {
        // Friday, Oct 16, 2026 at 09:31:15 AM IST (04:01:15 UTC) — market is OPEN
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

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        await act(async () => {
            await vi.advanceTimersByTimeAsync(60_000);
        });

        await waitFor(() => {
            expect(screen.getByTestId('mock-chart')).toBeInTheDocument();
        });
        expect(screen.queryByText('Awaiting Intraday Data')).not.toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22900.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22950.00');
    });

    it('pauses the 1-minute intraday poll when the market is closed', async () => {
        // Sunday, Oct 18, 2026 at 10:00 AM IST (04:30 UTC) — market is CLOSED (Weekend)
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-18T04:30:00Z'));

        let intradayFetchCount = 0;
        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf) => {
            if (tf === '15M') {
                intradayFetchCount += 1;
                return yesterdayCandles;
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Market Closed — Weekend')).toBeInTheDocument();
        });
        expect(intradayFetchCount).toBe(1);

        // Advance 3 minutes while market is closed -> no additional intraday polls should fire
        await act(async () => {
            await vi.advanceTimersByTimeAsync(180_000);
        });
        expect(intradayFetchCount).toBe(1);
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

    it('preserves older paginated intraday days across subsequent 1-minute polls and queries endDate before oldestDay', async () => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
        vi.setSystemTime(new Date('2026-10-16T04:01:15Z'));

        const olderCandles: DailyCandleData[] = [
            {
                date: '2026-10-14T09:15:00+05:30',
                open: 22500.0,
                high: 22600.0,
                low: 22480.0,
                close: 22580.0,
                vol: 900,
            },
        ];

        vi.mocked(api.getCandleData).mockImplementation(async (_symbol, tf, _page, _size, _forceFresh, endDate) => {
            if (tf === '15M') {
                if (endDate === '2026-10-14') {
                    return olderCandles;
                }
                return yesterdayCandles;
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Awaiting Intraday Data')).toBeInTheDocument();
        });

        const prevDayBtn = screen.getByRole('button', { name: /Previous day/i });
        // Click 1 -> steps to Oct 15 (and prefetches older candles with endDate = '2026-10-14')
        fireEvent.click(prevDayBtn);

        await waitFor(() => {
            expect(api.getCandleData).toHaveBeenCalledWith('NIFTY50', '15M', 0, 250, false, '2026-10-14');
        });

        // Click 2 -> steps to Oct 14
        fireEvent.click(prevDayBtn);
        await waitFor(() => {
            expect(screen.getByText(/Wed, Oct 14, 2026/i)).toBeInTheDocument();
        });

        // Advance 1-minute poll and verify Oct 14 remains active (not overwritten by latest 250)
        await act(async () => {
            await vi.advanceTimersByTimeAsync(60_000);
        });

        expect(screen.getByText(/Wed, Oct 14, 2026/i)).toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22500.00');
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
                id: 1,
                symbol: 'NIFTY50',
                timeframe: 'DAILY',
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
            expect(screen.getByText('Last Traded Price')).toBeInTheDocument();
        });

        // PriceSummaryBanner values
        expect(screen.getByText('22,700.00')).toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22600.00');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:22750.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:22550.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22700.00');
        expect(screen.getByText('Vol:').parentElement).toHaveTextContent('Vol:5,000,000');

        // Static date badge, no navigation buttons
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(2);
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
                        high: 22800.0,
                        low: 22580.0,
                        close: 22750.0,
                        vol: 4500000,
                    },
                ];
            }
            return [];
        });

        render(<App />);

        await waitFor(() => {
            expect(screen.getByText('Last Traded Price')).toBeInTheDocument();
        });

        // PriceSummaryBanner shows latest candle (close 22,750.00, change vs prev close +150.00)
        expect(screen.getByText('22,750.00')).toBeInTheDocument();
        expect(screen.getByText('(vs prev close)')).toBeInTheDocument();
        expect(screen.getByText('Open:').parentElement).toHaveTextContent('Open:22600.00');
        expect(screen.getByText('High:').parentElement).toHaveTextContent('High:22800.00');
        expect(screen.getByText('Low:').parentElement).toHaveTextContent('Low:22580.00');
        expect(screen.getByText('Close:').parentElement).toHaveTextContent('Close:22750.00');
        expect(screen.getByText('Vol:').parentElement).toHaveTextContent('Vol:4,500,000');

        // Static date badge, no navigation buttons
        expect(screen.getAllByText(/Fri, Oct 16, 2026/i).length).toBeGreaterThanOrEqual(2);
        expect(screen.queryByRole('button', { name: /previous day/i })).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /next day/i })).not.toBeInTheDocument();
        expect(screen.queryByText('15m')).not.toBeInTheDocument();
    });
});

