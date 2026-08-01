import { vi } from 'vitest';

export const createChart = vi.fn().mockReturnValue({
    addSeries: vi.fn().mockReturnValue({
        setData: vi.fn(),
        priceScale: vi.fn().mockReturnValue({
            applyOptions: vi.fn(),
        }),
        createPriceLine: vi.fn(),
        setMarkers: vi.fn(),
    }),
    remove: vi.fn(),
    applyOptions: vi.fn(),
    timeScale: vi.fn().mockReturnValue({
        setVisibleRange: vi.fn(),
        setVisibleLogicalRange: vi.fn(),
        subscribeVisibleTimeRangeChange: vi.fn(),
        subscribeVisibleLogicalRangeChange: vi.fn(),
    }),
    panes: vi.fn().mockReturnValue([]),
});

export const ColorType = {
    Solid: 'solid',
    VerticalGradient: 'vertical_gradient',
};

export const LineStyle = {
    Solid: 0,
    Dotted: 1,
    Dashed: 2,
};

export const CandlestickSeries = 'CandlestickSeries';
export const HistogramSeries = 'HistogramSeries';
export const LineSeries = 'LineSeries';
