import React, {useEffect, useRef} from 'react';
import type {IChartApi, IPriceLine, ISeriesApi, SeriesMarker, Time} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, createSeriesMarkers, LineSeries} from 'lightweight-charts';
import type {BacktestSignal, RenkoDataResponse} from '../services/api';

interface RenkoChartProps {
    data: RenkoDataResponse;
    signal: BacktestSignal[];
    selectedStrategy: string;
}

const RenkoChart: React.FC<RenkoChartProps> = ({data, signal, selectedStrategy}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
    const emaSeriesRef = useRef<ISeriesApi<'Line'> | null>(null);
    const seriesMarkersRef = useRef<any>(null);
    const currentPriceLineRightRef = useRef<IPriceLine | null>(null);
    const slPriceLineRightRef = useRef<IPriceLine | null>(null);
    const dateMappingRef = useRef<string[]>([]);

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
                    return dateMappingRef.current[index] || '';
                },
            },
            localization: {
                timeFormatter: (time: Time) => {
                    const index = typeof time === 'number' ? time : 0;
                    return dateMappingRef.current[index] || '';
                }
            }
        });

        chartRef.current = chart;

        const candlestickSeries = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickVisible: false,
        });
        candlestickSeriesRef.current = candlestickSeries;

        const emaSeries = chart.addSeries(LineSeries, {
            color: '#2962FF', // Blue
            lineWidth: 1,
            title: 'EMA 12',
        });
        emaSeriesRef.current = emaSeries;

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
        if (!chartRef.current || !candlestickSeriesRef.current || !emaSeriesRef.current || data.bricks.length === 0) return;

        // Store date mapping for the formatters
        dateMappingRef.current = data.bricks.map(b => b.date);

        // Process bricks to ensure unique timestamps
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

        // Calculate and set EMA 12
        const period = 12;
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
        emaSeriesRef.current.setData(emaData);

        // Set markers for trend numbers
        const trendMarkers: SeriesMarker<Time>[] = data.bricks.map((b, i) => ({
            time: i as unknown as Time,
            position: 'inBar',
            color: '#ffffff',
            shape: 'text' as any,
            text: `${b.zone}/${b.trend}`,
            size: 1,
        }));

        // Plot signal markers
        const signalMarkers: any[] = signal.flatMap(s => {
            // Find the index of the last brick on this date
            let brickIndex = -1;
            for (let j = data.bricks.length - 1; j >= 0; j--) {
                if (data.bricks[j].date === s.date) {
                    brickIndex = j;
                    break;
                }
            }
            if (brickIndex === -1) return [];

            return {
                time: brickIndex as unknown as Time,
                position: s.action === 'ENTER_LONG' ? 'belowBar' : (s.action === 'ENTER_SHORT' ? 'aboveBar' : 'belowBar') as any,
                color: s.action === 'ENTER_LONG' ? '#22c55e' : (s.action === 'ENTER_SHORT' ? '#ef4444' : '#3b82f6'),
                shape: s.action === 'ENTER_LONG' ? 'arrowUp' : (s.action === 'ENTER_SHORT' ? 'arrowDown' : 'arrowUp') as any,
                text: s.action.replace('ENTER_', ''),
                size: 2,
            };
        });

        const allMarkers = [...trendMarkers, ...signalMarkers];
        // Sort all markers by time
        allMarkers.sort((a, b) => (a.time as unknown as number) - (b.time as unknown as number));

        if (seriesMarkersRef.current) {
            seriesMarkersRef.current.setMarkers(allMarkers);
        } else {
            seriesMarkersRef.current = createSeriesMarkers(candlestickSeriesRef.current, allMarkers);
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

        // Add Current Price line
        currentPriceLineRightRef.current = candlestickSeriesRef.current.createPriceLine({
            price: data.current_price,
            color: currentColor,
            lineWidth: 1,
            lineStyle: 0, // Solid
            axisLabelVisible: true,
        });

        // Add SL Price line
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

        let startIndex = data.bricks.findIndex(b => b.date >= sixMonthsAgoStr);
        if (startIndex === -1) startIndex = 0;

        chartRef.current.timeScale().setVisibleRange({
            from: startIndex as unknown as Time,
            to: (data.bricks.length - 1) as unknown as Time,
        });
    }, [data, signal, selectedStrategy]);

    return (
        <div ref={chartContainerRef} style={{width: '100%', height: '600px', backgroundColor: '#020617'}}/>
    );
};

export default RenkoChart;
