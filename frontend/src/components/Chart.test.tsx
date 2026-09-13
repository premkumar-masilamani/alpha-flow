import { render, screen, fireEvent, act } from '@testing-library/react';
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
        vi.clearAllMocks();

        // 3. S&R without firstTouchDate does not attach primitive (no fallback to full horizontal line)
        const mockSrDataNoTouchDate = [
            {
                priceDate: '2026-06-03',
                zoneBottom: 100,
                zoneTop: 105,
                zoneMidpoint: 102.5,
                levelType: 'SUPPORT' as const,
                touchCount: 3,
            }
        ];

        const { unmount: unmountNoTouch } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                supportResistances={mockSrDataNoTouchDate}
                showSupportResistance={true}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstanceNoTouch = vi.mocked(createChart).mock.results[0].value;
        const candleMockNoTouch = chartInstanceNoTouch.addSeries.mock.results[0].value;
        expect(candleMockNoTouch.attachPrimitive).not.toHaveBeenCalled();

        unmountNoTouch();
    });

    it('attaches ChartPatternPrimitive to candlestick series when showChartPatterns is true and patterns exist', () => {
        const mockChartPatterns = [
            {
                id: 1,
                patternType: 'DOUBLE_TOP',
                shortName: 'DT',
                displayName: 'Double Top',
                sentiment: 'BEARISH_REVERSAL' as const,
                status: 'IN_PROGRESS' as const,
                startDate: '2026-06-01',
                endDate: '2026-06-03',
                necklinePrice: 100,
                targetPrice: 90,
                invalidationPrice: 110,
                pivotPoints: [
                    { date: '2026-06-01', price: 105, type: 'HIGH' as const, role: 'PEAK_1' },
                    { date: '2026-06-02', price: 100, type: 'LOW' as const, role: 'NECKLINE' },
                    { date: '2026-06-03', price: 105, type: 'HIGH' as const, role: 'PEAK_2' }
                ]
            }
        ];

        // 1. Chart Patterns enabled
        const { unmount } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                chartPatterns={mockChartPatterns}
                showChartPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstance = vi.mocked(createChart).mock.results[0].value;
        const candlestickSeriesMock = chartInstance.addSeries.mock.results[0].value;
        expect(candlestickSeriesMock.attachPrimitive).toHaveBeenCalledTimes(1);

        unmount();
        vi.clearAllMocks();

        // 2. Chart Patterns disabled
        const { unmount: unmountDisabled } = render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                chartPatterns={mockChartPatterns}
                showChartPatterns={false}
                onLoadOlderData={vi.fn()}
            />
        );

        const chartInstanceDisabled = vi.mocked(createChart).mock.results[0].value;
        const candlestickSeriesMockDisabled = chartInstanceDisabled.addSeries.mock.results[0].value;
        expect(candlestickSeriesMockDisabled.attachPrimitive).not.toHaveBeenCalled();

        unmountDisabled();
    });

    interface MockCrosshairParam {
        time?: string;
        point?: { x: number; y: number };
    }

    it('renders tooltip with chart pattern details including target and stop-loss on hover', () => {
        let crosshairCallback: ((param: MockCrosshairParam) => void) | null = null;
        vi.mocked(createChart).mockImplementationOnce(() => {
            return {
                addSeries: vi.fn().mockReturnValue({
                    setData: vi.fn(),
                    priceScale: vi.fn().mockReturnValue({ applyOptions: vi.fn() }),
                    createPriceLine: vi.fn(),
                    setMarkers: vi.fn(),
                    attachPrimitive: vi.fn(),
                    detachPrimitive: vi.fn(),
                    priceToCoordinate: vi.fn().mockReturnValue(100),
                }),
                remove: vi.fn(),
                applyOptions: vi.fn(),
                subscribeCrosshairMove: (cb: (param: MockCrosshairParam) => void) => {
                    crosshairCallback = cb;
                },
                timeScale: vi.fn().mockReturnValue({
                    setVisibleRange: vi.fn(),
                    setVisibleLogicalRange: vi.fn(),
                    subscribeVisibleTimeRangeChange: vi.fn(),
                    subscribeVisibleLogicalRangeChange: vi.fn(),
                    timeToCoordinate: vi.fn().mockReturnValue(100),
                }),
                panes: vi.fn().mockReturnValue([]),
            } as unknown as ReturnType<typeof createChart>;
        });

        const mockChartPatterns = [
            {
                id: 1,
                patternType: 'DOUBLE_BOTTOM',
                shortName: 'DBM',
                displayName: 'Double Bottom',
                sentiment: 'BULLISH_REVERSAL' as const,
                status: 'COMPLETED' as const,
                startDate: '2026-06-01',
                endDate: '2026-06-03',
                necklinePrice: 100,
                targetPrice: 125.5,
                stopLossPrice: 95.25,
                pivotPoints: [
                    { date: '2026-06-01', price: 95, type: 'LOW' as const, role: 'TROUGH_1' },
                    { date: '2026-06-02', price: 100, type: 'HIGH' as const, role: 'PEAK' },
                    { date: '2026-06-03', price: 95, type: 'LOW' as const, role: 'TROUGH_2' },
                ],
            },
        ];

        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                chartPatterns={mockChartPatterns}
                showChartPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        expect(crosshairCallback).not.toBeNull();

        // 1. Move crosshair over another candle in the pattern - tooltip should NOT appear
        act(() => {
            crosshairCallback!({
                time: '2026-06-02',
                point: { x: 50, y: 100 },
            });
        });
        expect(screen.queryByText('Double Bottom')).not.toBeInTheDocument();

        // 2. Move crosshair over the short form label badge (x: 120, y: 90) - tooltip should appear
        act(() => {
            crosshairCallback!({
                time: '2026-06-03',
                point: { x: 120, y: 90 },
            });
        });

        // Verify tooltip rendered with long name, short name, status, target, and stop loss
        expect(screen.getByText('Double Bottom')).toBeInTheDocument();
        expect(screen.getByText('DBM')).toBeInTheDocument();
        expect(screen.getByText('COMPLETED')).toBeInTheDocument();
        expect(screen.getByText('$125.50')).toBeInTheDocument();
        expect(screen.getByText('$95.25')).toBeInTheDocument();
    });

    it('renders tooltip with in-progress chart pattern details without target and stop loss', () => {
        let crosshairCallback: ((param: MockCrosshairParam) => void) | null = null;
        vi.mocked(createChart).mockImplementationOnce(() => {
            return {
                addSeries: vi.fn().mockReturnValue({
                    setData: vi.fn(),
                    priceScale: vi.fn().mockReturnValue({ applyOptions: vi.fn() }),
                    createPriceLine: vi.fn(),
                    setMarkers: vi.fn(),
                    attachPrimitive: vi.fn(),
                    detachPrimitive: vi.fn(),
                    priceToCoordinate: vi.fn().mockReturnValue(100),
                }),
                remove: vi.fn(),
                applyOptions: vi.fn(),
                subscribeCrosshairMove: (cb: (param: MockCrosshairParam) => void) => {
                    crosshairCallback = cb;
                },
                timeScale: vi.fn().mockReturnValue({
                    setVisibleRange: vi.fn(),
                    setVisibleLogicalRange: vi.fn(),
                    subscribeVisibleTimeRangeChange: vi.fn(),
                    subscribeVisibleLogicalRangeChange: vi.fn(),
                    timeToCoordinate: vi.fn().mockReturnValue(100),
                }),
                panes: vi.fn().mockReturnValue([]),
            } as unknown as ReturnType<typeof createChart>;
        });

        const mockChartPatterns = [
            {
                id: 2,
                patternType: 'HEAD_AND_SHOULDERS',
                shortName: 'HNS',
                displayName: 'Head and Shoulders',
                sentiment: 'BEARISH_REVERSAL' as const,
                status: 'IN_PROGRESS' as const,
                startDate: '2026-06-01',
                endDate: '2026-06-03',
                necklinePrice: 100,
                targetPrice: 85,
                stopLossPrice: 108,
                pivotPoints: [
                    { date: '2026-06-01', price: 105, type: 'HIGH' as const, role: 'LEFT_SHOULDER' },
                    { date: '2026-06-02', price: 115, type: 'HIGH' as const, role: 'HEAD' },
                    { date: '2026-06-03', price: 106, type: 'HIGH' as const, role: 'RIGHT_SHOULDER' },
                ],
            },
        ];

        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                chartPatterns={mockChartPatterns}
                showChartPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        expect(crosshairCallback).not.toBeNull();

        // 1. Move crosshair over earlier candle - tooltip should NOT appear
        act(() => {
            crosshairCallback!({
                time: '2026-06-02',
                point: { x: 50, y: 100 },
            });
        });
        expect(screen.queryByText('Head and Shoulders')).not.toBeInTheDocument();

        // 2. Move crosshair over short form label badge (x: 120, y: 90)
        act(() => {
            crosshairCallback!({
                time: '2026-06-03',
                point: { x: 120, y: 90 },
            });
        });

        expect(screen.getByText('Head and Shoulders')).toBeInTheDocument();
        expect(screen.getByText('HNS')).toBeInTheDocument();
        expect(screen.getByText('IN PROGRESS')).toBeInTheDocument();
        expect(screen.queryByText('$85.00')).not.toBeInTheDocument();
        expect(screen.queryByText('$108.00')).not.toBeInTheDocument();
    });

    it('renders combined tooltip when candlestick pattern and chart pattern coincide', () => {
        let crosshairCallback: ((param: MockCrosshairParam) => void) | null = null;
        vi.mocked(createChart).mockImplementationOnce(() => {
            return {
                addSeries: vi.fn().mockReturnValue({
                    setData: vi.fn(),
                    priceScale: vi.fn().mockReturnValue({ applyOptions: vi.fn() }),
                    createPriceLine: vi.fn(),
                    setMarkers: vi.fn(),
                    attachPrimitive: vi.fn(),
                    detachPrimitive: vi.fn(),
                    priceToCoordinate: vi.fn().mockReturnValue(100),
                }),
                remove: vi.fn(),
                applyOptions: vi.fn(),
                subscribeCrosshairMove: (cb: (param: MockCrosshairParam) => void) => {
                    crosshairCallback = cb;
                },
                timeScale: vi.fn().mockReturnValue({
                    setVisibleRange: vi.fn(),
                    setVisibleLogicalRange: vi.fn(),
                    subscribeVisibleTimeRangeChange: vi.fn(),
                    subscribeVisibleLogicalRangeChange: vi.fn(),
                    timeToCoordinate: vi.fn().mockReturnValue(100),
                }),
                panes: vi.fn().mockReturnValue([]),
            } as unknown as ReturnType<typeof createChart>;
        });

        const mockCandlesticks = [
            { date: '2026-06-02', shortName: 'HAM', longName: 'Hammer', sentiment: SENTIMENT_TYPES.BULLISH_REVERSAL },
        ];

        const mockChartPatterns = [
            {
                id: 1,
                patternType: 'DOUBLE_BOTTOM',
                shortName: 'DBM',
                displayName: 'Double Bottom',
                sentiment: 'BULLISH_REVERSAL' as const,
                status: 'COMPLETED' as const,
                startDate: '2026-06-01',
                endDate: '2026-06-03',
                necklinePrice: 100,
                targetPrice: 130,
                stopLossPrice: 90,
                pivotPoints: [
                    { date: '2026-06-01', price: 95, type: 'LOW' as const, role: 'TROUGH_1' },
                    { date: '2026-06-02', price: 100, type: 'HIGH' as const, role: 'PEAK' },
                    { date: '2026-06-03', price: 95, type: 'LOW' as const, role: 'TROUGH_2' },
                ],
            },
        ];

        render(
            <Chart
                data={mockData}
                indicators={mockIndicators}
                enabled={new Set()}
                configs={mockConfigs}
                symbol="AAPL"
                timeframe="DAILY"
                candlestickPatterns={mockCandlesticks}
                showCandlestickPatterns={true}
                chartPatterns={mockChartPatterns}
                showChartPatterns={true}
                onLoadOlderData={vi.fn()}
            />
        );

        // 1. Hover on candlestick pattern date (x: 50, y: 100) -> Candlestick tooltip appears, chart pattern does NOT
        act(() => {
            crosshairCallback!({
                time: '2026-06-02',
                point: { x: 50, y: 100 },
            });
        });
        expect(screen.getByText('Hammer')).toBeInTheDocument();
        expect(screen.getByText('HAM')).toBeInTheDocument();
        expect(screen.queryByText('Double Bottom')).not.toBeInTheDocument();

        // 2. Hover on chart pattern short form label (x: 120, y: 90) -> Chart pattern tooltip appears
        act(() => {
            crosshairCallback!({
                time: '2026-06-03',
                point: { x: 120, y: 90 },
            });
        });
        expect(screen.getByText('Double Bottom')).toBeInTheDocument();
        expect(screen.getByText('DBM')).toBeInTheDocument();
        expect(screen.getByText('COMPLETED')).toBeInTheDocument();
        expect(screen.getByText('$130.00')).toBeInTheDocument();
        expect(screen.getByText('$90.00')).toBeInTheDocument();

        // 3. Move crosshair away
        act(() => {
            crosshairCallback!({
                time: '2026-06-10',
                point: { x: 300, y: 300 },
            });
        });
        expect(screen.queryByText('Double Bottom')).not.toBeInTheDocument();
        expect(screen.queryByText('Hammer')).not.toBeInTheDocument();
    });
});
