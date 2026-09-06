import React, {useEffect, useRef, useState} from 'react';
import type {
    ISeriesApi,
    Time,
    ISeriesPrimitive,
    IPrimitivePaneView,
    IPrimitivePaneRenderer,
    LogicalRange,
    Logical
} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, HistogramSeries, LineSeries, LineStyle, createSeriesMarkers} from 'lightweight-charts';
import { INDICATOR_COLORS } from '../config/indicatorColors';

/* eslint-disable @typescript-eslint/no-explicit-any */
// Custom series primitive to fill a background color band between upper and lower bounds.
class HorizontalBandPrimitive implements ISeriesPrimitive {
    private _series: ISeriesApi<any> | null = null;
    private _topPrice: number;
    private _bottomPrice: number;
    private _color: string;

    constructor(topPrice: number, bottomPrice: number, color: string) {
        this._topPrice = topPrice;
        this._bottomPrice = bottomPrice;
        this._color = color;
    }

    attached(param: { series: ISeriesApi<any> }) {
        this._series = param.series;
    }

    detached() {
        this._series = null;
    }

    paneViews() {
        return [new HorizontalBandPaneView(this)];
    }

    getTopPrice() { return this._topPrice; }
    getBottomPrice() { return this._bottomPrice; }
    getColor() { return this._color; }
    getSeries() { return this._series; }
}

class HorizontalBandPaneView implements IPrimitivePaneView {
    private _primitive: HorizontalBandPrimitive;

    constructor(primitive: HorizontalBandPrimitive) {
        this._primitive = primitive;
    }

    zOrder() {
        return 'bottom' as const;
    }

    renderer() {
        return new HorizontalBandRenderer(this._primitive);
    }
}

class HorizontalBandRenderer implements IPrimitivePaneRenderer {
    private _primitive: HorizontalBandPrimitive;

    constructor(primitive: HorizontalBandPrimitive) {
        this._primitive = primitive;
    }

    draw(target: any) {
        const series = this._primitive.getSeries();
        if (!series) return;

        const topY = series.priceToCoordinate(this._primitive.getTopPrice());
        const bottomY = series.priceToCoordinate(this._primitive.getBottomPrice());

        if (topY === null || bottomY === null) return;

        target.useBitmapCoordinateSpace((scope: any) => {
            const ctx = scope.context;
            const verticalPixelRatio = scope.verticalPixelRatio;

            const renderTopY = topY * verticalPixelRatio;
            const renderBottomY = bottomY * verticalPixelRatio;
            const renderWidth = scope.bitmapSize.width;

            ctx.fillStyle = this._primitive.getColor();
            ctx.fillRect(0, renderTopY, renderWidth, renderBottomY - renderTopY);
        });
    }
}
/* eslint-enable @typescript-eslint/no-explicit-any */



// Hardcoded indicator color mapping from configuration
const getIndicatorColor = (type: string, source: string, params: string, outputName: string): string | null => {
    const rules = INDICATOR_COLORS[type];
    if (!rules) return null;

    const cleanParams = params.replace(/\s+/g, '');

    if (rules.bySourceAndParams) {
        const key = `${source}|${cleanParams}`;
        if (rules.bySourceAndParams[key]) {
            return rules.bySourceAndParams[key];
        }
    }

    if (rules.byParams && rules.byParams[cleanParams]) {
        return rules.byParams[cleanParams];
    }

    if (rules.byOutput && rules.byOutput[outputName]) {
        return rules.byOutput[outputName];
    }

    return rules.default || null;
};

import {type DailyCandleData, type IndicatorSeries, indicatorKey, type IndicatorConfig, type CandlestickPatternData, type SupportResistanceData} from '../services/api';
import {RefreshCw} from 'lucide-react';

interface ChartProps {
    data: DailyCandleData[];
    indicators: IndicatorSeries[];
    enabled: Set<string>;
    configs: IndicatorConfig[];
    symbol: string;
    timeframe: string;
    candlestickPatterns?: CandlestickPatternData[];
    showCandlestickPatterns?: boolean;
    supportResistances?: SupportResistanceData[];
    showSupportResistance?: boolean;
    onLoadOlderData: () => void;
}

interface LegendEntry {
    label: string;
    color: string;
}

// Deterministic palette; assigned in enabled-order so a given chart is stable across renders.
const PALETTE = ['#f59e0b', '#06b6d4', '#a855f7', '#ec4899', '#84cc16', '#f43f5e', '#22d3ee', '#fb923c', '#eab308'];

