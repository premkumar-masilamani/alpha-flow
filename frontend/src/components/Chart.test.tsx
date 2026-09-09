import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createChart, createSeriesMarkers } from 'lightweight-charts';
import Chart from './Chart';
import type { DailyCandleData, IndicatorSeries, IndicatorConfig } from '../services/api';
import { SENTIMENT_TYPES } from '../services/api';

// Mock lightweight-charts
vi.mock('lightweight-charts', () => {
    return {
        createChart: vi.fn().mockReturnValue({
            addSeries: vi.fn().mockReturnValue({
                setData: vi.fn(),
                priceScale: vi.fn().mockReturnValue({
                    applyOptions: vi.fn(),
                }),
                createPriceLine: vi.fn(),
                setMarkers: vi.fn(),
                attachPrimitive: vi.fn(),
                detachPrimitive: vi.fn(),
                priceToCoordinate: vi.fn().mockReturnValue(100),
            }),
            remove: vi.fn(),
            applyOptions: vi.fn(),
            subscribeCrosshairMove: vi.fn(),
            timeScale: vi.fn().mockReturnValue({
                setVisibleRange: vi.fn(),
                setVisibleLogicalRange: vi.fn(),
                subscribeVisibleTimeRangeChange: vi.fn(),
                subscribeVisibleLogicalRangeChange: vi.fn(),
                timeToCoordinate: vi.fn().mockReturnValue(100),
            }),
            panes: vi.fn().mockReturnValue([]),
        }),
        ColorType: {
            Solid: 'solid',
            VerticalGradient: 'vertical_gradient',
        },
        LineStyle: {
            Solid: 0,
            Dotted: 1,
            Dashed: 2,
        },
        CandlestickSeries: 'CandlestickSeries',
        HistogramSeries: 'HistogramSeries',
        LineSeries: 'LineSeries',
        createSeriesMarkers: vi.fn().mockReturnValue({
            setMarkers: vi.fn(),
        }),
    };
});

