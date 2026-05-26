import React, {useEffect, useRef} from 'react';
import type {IChartApi, ISeriesApi, Time} from 'lightweight-charts';
import {CandlestickSeries, ColorType, createChart, HistogramSeries} from 'lightweight-charts';
import type {DailyCandleData} from '../services/api';

interface ChartProps {
    data: DailyCandleData[];
}

const Chart: React.FC<ChartProps> = ({data}) => {
    const chartContainerRef = useRef<HTMLDivElement>(null);
    const chartRef = useRef<IChartApi | null>(null);
    const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
    const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);

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
    }, [data]);

    return <div ref={chartContainerRef} style={{width: '100%', height: '600px', backgroundColor: '#020617'}}/>;
};

export default Chart;
