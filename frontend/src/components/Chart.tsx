import React, {useEffect, useRef} from 'react';
import type {IChartApi, ISeriesApi, Time,} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, createSeriesMarkers, HistogramSeries,} from 'lightweight-charts';
import type {BacktestSignal, CandleData} from '../services/api';

interface ChartProps {
    data: CandleData[];
    signal: BacktestSignal[];
    selectedStrategy: string;
}

const Chart: React.FC<ChartProps> = ({data, signal, selectedStrategy}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
    const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);
    const seriesMarkersRef = useRef<any>(null);

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
                timeVisible: true,
            }
        });

        chartRef.current = chart;

        candlestickSeriesRef.current = chart.addSeries(CandlestickSeries, {
            upColor: '#22c55e',
            downColor: '#ef4444',
            borderVisible: false,
            wickUpColor: '#22c55e',
            wickDownColor: '#ef4444',
        });

        const volumeSeries = chart.addSeries(HistogramSeries, {
            color: '#3b82f6',
            priceFormat: {
                type: 'volume',
            },
            priceScaleId: '',
        });
        volumeSeries.priceScale().applyOptions({
            scaleMargins: {
                top: 0.8,
                bottom: 0,
            },
        });
        volumeSeriesRef.current = volumeSeries;

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
        if (!chartRef.current || !candlestickSeriesRef.current || !volumeSeriesRef.current || data.length === 0) return;

        // Financial data usually comes sorted, but we ensure it for chart stability
        const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));

        const formattedCandlestickData = [];
        const formattedVolumeData = [];

        for (const d of sortedData) {
            const open = Number(d.open);
            const high = Number(d.high);
            const low = Number(d.low);
            const close = Number(d.close);
            const vol = Number(d.vol);

            formattedCandlestickData.push({
                time: d.date as Time,
                open,
                high,
                low,
                close,
            });

            formattedVolumeData.push({
                time: d.date as Time,
                value: vol,
                color: close >= open ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)',
            });
        }

        candlestickSeriesRef.current.setData(formattedCandlestickData);
        volumeSeriesRef.current.setData(formattedVolumeData);

        // Plot signal markers
        const signalMarkers = signal.map(s => ({
            time: s.date as Time,
            position: s.action === 'ENTER_LONG' ? 'belowBar' : (s.action === 'ENTER_SHORT' ? 'aboveBar' : 'belowBar') as any,
            color: s.action === 'ENTER_LONG' ? '#22c55e' : (s.action === 'ENTER_SHORT' ? '#ef4444' : '#3b82f6'),
            shape: s.action === 'ENTER_LONG' ? 'arrowUp' : (s.action === 'ENTER_SHORT' ? 'arrowDown' : 'arrowUp') as any,
            text: s.action.replace('ENTER_', ''),
            size: 2,
        }));
        // Sort signal markers by time to avoid lightweight-charts warnings/errors
        signalMarkers.sort((a, b) => (a.time as string).localeCompare(b.time as string));

        if (seriesMarkersRef.current) {
            seriesMarkersRef.current.setMarkers(signalMarkers);
        } else {
            seriesMarkersRef.current = createSeriesMarkers(candlestickSeriesRef.current, signalMarkers);
        }

        // Set initial display to latest six months
        const lastDate = sortedData[sortedData.length - 1].date;
        const lastDateObj = new Date(lastDate);
        const sixMonthsAgo = new Date(lastDateObj);
        sixMonthsAgo.setMonth(sixMonthsAgo.getMonth() - 6);
        const sixMonthsAgoStr = sixMonthsAgo.toISOString().split('T')[0];

        chartRef.current.timeScale().setVisibleRange({
            from: sixMonthsAgoStr as Time,
            to: lastDate as Time,
        });
    }, [data, signal, selectedStrategy]);

    return <div ref={chartContainerRef} style={{width: '100%', height: '600px', backgroundColor: '#020617'}}/>;
};

export default Chart;
