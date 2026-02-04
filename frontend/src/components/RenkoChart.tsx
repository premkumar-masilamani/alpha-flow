import React, {useEffect, useRef, useState} from 'react';
import type {IChartApi, IPriceLine, ISeriesApi, SeriesMarker, Time,} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, createSeriesMarkers, LineSeries,} from 'lightweight-charts';
import type {RenkoData} from '../services/api';

interface RenkoChartProps {
    data: RenkoData;
}

const SHORT_GMMA_PERIODS = [3, 5, 8, 10, 12, 15];
const LONG_GMMA_PERIODS = [30, 35, 40, 45, 50, 60];

const RenkoChart: React.FC<RenkoChartProps> = ({data}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
    const gmmaSeriesRef = useRef<Record<number, ISeriesApi<'Line'>>>({});
    const seriesMarkersRef = useRef<any>(null);
    const currentPriceLineRightRef = useRef<IPriceLine | null>(null);
    const slPriceLineRightRef = useRef<IPriceLine | null>(null);
    const dateMapping = useRef<string[]>([]);

    const [visiblePeriods, setVisiblePeriods] = useState<number[]>([
        ...SHORT_GMMA_PERIODS,
        ...LONG_GMMA_PERIODS
    ]);

    const togglePeriod = (period: number) => {
        setVisiblePeriods(prev =>
            prev.includes(period)
                ? prev.filter(p => p !== period)
                : [...prev, period]
        );
    };

    useEffect(() => {
        if (!chartContainerRef.current) return;

        const chart = createChart(chartContainerRef.current, {
            layout: {
                background: {type: ColorType.Solid, color: '#020617'},
                textColor: '#94a3b8',
            },
            grid: {
                vertLines: {color: '#1e293b'},
                horzLines: {color: '#1e293b'},
            },
            width: chartContainerRef.current.clientWidth,
            height: 600,
            timeScale: {
                borderColor: '#334155',
                timeVisible: false,
                tickMarkFormatter: (time: Time) => {
                    const index = typeof time === 'number' ? time : 0;
                    return dateMapping.current[index] || '';
                },
            },
            localization: {
                timeFormatter: (time: Time) => {
                    const index = typeof time === 'number' ? time : 0;
                    return dateMapping.current[index] || '';
                }
            }
        });

        chartRef.current = chart;

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickVisible: false,
            priceScaleId: 'right',
        });
        candlestickSeriesRef.current = candlestickSeries;

        SHORT_GMMA_PERIODS.forEach(period => {
            gmmaSeriesRef.current[period] = chart.addSeries(LineSeries, {
                color: '#22c55e', // Green
                lineWidth: 1,
                priceScaleId: 'right',
                title: `EMA ${period}`,
            });
        });

        LONG_GMMA_PERIODS.forEach(period => {
            gmmaSeriesRef.current[period] = chart.addSeries(LineSeries, {
                color: '#ef4444', // Red
                lineWidth: 1,
                priceScaleId: 'right',
                title: `EMA ${period}`,
            });
        });

        chart.priceScale('right').applyOptions({
            visible: true,
            borderColor: '#334155',
        });

        const handleResize = () => {
            if (chartContainerRef.current) {
                chart.applyOptions({width: chartContainerRef.current.clientWidth});
            }
        };
        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
            chart.remove();
        };
    }, []);

    useEffect(() => {
        if (!chartRef.current || !candlestickSeriesRef.current || data.bricks.length === 0) return;

        // Store date mapping for the formatters
        dateMapping.current = data.bricks.map(b => b.date);

        // Process bricks to ensure unique timestamps
        // We'll use a sequence of numbers as timestamps to keep bricks equally spaced
        const formattedBricks = data.bricks.map((b, i) => {
            const isUp = b.direction === 'up';
            const high = Number(b.high);
            const low = Number(b.low);
            return {
                time: i as unknown as Time,
                open: isUp ? low : high,
                high,
                low,
                close: isUp ? high : low,
            };
        });

        candlestickSeriesRef.current.setData(formattedBricks);

        // Calculate and set GMMA
        const allPeriods = [...SHORT_GMMA_PERIODS, ...LONG_GMMA_PERIODS];
        allPeriods.forEach(period => {
            const series = gmmaSeriesRef.current[period];
            if (series) {
                const k = 2 / (period + 1);
                let emaValue = formattedBricks[0].close;
                const emaData = formattedBricks.map((brick, i) => {
                    if (i > 0) {
                        emaValue = (brick.close * k) + (emaValue * (1 - k));
                    }
                    return {
                        time: brick.time,
                        value: emaValue,
                    };
                });
                series.setData(emaData);
            }
        });

        // Set markers for trend numbers
        const markers: SeriesMarker<Time>[] = data.bricks.map((b, i) => ({
            time: i as unknown as Time,
            position: 'inBar',
            color: '#ffffff',
            shape: 'text' as any,
            text: `${b.zone}/${b.trend}`,
            size: 1,
        }));

        if (seriesMarkersRef.current) {
            seriesMarkersRef.current.setMarkers(markers);
        } else {
            seriesMarkersRef.current = createSeriesMarkers(candlestickSeriesRef.current, markers);
        }

        // Remove old price lines if they exist
        if (currentPriceLineRightRef.current) {
            candlestickSeriesRef.current.removePriceLine(currentPriceLineRightRef.current);
        }
        if (slPriceLineRightRef.current) {
            candlestickSeriesRef.current.removePriceLine(slPriceLineRightRef.current);
        }

        // Determine Current line color based on latest brick direction
        const latestBrick = data.bricks[data.bricks.length - 1];
        const currentColor = latestBrick.direction === 'up' ? '#22c55e' : '#ef4444';

        // Add Current Price line (Right)
        currentPriceLineRightRef.current = candlestickSeriesRef.current.createPriceLine({
            price: data.current_price,
            color: currentColor,
            lineWidth: 1,
            lineStyle: 0, // Solid
            axisLabelVisible: true,
        });

        // Add SL Price line (Right)
        slPriceLineRightRef.current = candlestickSeriesRef.current.createPriceLine({
            price: data.stop_loss_price,
            color: '#3b82f6',
            lineWidth: 1,
            lineStyle: 0, // Solid
            axisLabelVisible: true,
        });

        // Set initial display to latest six months
        const lastDate = data.bricks[data.bricks.length - 1].date;
        const lastDateObj = new Date(lastDate);
        const sixMonthsAgo = new Date(lastDateObj);
        sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
        const sixMonthsAgoStr = sixMonthsAgo.toISOString().split('T')[0];

        // Find the first index that is >= sixMonthsAgoStr
        let startIndex = data.bricks.findIndex(b => b.date >= sixMonthsAgoStr);
        if (startIndex === -1) startIndex = 0;

        chartRef.current.timeScale().setVisibleRange({
            from: startIndex as unknown as Time,
            to: (data.bricks.length - 1) as unknown as Time,
        });
    }, [data]);

    useEffect(() => {
        Object.entries(gmmaSeriesRef.current).forEach(([period, series]) => {
            series.applyOptions({
                visible: visiblePeriods.includes(Number(period))
            });
        });
    }, [visiblePeriods]);

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-wrap gap-4 p-4 bg-slate-900/50 border border-slate-800 rounded-lg">
                <div className="flex flex-col gap-2">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Short GMMA</span>
                    <div className="flex flex-wrap gap-2">
                        {SHORT_GMMA_PERIODS.map(period => (
                            <label key={period} className="flex items-center gap-2 cursor-pointer group">
                                <input
                                    type="checkbox"
                                    checked={visiblePeriods.includes(period)}
                                    onChange={() => togglePeriod(period)}
                                    className="w-4 h-4 rounded border-slate-700 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900"
                                />
                                <span className="text-sm text-slate-300 group-hover:text-white transition-colors">
                                    EMA {period}
                                </span>
                            </label>
                        ))}
                    </div>
                </div>
                <div className="w-px bg-slate-800 self-stretch" />
                <div className="flex flex-col gap-2">
                    <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Long GMMA</span>
                    <div className="flex flex-wrap gap-2">
                        {LONG_GMMA_PERIODS.map(period => (
                            <label key={period} className="flex items-center gap-2 cursor-pointer group">
                                <input
                                    type="checkbox"
                                    checked={visiblePeriods.includes(period)}
                                    onChange={() => togglePeriod(period)}
                                    className="w-4 h-4 rounded border-slate-700 bg-slate-800 text-blue-500 focus:ring-blue-500 focus:ring-offset-slate-900"
                                />
                                <span className="text-sm text-slate-300 group-hover:text-white transition-colors">
                                    EMA {period}
                                </span>
                            </label>
                        ))}
                    </div>
                </div>
            </div>
            <div ref={chartContainerRef} style={{width: '100%', height: '600px', backgroundColor: '#020617'}}/>
        </div>
    );
};

export default RenkoChart;