const INDICATOR_ORDER = [
  "EMA (5)",
  "EMA (13)",
  "EMA (26)",
  "BB (20)",
  "RSI (14)",
  "Stoch (14,3,3)",
  "MACD (12,26,9)"
];

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
        case 'BB':
            return [
                {name: 'upper', style: 'line', suffix: ' Upper'},
                {name: 'middle', style: 'line', suffix: ' Middle'},
                {name: 'lower', style: 'line', suffix: ' Lower'},
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
        case 'BB': {
            const upperPoints = lineData(series, 'upper');
            const middlePoints = lineData(series, 'middle');
            const lowerPoints = lineData(series, 'lower');
            const uVal = upperPoints[upperPoints.length - 1]?.value;
            const mVal = middlePoints[middlePoints.length - 1]?.value;
            const lVal = lowerPoints[lowerPoints.length - 1]?.value;
            return `Upper: ${uVal !== undefined ? uVal.toFixed(2) : 'N/A'}, Mid: ${mVal !== undefined ? mVal.toFixed(2) : 'N/A'}, Lower: ${lVal !== undefined ? lVal.toFixed(2) : 'N/A'}`;
        }
        default: {
            const valPoints = lineData(series, 'value');
            const val = valPoints[valPoints.length - 1]?.value;
            return val !== undefined ? val.toFixed(2) : 'N/A';
        }
    }
};

const EMPTY_CANDLESTICK_PATTERNS: CandlestickPatternData[] = [];
const EMPTY_SUPPORT_RESISTANCES: SupportResistanceData[] = [];

