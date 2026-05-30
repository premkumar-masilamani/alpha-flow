import React, {useEffect, useRef, useState} from 'react';
import type {ISeriesApi, Time} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, HistogramSeries, LineSeries, LineStyle} from 'lightweight-charts';
import {type DailyCandleData, type IndicatorSeries, indicatorKey, type IndicatorConfig} from '../services/api';
import {RefreshCw} from 'lucide-react';

interface ChartProps {
    data: DailyCandleData[];
    indicators: IndicatorSeries[];
    enabled: Set<string>;
    configs: IndicatorConfig[];
    symbol: string;
    onLoadOlderData: () => void;
}

interface LegendEntry {
    label: string;
    color: string;
}

// Deterministic palette; assigned in enabled-order so a given chart is stable across renders.
const PALETTE = ['#f59e0b', '#06b6d4', '#a855f7', '#ec4899', '#84cc16', '#f43f5e', '#22d3ee', '#fb923c', '#eab308'];

type Placement = 'priceOverlay' | 'volumeOverlay' | 'oscillator';

const placementFor = (series: IndicatorSeries): Placement => {
    if (series.type === 'RSI' || series.type === 'STOCHASTIC' || series.type === 'MACD') return 'oscillator';
    if (series.type === 'SMA' && series.source === 'VOLUME') return 'volumeOverlay';
    return 'priceOverlay'; // EMA, and SMA on a price field
};

// Which output names a series plots, and how each renders.
const outputsFor = (type: string): {name: string; style: 'line' | 'histogram'; suffix: string}[] => {
    switch (type) {
        case 'STOCHASTIC':
            return [{name: 'k', style: 'line', suffix: ' %K'}, {name: 'd', style: 'line', suffix: ' %D'}];
        case 'MACD':
            return [
                {name: 'macd', style: 'line', suffix: ' MACD'},
                {name: 'signal', style: 'line', suffix: ' Signal'},
                {name: 'histogram', style: 'histogram', suffix: ' Hist'},
            ];
        default:
            return [{name: 'value', style: 'line', suffix: ''}];
    }
};

const lineData = (series: IndicatorSeries, output: string) =>
    series.points
        .filter((p) => p.values[output] !== undefined && p.values[output] !== null)
        .map((p) => ({time: p.date as Time, value: Number(p.values[output])}))
        .sort((a, b) => String(a.time).localeCompare(String(b.time)));

const formatLabel = (label: string): string =>
    label.replace(/([A-Za-z]+)\(([^)]+)\)/, "$1 ($2)");

const getLatestValuesString = (series: IndicatorSeries): string => {
    switch (series.type) {
        case 'STOCHASTIC': {
            const kPoints = lineData(series, 'k');
            const dPoints = lineData(series, 'd');
            const kVal = kPoints[kPoints.length - 1]?.value;
            const dVal = dPoints[dPoints.length - 1]?.value;
            return `K: ${kVal !== undefined ? kVal.toFixed(2) : 'N/A'}, D: ${dVal !== undefined ? dVal.toFixed(2) : 'N/A'}`;
        }
        case 'MACD': {
            const macdPoints = lineData(series, 'macd');
            const sigPoints = lineData(series, 'signal');
            const histPoints = lineData(series, 'histogram');
            const mVal = macdPoints[macdPoints.length - 1]?.value;
            const sVal = sigPoints[sigPoints.length - 1]?.value;
            const hVal = histPoints[histPoints.length - 1]?.value;
            return `MACD: ${mVal !== undefined ? mVal.toFixed(2) : 'N/A'}, Signal: ${sVal !== undefined ? sVal.toFixed(2) : 'N/A'}, Hist: ${hVal !== undefined ? hVal.toFixed(2) : 'N/A'}`;
        }
        default: {
            const valPoints = lineData(series, 'value');
            const val = valPoints[valPoints.length - 1]?.value;
            return val !== undefined ? val.toFixed(2) : 'N/A';
        }
    }
};

