import React, { useEffect, useRef } from 'react';
import {
  createChart,
  ColorType,
} from 'lightweight-charts';
import type {
  IChartApi,
  ISeriesApi,
  CandlestickData,
  LineData,
  HistogramData,
  UTCTimestamp,
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
        background: { type: ColorType.Solid, color: '#0f172a' },
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
        secondsVisible: false,
      },
      rightPriceScale: {
        borderColor: '#334155',
      },
    });

    chartRef.current = chart;

    const candlestickSeries = (chart as any).addCandlestickSeries({
      upColor: '#22c55e',
      downColor: '#ef4444',
      borderVisible: false,
      wickUpColor: '#22c55e',
      wickDownColor: '#ef4444',
    });
    candlestickSeriesRef.current = candlestickSeries;

    const volumeSeries = (chart as any).addHistogramSeries({
      color: '#3b82f6',
      priceFormat: {
        type: 'volume',
      },
      priceScaleId: '', // set as an overlay
    });
    volumeSeries.priceScale().applyOptions({
      scaleMargins: {
        top: 0.8,
        bottom: 0,
      },
    });
    volumeSeriesRef.current = volumeSeries;

    // Handle window resize
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

    const formattedCandlestickData: CandlestickData[] = data.map((d) => ({
      time: (new Date(d.date).getTime() / 1000) as UTCTimestamp,
      open: d.open,
      high: d.high,
      low: d.low,
      close: d.close,
    })).sort((a, b) => (a.time as number) - (b.time as number));

    candlestickSeriesRef.current.setData(formattedCandlestickData);

    const formattedVolumeData: HistogramData[] = data.map((d) => ({
      time: (new Date(d.date).getTime() / 1000) as UTCTimestamp,
      value: d.vol,
      color: d.close >= d.open ? '#22c55e' : '#ef4444',
    })).sort((a, b) => (a.time as number) - (b.time as number));

    volumeSeriesRef.current.setData(formattedVolumeData);

    // Indicators
    indicators.forEach((indicator) => {
      if (indicator.visible) {
        if (!indicatorSeriesRefs.current[indicator.id]) {
          indicatorSeriesRefs.current[indicator.id] = (chartRef.current! as any).addLineSeries({
            color: indicator.color,
            lineWidth: 2,
            title: indicator.label,
          });
        }

        const indicatorData: LineData[] = data.map((d) => ({
          time: (new Date(d.date).getTime() / 1000) as UTCTimestamp,
          value: (d as any)[indicator.id],
        })).filter(d => d.value !== null && d.value !== undefined)
           .sort((a, b) => (a.time as number) - (b.time as number));

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

  return <div ref={chartContainerRef} className="w-full h-full min-h-[600px]" />;
};

export default Chart;