const Chart: React.FC<ChartProps> = ({data, indicators, enabled, configs, symbol, timeframe, candlestickPatterns = EMPTY_CANDLESTICK_PATTERNS, showCandlestickPatterns = false, supportResistances = EMPTY_SUPPORT_RESISTANCES, showSupportResistance = false, onLoadOlderData}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const [legend, setLegend] = useState<LegendEntry[]>([]);
    const [chartHeight, setChartHeight] = useState(600);
    const [tooltip, setTooltip] = useState<{
        visible: boolean;
        x: number;
        y: number;
        longName: string;
        shortName: string;
        sentiment: string;
    } | null>(null);
    const chartRef = useRef<ReturnType<typeof createChart> | null>(null);
    const visibleLogicalRangeRef = useRef<LogicalRange | null>(null);
    const prevDataLengthRef = useRef<number>(0);
    const prevSymbolRef = useRef<string>('');
    const prevTimeframeRef = useRef<string>('');

    // Rebuild the chart whenever the data or the visible indicator set changes. Recreating (rather than
    // diffing series) keeps pane management simple; the trade-off is that toggling resets the zoom.
    useEffect(() => {
        if (!chartContainerRef.current || data.length === 0) {
            setLegend([]);
            return;
        }

        // Reset logical range if symbol or timeframe changed
        if (prevSymbolRef.current !== symbol || prevTimeframeRef.current !== timeframe) {
            visibleLogicalRangeRef.current = null;
            prevDataLengthRef.current = 0;
            prevSymbolRef.current = symbol;
            prevTimeframeRef.current = timeframe;
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
            timeScale: {
                borderColor: '#334155',
                timeVisible: true,
                rightOffset: 10,
            },
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

        let visibleCandlestickPatterns = candlestickPatterns;
        if (!showCandlestickPatterns) {
            visibleCandlestickPatterns = [];
        }

        // Deduplicate candlestick patterns by date to prevent duplicate time assertions in lightweight-charts
        const uniqueCandlestickPatternsMap = new Map<string, CandlestickPatternData>();
        const safeCandlestickPatterns = Array.isArray(visibleCandlestickPatterns) ? visibleCandlestickPatterns : [];
        for (const p of safeCandlestickPatterns) {
            if (p && p.date && !uniqueCandlestickPatternsMap.has(p.date)) {
                uniqueCandlestickPatternsMap.set(p.date, p);
            }
        }
        const deduplicatedCandlestickPatterns = Array.from(uniqueCandlestickPatternsMap.values());

        if (deduplicatedCandlestickPatterns.length > 0) {
            const markers = deduplicatedCandlestickPatterns.map((p) => {
                const isBullish = p.sentiment.startsWith('BULLISH');
                return {
                    time: p.date as Time,
                    position: isBullish ? 'belowBar' as const : 'aboveBar' as const,
                    color: isBullish ? '#22c55e' : '#ef4444',
                    shape: isBullish ? 'arrowUp' as const : 'arrowDown' as const,
                    text: p.shortName,
                };
            });
            createSeriesMarkers(candlestickSeries, markers);
        }

        if (showSupportResistance && supportResistances.length > 0) {
            supportResistances.forEach((sr) => {
                candlestickSeries.createPriceLine({
                    price: sr.zoneMidpoint,
                    color: sr.levelType === 'SUPPORT' ? '#22c55e' : '#ef4444',
                    lineWidth: 1,
                    lineStyle: LineStyle.Solid,
                    axisLabelVisible: true,
                    title: '',
                });
            });
        }

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

        const sortedIndicators = [...indicators].sort((a, b) => {
            const idxA = INDICATOR_ORDER.indexOf(a.label);
            const idxB = INDICATOR_ORDER.indexOf(b.label);
            if (idxA === -1 && idxB === -1) return a.label.localeCompare(b.label);
            if (idxA === -1) return 1;
            if (idxB === -1) return -1;
            return idxA - idxB;
        });

        for (const series of sortedIndicators) {
            const key = indicatorKey(series);
            const isVolMA = series.type === 'SMA' && series.source === 'VOLUME' && series.params === 'period=20';
            if (!isVolMA && !enabled.has(key)) continue;
            const placement = placementFor(series);
            const paneIndex = placement === 'oscillator' ? nextPane++ : 0;

            for (const output of outputsFor(series.type)) {
                const points = lineData(series, output.name);
                if (points.length === 0) continue;
                const hardcodedColor = getIndicatorColor(series.type, series.source, series.params, output.name);
                const color = hardcodedColor || nextColor();
                const formattedTitle = formatLabel(series.label) + output.suffix;

                if (output.style === 'histogram') {
                    const histData = points.map(p => ({
                        time: p.time,
                        value: p.value,
                        color: series.type === 'MACD'
                            ? (p.value >= 0 ? 'rgba(34, 197, 94, 0.7)' : 'rgba(239, 68, 68, 0.7)')
                            : color
                    }));
                    const hist = chart.addSeries(HistogramSeries, {priceLineVisible: false}, paneIndex);
                    hist.setData(histData);
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

                        // Attach the shaded background primitive for RSI and Stochastic bounds
                        if ((series.type === 'RSI' || series.type === 'STOCHASTIC') && (output.name === 'value' || output.name === 'k')) {
                            if (upper !== undefined && upper !== null && lower !== undefined && lower !== null) {
                                const bandColor = series.type === 'RSI'
                                    ? 'rgba(168, 85, 247, 0.1)' // RSI Purple with 10% opacity
                                    : 'rgba(59, 130, 246, 0.1)'; // Stochastic Blue with 10% opacity
                                const bandPrimitive = new HorizontalBandPrimitive(
                                    Number(upper),
                                    Number(lower),
                                    bandColor
                                );
                                line.attachPrimitive(bandPrimitive);
                            }
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

        // Adjust logical range if older data was prepended
        if (visibleLogicalRangeRef.current && prevDataLengthRef.current > 0) {
            const addedCount = sortedData.length - prevDataLengthRef.current;
            if (addedCount > 0) {
                visibleLogicalRangeRef.current = {
                    from: (visibleLogicalRangeRef.current.from + addedCount) as Logical,
                    to: (visibleLogicalRangeRef.current.to + addedCount) as Logical,
                };
            }
        }
        prevDataLengthRef.current = sortedData.length;

        // Set visible range (either restore saved or set default 250 bars)
        if (visibleLogicalRangeRef.current) {
            chart.timeScale().setVisibleLogicalRange(visibleLogicalRangeRef.current);
        } else {
            // Default view: latest 250 bars with 10 bars of empty space on the right
            requestAnimationFrame(() => {
                chart.timeScale().setVisibleLogicalRange({
                    from: sortedData.length - 250,
                    to: sortedData.length - 1 + 10,
                });
            });
        }

        const LEGEND_ORDER = ['EMA (5)', 'EMA (13)', 'EMA (26)', 'Vol (20)'];
        const getOrderIndex = (label: string): number => {
            const index = LEGEND_ORDER.findIndex(prefix => label.startsWith(prefix));
            return index === -1 ? 999 : index;
        };
        legendEntries.sort((a, b) => getOrderIndex(a.label) - getOrderIndex(b.label));

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

        // Subscribe to logical range changes for scroll position and older data loading
        chart.timeScale().subscribeVisibleLogicalRangeChange((logicalRange) => {
            if (!logicalRange) return;
            visibleLogicalRangeRef.current = logicalRange;
            if (logicalRange.from <= 2) {
                onLoadOlderData();
            }
        });

        // Subscribe to crosshair move events to show the pattern long name tooltip on hover
        chart.subscribeCrosshairMove((param) => {
            if (
                !param.time ||
                !param.point ||
                param.point.x < 0 ||
                param.point.y < 0
            ) {
                setTooltip(null);
                return;
            }

            let dateStr = '';
            if (typeof param.time === 'string') {
                dateStr = param.time;
            } else if (param.time && typeof param.time === 'object') {
                const t = param.time as { year?: number; month?: number; day?: number };
                if (t.year && t.month && t.day) {
                    const y = t.year;
                    const m = String(t.month).padStart(2, '0');
                    const d = String(t.day).padStart(2, '0');
                    dateStr = `${y}-${m}-${d}`;
                }
            }

            if (!dateStr) {
                setTooltip(null);
                return;
            }

            const pattern = deduplicatedCandlestickPatterns.find((p) => p.date === dateStr);

            if (pattern) {
                let tooltipX = param.point.x + 15;
                let tooltipY = param.point.y + 15;
                
                const containerWidth = chartContainerRef.current?.clientWidth || 0;
                const containerHeight = chartContainerRef.current?.clientHeight || 0;
                
                if (tooltipX + 220 > containerWidth) {
                    tooltipX = param.point.x - 235;
                }
                if (tooltipY + 80 > containerHeight) {
                    tooltipY = param.point.y - 95;
                }

                setTooltip({
                    visible: true,
                    x: tooltipX,
                    y: tooltipY,
                    longName: pattern.longName,
                    shortName: pattern.shortName,
                    sentiment: pattern.sentiment,
                });
            } else {
                setTooltip(null);
            }
        });

        return () => {
            resizeObserver.disconnect();
            chartRef.current = null;
            chart.remove();
        };
    }, [data, indicators, enabled, configs, symbol, timeframe, candlestickPatterns, showCandlestickPatterns, supportResistances, showSupportResistance, onLoadOlderData]);

    const handleResetZoom = () => {
        if (!chartRef.current || data.length === 0) return;
        const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));

        const targetRange = {
            from: (sortedData.length - 250) as Logical,
            to: (sortedData.length - 1 + 10) as Logical,
        };
        requestAnimationFrame(() => {
            chartRef.current?.timeScale().setVisibleLogicalRange(targetRange);
        });
        visibleLogicalRangeRef.current = targetRange;
    };

    const oscillatorIndicators = indicators
        .filter((series) => enabled.has(indicatorKey(series)) && placementFor(series) === 'oscillator')
        .sort((a, b) => {
            const idxA = INDICATOR_ORDER.indexOf(a.label);
            const idxB = INDICATOR_ORDER.indexOf(b.label);
            if (idxA === -1 && idxB === -1) return a.label.localeCompare(b.label);
            if (idxA === -1) return 1;
            if (idxB === -1) return -1;
            return idxA - idxB;
        });

    return (
        <div 
            className="relative w-full h-full min-h-0 flex-1 flex flex-col"
            onMouseLeave={() => setTooltip(null)}
        >
            <button
                onClick={handleResetZoom}
                className="absolute top-2 right-2 z-10 flex items-center gap-1.5 bg-slate-900/80 border border-slate-800 hover:bg-slate-800 text-slate-300 hover:text-white px-2.5 py-1 rounded shadow-lg backdrop-blur text-xs font-semibold transition-all hover:scale-105 active:scale-95"
                title="Reset zoom to default (250 bars)"
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

            {tooltip && tooltip.visible && (
                <div
                    className="absolute z-30 pointer-events-none bg-slate-900/90 border border-slate-700 text-white text-xs rounded-lg p-3 shadow-xl shadow-black/50 backdrop-blur-sm flex flex-col gap-1 transition-all duration-100 ease-out"
                    style={{
                        left: `${tooltip.x}px`,
                        top: `${tooltip.y}px`,
                    }}
                >
                    <div className="font-bold flex items-center gap-1.5 text-sm">
                        <span className={`w-2.5 h-2.5 rounded-full ${tooltip.sentiment.startsWith('BULLISH') ? 'bg-emerald-500 shadow-lg shadow-emerald-500/50' : 'bg-rose-500 shadow-lg shadow-rose-500/50'}`} />
                        <span className="text-white">{tooltip.longName}</span>
                        <span className="text-[10px] bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded font-mono uppercase tracking-wider">{tooltip.shortName}</span>
                    </div>
                    <div className="text-[10px] text-slate-400 font-semibold uppercase tracking-wider pl-4">
                        {tooltip.sentiment.replace(/_/g, ' ')}
                    </div>
                </div>
            )}
        </div>
    );
};

export default Chart;