const Chart: React.FC<ChartProps> = ({data, indicators, enabled, configs, symbol, onLoadOlderData}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const [legend, setLegend] = useState<LegendEntry[]>([]);
    const [chartHeight, setChartHeight] = useState(600);
    const chartRef = useRef<ReturnType<typeof createChart> | null>(null);
    const visibleRangeRef = useRef<{ from: Time; to: Time } | null>(null);
    const prevSymbolRef = useRef<string>('');

    // Rebuild the chart whenever the data or the visible indicator set changes. Recreating (rather than
    // diffing series) keeps pane management simple; the trade-off is that toggling resets the zoom.
    useEffect(() => {
        if (!chartContainerRef.current || data.length === 0) {
            setLegend([]);
            return;
        }

        // Reset visible range if symbol changed
        if (prevSymbolRef.current !== symbol) {
            visibleRangeRef.current = null;
            prevSymbolRef.current = symbol;
        }

        const width = chartContainerRef.current.clientWidth;
        const height = chartContainerRef.current.clientHeight || 600;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: {type: ColorType.Solid, color: '#020617'},
                textColor: '#94a3b8',
            },
            grid: {
                vertLines: {color: '#1e293b'},
                horzLines: {color: '#1e293b'},
            },
            width: width,
            height: height,
            timeScale: {borderColor: '#334155', timeVisible: true},
        });
        chartRef.current = chart;

        const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#22c55e',
            wickDownColor: '#ef4444',
        }, 0);
        candlestickSeries.setData(sortedData.map((d) => ({
            time: d.date as Time,
            open: Number(d.open),
            high: Number(d.high),
            low: Number(d.low),
            close: Number(d.close),
        })));

        const volumeSeries = chart.addSeries(HistogramSeries, {
            color: '#3b82f6',
            priceFormat: {type: 'volume'},
            priceScaleId: '',
        }, 0);
        volumeSeries.priceScale().applyOptions({scaleMargins: {top: 0.8, bottom: 0}});
        volumeSeries.setData(sortedData.map((d) => ({
            time: d.date as Time,
            value: Number(d.vol),
            color: Number(d.close) >= Number(d.open) ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)',
        })));

        const legendEntries: LegendEntry[] = [];
        let colorIdx = 0;
        const nextColor = () => PALETTE[colorIdx++ % PALETTE.length];
        let nextPane = 1;

        for (const series of indicators) {
            if (!enabled.has(indicatorKey(series))) continue;
            const placement = placementFor(series);
            const paneIndex = placement === 'oscillator' ? nextPane++ : 0;

            for (const output of outputsFor(series.type)) {
                const points = lineData(series, output.name);
                if (points.length === 0) continue;
                const color = nextColor();
                const formattedTitle = formatLabel(series.label) + output.suffix;

                if (output.style === 'histogram') {
                    const hist = chart.addSeries(HistogramSeries, {color, priceLineVisible: false}, paneIndex);
                    hist.setData(points);
                } else {
                    const line: ISeriesApi<'Line'> = chart.addSeries(LineSeries, {
                        color,
                        lineWidth: 1,
                        priceLineVisible: false,
                        lastValueVisible: true,
                        // Price-scale overlays share the candle scale; volume-overlays share the volume scale.
                        ...(placement === 'volumeOverlay' ? {priceScaleId: ''} : {}),
                    }, paneIndex);
                    line.setData(points);

                    // Add dynamic bounds lines from backend config for subpane oscillator series
                    if (placement === 'oscillator') {
                        const matchedConfig = configs.find(
                            (c) => c.type === series.type && c.params === series.params
                        );
                        let upper = matchedConfig?.upperBound;
                        let lower = matchedConfig?.lowerBound;

                        // Fallback defaults
                        if (upper === undefined || upper === null) {
                            if (series.type === 'RSI') upper = 70;
                            else if (series.type === 'STOCHASTIC') upper = 80;
                        }
                        if (lower === undefined || lower === null) {
                            if (series.type === 'RSI') lower = 30;
                            else if (series.type === 'STOCHASTIC') lower = 20;
                        }

                        if (upper !== undefined && upper !== null) {
                            line.createPriceLine({
                                price: Number(upper),
                                color: '#94a3b8', // slate-400 (visible gray)
                                lineWidth: 1,
                                lineStyle: LineStyle.Dotted,
                                axisLabelVisible: true,
                                title: '',
                            });
                        }
                        if (lower !== undefined && lower !== null) {
                            line.createPriceLine({
                                price: Number(lower),
                                color: '#94a3b8', // slate-400 (visible gray)
                                lineWidth: 1,
                                lineStyle: LineStyle.Dotted,
                                axisLabelVisible: true,
                                title: '',
                            });
                        }
                    }
                }
                
                // Exclude oscillator subpane indicators from the main chart legend
                if (placement !== 'oscillator') {
                    const latestValue = points[points.length - 1]?.value;
                    const displayLabel = latestValue !== undefined ? `${formattedTitle} - ${latestValue.toFixed(2)}` : formattedTitle;
                    legendEntries.push({label: displayLabel, color});
                }
            }
        }

        // Give the price pane the most height; oscillator panes share the rest.
        try {
            const panes = chart.panes();
            if (panes.length > 1) {
                panes[0].setStretchFactor(3);
                for (let i = 1; i < panes.length; i++) panes[i].setStretchFactor(1);
            }
        } catch {
            // setStretchFactor unavailable — fall back to default pane sizing.
        }

        // Set visible range (either restore saved or set default 6 months)
        if (visibleRangeRef.current) {
            chart.timeScale().setVisibleRange(visibleRangeRef.current);
        } else {
            // Default view: latest six months.
            const lastDate = sortedData[sortedData.length - 1].date;
            const sixMonthsAgo = new Date(lastDate);
            sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
            chart.timeScale().setVisibleRange({
                from: sixMonthsAgo.toISOString().split('T')[0] as Time,
                to: lastDate as Time,
            });
        }

        setLegend(legendEntries);

        // ResizeObserver manages dynamic resizing for both width and height (e.g. sidebar toggle)
        const resizeObserver = new ResizeObserver((entries) => {
            if (!entries || entries.length === 0) return;
            const {width: newWidth, height: newHeight} = entries[0].contentRect;
            chart.applyOptions({ width: newWidth, height: newHeight });
            setChartHeight(newHeight);
        });

        if (chartContainerRef.current) {
            resizeObserver.observe(chartContainerRef.current);
            setChartHeight(chartContainerRef.current.clientHeight || height);
        }

        // Subscribe to time scale visible range changes
        chart.timeScale().subscribeVisibleTimeRangeChange((range) => {
            if (range) {
                visibleRangeRef.current = range;
            }
        });

        // Trigger load older data when scrolled left
        chart.timeScale().subscribeVisibleLogicalRangeChange((logicalRange) => {
            if (!logicalRange) return;
            if (logicalRange.from <= 2) {
                onLoadOlderData();
            }
        });

        return () => {
            resizeObserver.disconnect();
            chartRef.current = null;
            chart.remove();
        };
    }, [data, indicators, enabled, configs, symbol, onLoadOlderData]);

    const handleResetZoom = () => {
        if (!chartRef.current || data.length === 0) return;
        const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));
        const lastDate = sortedData[sortedData.length - 1].date;
        const sixMonthsAgo = new Date(lastDate);
        sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
        const fromStr = sixMonthsAgo.toISOString().split('T')[0] as Time;
        const toStr = lastDate as Time;

        chartRef.current.timeScale().setVisibleRange({
            from: fromStr,
            to: toStr,
        });
        visibleRangeRef.current = { from: fromStr, to: toStr };
    };

    const oscillatorIndicators = indicators.filter((series) => enabled.has(indicatorKey(series)) && placementFor(series) === 'oscillator');

    return (
        <div className="relative w-full h-full min-h-0 flex-1 flex flex-col">
            <button
                onClick={handleResetZoom}
                className="absolute top-2 right-2 z-10 flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 hover:bg-slate-800 text-slate-300 hover:text-white px-2.5 py-1 rounded shadow-lg backdrop-blur text-xs font-semibold transition-all hover:scale-105 active:scale-95"
                title="Reset zoom to default (6 months)"
            >
                <RefreshCw size={12} className="animate-hover" />
                <span>Reset Zoom</span>
            </button>

            {legend.length > 0 && (
                <div className="absolute top-2 left-2 z-10 flex flex-col gap-0.5 bg-slate-900/70 rounded px-2 py-1 backdrop-blur pointer-events-none">
                    {legend.map((e) => (
                        <span key={e.label} className="flex items-center gap-1.5 text-[10px] font-medium text-slate-300">
                            <span className="inline-block w-2.5 h-0.5 rounded" style={{backgroundColor: e.color}}/>
                            {e.label}
                        </span>
                    ))}
                </div>
            )}

            {/* Subpane Overlay Headers */}
            {oscillatorIndicators.map((series, idx) => {
                const N = oscillatorIndicators.length;
                const top = chartHeight * (3 + idx) / (3 + N);
                const valuesStr = getLatestValuesString(series);
                return (
                    <div 
                        key={indicatorKey(series)}
                        className="absolute left-2 z-10 bg-slate-900/75 border border-slate-800 rounded px-2 py-0.5 backdrop-blur text-[10px] font-bold text-slate-300 pointer-events-none select-none"
                        style={{ top: `${top + 4}px` }}
                    >
                        <span className="text-slate-400 mr-1">{formatLabel(series.label)}:</span>
                        <span className="font-mono text-white">{valuesStr}</span>
                    </div>
                );
            })}

            <div ref={chartContainerRef} className="w-full h-full" style={{backgroundColor: '#020617'}}/>
        </div>
    );
};

export default Chart;
