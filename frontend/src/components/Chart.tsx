import React, { useEffect, useRef } from 'react';
import {
  createChart,
  ColorType,
  CandlestickSeries,
  HistogramSeries,
  LineSeries,
} from 'lightweight-charts';
import type {
  IChartApi,
  ISeriesApi,
} from 'lightweight-charts';
import type { MarketData } from '../services/api';
import type { IndicatorConfig } from './Controls';

interface ChartProps {
  data: MarketData[];
  indicators: IndicatorConfig[];
}

const Chart: React.FC<ChartProps> = ({ data, indicators }) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const volumeSeriesRef = useRef<ISeriesApi<'Histogram'> | null>(null);
  const indicatorSeriesRefs = useRef<{ [key: string]: ISeriesApi<'Line'> }>({});

  useEffect(() => {
    if (!chartContainerRef.current) return;

    const chart = createChart(chartContainerRef.current, {
      layout: {
        background: { type: ColorType.Solid, color: '#020617' },
        textColor: '#94a3b8',
      },
      grid: {
        vertLines: { color: '#1e293b' },
        horzLines: { color: '#1e293b' },
      },
      width: chartContainerRef.current.clientWidth,
      height: 600,
      timeScale: {
        borderColor: '#334155',
        timeVisible: true,
      }
    });

    chartRef.current = chart;

    const candlestickSeries = chart.addSeries(CandlestickSeries, {
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderVisible: false,
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });
    candlestickSeriesRef.current = candlestickSeries;

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
        chart.applyOptions({ width: chartContainerRef.current.clientWidth });
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

    const sortedData = [...data].sort((a, b) => a.date.localeCompare(b.date));

    const formattedCandlestickData = sortedData.map((d) => ({
      time: d.date,
      open: Number(d.open),
      high: Number(d.high),
      low: Number(d.low),
      close: Number(d.close),
    }));

    candlestickSeriesRef.current.setData(formattedCandlestickData);

    const formattedVolumeData = sortedData.map((d) => ({
      time: d.date,
      value: Number(d.vol),
      color: Number(d.close) >= Number(d.open) ? 'rgba(34, 197, 94, 0.5)' : 'rgba(239, 68, 68, 0.5)',
    }));

    volumeSeriesRef.current.setData(formattedVolumeData);

    // Indicators
    indicators.forEach((indicator) => {
      if (indicator.visible) {
        if (!indicatorSeriesRefs.current[indicator.id]) {
          indicatorSeriesRefs.current[indicator.id] = chartRef.current!.addSeries(LineSeries, {
            color: indicator.color,
            lineWidth: 2,
            title: indicator.label,
          });
        }

        const indicatorData = sortedData.map((d) => ({
          time: d.date,
          value: Number((d as any)[indicator.id]),
        })).filter(d => !isNaN(d.value));

        indicatorSeriesRefs.current[indicator.id].setData(indicatorData);
      } else {
        if (indicatorSeriesRefs.current[indicator.id]) {
          chartRef.current!.removeSeries(indicatorSeriesRefs.current[indicator.id]);
          delete indicatorSeriesRefs.current[indicator.id];
        }
      }
    });

    chartRef.current.timeScale().fitContent();
  }, [data, indicators]);

  return <div ref={chartContainerRef} style={{ width: '100%', height: '600px', backgroundColor: '#020617' }} />;
};

export default Chart;
