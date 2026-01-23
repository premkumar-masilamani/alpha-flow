import React, { useEffect, useRef } from 'react';
import {
  createChart,
  ColorType,
  CandlestickSeries,
  createSeriesMarkers,
} from 'lightweight-charts';
import type {
  IChartApi,
  ISeriesApi,
  Time,
  SeriesMarker,
  IPriceLine,
} from 'lightweight-charts';
import type { RenkoData } from '../services/api';

interface RenkoChartProps {
  data: RenkoData;
}

const RenkoChart: React.FC<RenkoChartProps> = ({ data }) => {
  const chartContainerRef = useRef<HTMLDivElement>(null);
  const chartRef = useRef<IChartApi | null>(null);
  const candlestickSeriesRef = useRef<ISeriesApi<'Candlestick'> | null>(null);
  const seriesMarkersRef = useRef<any>(null);
  const currentPriceLineRightRef = useRef<IPriceLine | null>(null);
  const slPriceLineRightRef = useRef<IPriceLine | null>(null);
  const dateMapping = useRef<string[]>([]);

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

    chart.priceScale('right').applyOptions({
      visible: true,
      borderColor: '#334155',
    });

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

  return <div ref={chartContainerRef} style={{ width: '100%', height: '600px', backgroundColor: '#020617' }} />;
};

export default RenkoChart;