describe('Chart Component', () => {
    const mockData: DailyCandleData[] = [
        { date: '2026-06-01', open: 100, high: 110, low: 90, close: 105, vol: 5000 },
        { date: '2026-06-02', open: 105, high: 115, low: 100, close: 112, vol: 6000 },
        { date: '2026-06-03', open: 112, high: 120, low: 110, close: 115, vol: 7000 }
    ];

    const mockIndicators: IndicatorSeries[] = [
        {
            type: 'EMA',
            source: 'CLOSE',
            params: 'period=5',
            label: 'EMA(5)',
            points: [
                { date: '2026-06-01', values: { value: 100 } },
                { date: '2026-06-02', values: { value: 102 } },
                { date: '2026-06-03', values: { value: 105 } }
            ]
        },
        {
            type: 'RSI',
            source: 'CLOSE',
            params: 'period=14',
            label: 'RSI(14)',
            points: [
                { date: '2026-06-01', values: { value: 50 } },
                { date: '2026-06-02', values: { value: 55 } },
                { date: '2026-06-03', values: { value: 60 } }
            ]
        },
        {
            type: 'SMA',
            source: 'VOLUME',
            params: 'period=20',
            label: 'Vol (20)',
            points: [
                { date: '2026-06-01', values: { value: 1000 } },
                { date: '2026-06-02', values: { value: 1000 } },
                { date: '2026-06-03', values: { value: 1000 } }
            ]
        }
    ];

    const mockConfigs: IndicatorConfig[] = [
        { timeframe: 'DAILY', type: 'EMA', source: 'CLOSE', params: 'period=5', label: 'EMA(5)' },
        { timeframe: 'DAILY', type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI(14)' },
        { timeframe: 'DAILY', type: 'SMA', source: 'VOLUME', params: 'period=20', label: 'Vol (20)' }
    ];

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders the chart element and Reset Zoom button', () => {
        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set(['EMA|CLOSE|period=5'])}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                onLoadOlderData={vi.fn()}
            />
        );

        expect(screen.getByText('Reset Zoom')).toBeInTheDocument();
        expect(screen.getByText('EMA (5) - 105.00')).toBeInTheDocument();
    });

    it('calls createChart and adds candlestick, volume and indicator series', () => {
        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set(['EMA|CLOSE|period=5'])}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                onLoadOlderData={vi.fn()}
            />
        );

        expect(createChart).toHaveBeenCalled();
        const chartInstance = vi.mocked(createChart).mock.results[0].value;
        // 4 series: Candlestick, Volume, enabled EMA(5), and always-displayed Vol (20)
        expect(chartInstance.addSeries).toHaveBeenCalledTimes(4);
    });

    it('triggers setVisibleLogicalRange when clicking Reset Zoom', async () => {
        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set(['EMA|CLOSE|period=5'])}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstance = vi.mocked(createChart).mock.results[0].value;
        const setVisibleLogicalRangeMock = chartInstance.timeScale().setVisibleLogicalRange;

        const resetButton = screen.getByText('Reset Zoom');
        fireEvent.click(resetButton);

        await new Promise((resolve) => requestAnimationFrame(resolve));

        expect(setVisibleLogicalRangeMock).toHaveBeenCalled();
    });

    it('triggers onLoadOlderData when logical range change reaches early index', () => {
        const onLoadOlderDataMock = vi.fn();
        let capturedCallback: ((logicalRange: { from: number; to: number } | null) => void) | null = null;

        // Custom implementation to capture the logical range callback
        vi.mocked(createChart).mockImplementationOnce(() => {
            return {
                addSeries: vi.fn().mockReturnValue({
                    setData: vi.fn(),
                    priceScale: vi.fn().mockReturnValue({
                        applyOptions: vi.fn(),
                    }),
                    createPriceLine: vi.fn(),
                    setMarkers: vi.fn(),
                    attachPrimitive: vi.fn(),
                    detachPrimitive: vi.fn(),
                    priceToCoordinate: vi.fn().mockReturnValue(100),
                }),
                remove: vi.fn(),
                applyOptions: vi.fn(),
                subscribeCrosshairMove: vi.fn(),
                timeScale: vi.fn().mockReturnValue({
                    setVisibleRange: vi.fn(),
                    setVisibleLogicalRange: vi.fn(),
                    subscribeVisibleTimeRangeChange: vi.fn(),
                    subscribeVisibleLogicalRangeChange: (cb: (logicalRange: { from: number; to: number } | null) => void) => {
                        capturedCallback = cb;
                    },
                    timeToCoordinate: vi.fn().mockReturnValue(100),
                }),
                panes: vi.fn().mockReturnValue([]),
            } as unknown as ReturnType<typeof createChart>;
        });

        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set(['EMA|CLOSE|period=5'])}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                onLoadOlderData={onLoadOlderDataMock}
            />
        );

        expect(capturedCallback).not.toBeNull();

        // Call callback with logical range from > 2 -> should not trigger onLoadOlderData
        capturedCallback!({ from: 5, to: 20 });
        expect(onLoadOlderDataMock).not.toHaveBeenCalled();

        // Call callback with logical range from <= 2 -> should trigger onLoadOlderData
        capturedCallback!({ from: 1, to: 20 });
        expect(onLoadOlderDataMock).toHaveBeenCalledTimes(1);
    });

    it('sets markers on candlestick series depending on showCandlestickPatterns', () => {
        const mockPatterns = [
            { date: '2026-06-01', shortName: 'HAM', longName: 'Hammer', sentiment: SENTIMENT_TYPES.BULLISH_REVERSAL },
            { date: '2026-06-03', shortName: 'ENG', longName: 'Engulfing', sentiment: SENTIMENT_TYPES.BEARISH_REVERSAL }
        ];

        // 1. Patterns enabled
        const { unmount } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                candlestickPatterns={mockPatterns}
                showCandlestickPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstance = vi.mocked(createChart).mock.results[0].value;
        const candlestickSeriesMock = chartInstance.addSeries.mock.results[0].value;
        expect(createSeriesMarkers).toHaveBeenCalledTimes(1);
        expect(createSeriesMarkers).toHaveBeenCalledWith(candlestickSeriesMock, [
            { time: '2026-06-01', position: 'belowBar', color: '#22c55e', shape: 'arrowUp', text: 'HAM' },
            { time: '2026-06-03', position: 'aboveBar', color: '#ef4444', shape: 'arrowDown', text: 'ENG' }
        ]);

        unmount();
        vi.clearAllMocks();

        // 2. Patterns disabled (showCandlestickPatterns = false)
        const { unmount: unmountDisabled } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                candlestickPatterns={mockPatterns}
                showCandlestickPatterns={false}
                onLoadOlderData={vi.fn()}
            />
        );

        expect(createSeriesMarkers).not.toHaveBeenCalled();

        unmountDisabled();
        vi.clearAllMocks();

        // 3. Deduplicate duplicate dates (only show the first pattern on a given date)
        const duplicateMockPatterns = [
            { date: '2026-06-03', shortName: 'HAM', longName: 'Hammer', sentiment: SENTIMENT_TYPES.BULLISH_REVERSAL },
            { date: '2026-06-03', shortName: 'ENG', longName: 'Engulfing', sentiment: SENTIMENT_TYPES.BEARISH_REVERSAL }
        ];

        const { unmount: unmountDup } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                candlestickPatterns={duplicateMockPatterns}
                showCandlestickPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        const dupChartInstance = vi.mocked(createChart).mock.results[0].value;
        const dupCandleMock = dupChartInstance.addSeries.mock.results[0].value;
        expect(createSeriesMarkers).toHaveBeenCalledTimes(1);
        // Only the first pattern HAM (on '2026-06-03') should be displayed.
        expect(createSeriesMarkers).toHaveBeenCalledWith(dupCandleMock, [
            { time: '2026-06-03', position: 'belowBar', color: '#22c55e', shape: 'arrowUp', text: 'HAM' }
        ]);

        unmountDup();
    });

    it('attaches SupportResistancePrimitive to candlestick series when showSupportResistance is true', () => {
        const mockSrData = [
            {
                priceDate: '2026-06-03',
                firstTouchDate: '2026-06-01',
                lastTouchDate: '2026-06-02',
                zoneBottom: 100,
                zoneTop: 105,
                zoneMidpoint: 102.5,
                levelType: 'SUPPORT' as const,
                touchCount: 3,
            }
        ];

        // 1. S&R enabled
        const { unmount } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                supportResistances={mockSrData}
                showSupportResistance={true}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstance = vi.mocked(createChart).mock.results[0].value;
        const candlestickSeriesMock = chartInstance.addSeries.mock.results[0].value;
        expect(candlestickSeriesMock.attachPrimitive).toHaveBeenCalledTimes(1);

        unmount();
        vi.clearAllMocks();

        // 2. S&R disabled
        const { unmount: unmountDisabled } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                supportResistances={mockSrData}
                showSupportResistance={false}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstanceDisabled = vi.mocked(createChart).mock.results[0].value;
        const candlestickSeriesMockDisabled = chartInstanceDisabled.addSeries.mock.results[0].value;
        expect(candlestickSeriesMockDisabled.attachPrimitive).not.toHaveBeenCalled();

        unmountDisabled();
    });
});
