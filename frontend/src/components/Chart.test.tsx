import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { createChart } from 'lightweight-charts';
import Chart from './Chart';
import type { DailyCandleData, IndicatorSeries, IndicatorConfig } from '../services/api';

// Mock lightweight-charts
vi.mock('lightweight-charts', () => import('../__mocks__/lightweight-charts'));

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
        }
    ];

    const mockConfigs: IndicatorConfig[] = [
        { timeframe: 'DAILY', type: 'EMA', source: 'CLOSE', params: 'period=5', label: 'EMA(5)' },
        { timeframe: 'DAILY', type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI(14)' }
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
        expect(chartInstance.addSeries).toHaveBeenCalledTimes(3);
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
                }),
                remove: vi.fn(),
                applyOptions: vi.fn(),
                timeScale: vi.fn().mockReturnValue({
                    setVisibleRange: vi.fn(),
                    setVisibleLogicalRange: vi.fn(),
                    subscribeVisibleTimeRangeChange: vi.fn(),
                    subscribeVisibleLogicalRangeChange: (cb: (logicalRange: { from: number; to: number } | null) => void) => {
                        capturedCallback = cb;
                    },
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
});
