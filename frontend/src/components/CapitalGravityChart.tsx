import React, { useEffect, useRef, useState, useMemo } from 'react';
import type { MarketData } from '../services/api';

interface CapitalGravityChartProps {
  data: MarketData[];
}

const BAR_WIDTH = 60;
const BAR_SPACING = 40;
const TOTAL_BAR_WIDTH = BAR_WIDTH + BAR_SPACING;

const CapitalGravityChart: React.FC<CapitalGravityChartProps> = ({ data }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;
  const [scrollOffset, setScrollOffset] = useState(0);
  const [dimensions, setDimensions] = useState({ width: 0, height: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState(0);
  const [hoverData, setHoverData] = useState<{ d: MarketData, x: number, y: number } | null>(null);

  const [showVwap, setShowVwap] = useState(true);
  const [showHalo, setShowHalo] = useState(true);
  const [showVa, setShowVa] = useState(true);

  useEffect(() => {
    if (!containerRef.current) return;
    const updateDimensions = () => {
      if (containerRef.current) {
        setDimensions({
          width: containerRef.current.clientWidth,
          height: containerRef.current.clientHeight || 600,
        });
      }
    };
    updateDimensions();
    const resizeObserver = new ResizeObserver(updateDimensions);
    resizeObserver.observe(containerRef.current);
    return () => resizeObserver.disconnect();
  }, []);

  // Set initial scroll to show latest data
  useEffect(() => {
    if (dimensions.width > 0 && data.length > 0 && scrollOffset === 0) {
      const totalContentWidth = data.length * TOTAL_BAR_WIDTH;
      if (totalContentWidth > dimensions.width) {
        setScrollOffset(totalContentWidth - dimensions.width + BAR_SPACING);
      }
    }
  }, [dimensions.width, data.length]);

  const visibleRange = useMemo(() => {
    const startIdx = Math.max(0, Math.floor(scrollOffset / TOTAL_BAR_WIDTH));
    const endIdx = Math.min(data.length - 1, Math.ceil((scrollOffset + dimensions.width) / TOTAL_BAR_WIDTH));
    return { startIdx, endIdx };
  }, [scrollOffset, dimensions.width, data.length]);

  const priceRange = useMemo(() => {
    if (data.length === 0) return { min: 0, max: 100 };
    const visibleData = data.slice(visibleRange.startIdx, visibleRange.endIdx + 1);
    if (visibleData.length === 0) return { min: 0, max: 100 };

    let min = Infinity;
    let max = -Infinity;
    visibleData.forEach(d => {
      min = Math.min(min, d.capital_val, d.vwap);
      max = Math.max(max, d.capital_vah, d.vwap);
    });

    const range = max - min;
    const padding = range * 0.15 || 10;
    return { min: min - padding, max: max + padding };
  }, [data, visibleRange]);

  const getY = (price: number) => {
    if (priceRange.max === priceRange.min) return dimensions.height / 2;
    return dimensions.height - ((price - priceRange.min) / (priceRange.max - priceRange.min)) * dimensions.height;
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || dimensions.width === 0 || dimensions.height === 0) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.save();
    ctx.scale(dpr, dpr);

    // Clear and background
    ctx.clearRect(0, 0, dimensions.width, dimensions.height);
    ctx.fillStyle = '#020617';
    ctx.fillRect(0, 0, dimensions.width, dimensions.height);

    // Draw horizontal grid lines
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    const gridCount = 8;
    for (let i = 0; i <= gridCount; i++) {
        const y = (dimensions.height / gridCount) * i;
        ctx.beginPath();
        ctx.moveTo(0, y);
        ctx.lineTo(dimensions.width, y);
        ctx.stroke();

        // Price labels
        const price = priceRange.max - (i / gridCount) * (priceRange.max - priceRange.min);
        ctx.fillStyle = '#64748b';
        ctx.font = '10px Inter, sans-serif';
        ctx.fillText(price.toFixed(2), dimensions.width - 50, y - 5);
    }

    // Global max capital for normalization
    const maxCapital = data.reduce((max, d) => Math.max(max, d.total_capital), 0);

    // Rendering logic
    data.forEach((d, i) => {
      if (i < visibleRange.startIdx || i > visibleRange.endIdx) return;

      const x = i * TOTAL_BAR_WIDTH - scrollOffset + BAR_SPACING / 2 + BAR_WIDTH / 2;

      const yPoc = getY(d.capital_poc);
      const yVah = getY(d.capital_vah);
      const yVal = getY(d.capital_val);
      const vwapY = getY(d.vwap);

      // 1. Value Area Field (Density Field)
      if (showVa && Math.abs(yVal - yVah) > 0.1) {
        const gradient = ctx.createLinearGradient(0, yVah, 0, yVal);
        const pocPos = Math.max(0, Math.min(1, (yPoc - yVah) / (yVal - yVah)));

        gradient.addColorStop(0, 'rgba(59, 130, 246, 0)');
        gradient.addColorStop(pocPos, 'rgba(59, 130, 246, 0.4)');
        gradient.addColorStop(1, 'rgba(59, 130, 246, 0)');

        ctx.fillStyle = gradient;
        ctx.fillRect(x - BAR_WIDTH / 2, yVah, BAR_WIDTH, yVal - yVah);
      }

      // 2. VWAP Effort Vector
      if (showVwap && Math.abs(vwapY - yPoc) > 2) {
        ctx.beginPath();
        ctx.moveTo(x, yPoc);
        ctx.lineTo(x, vwapY);
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.7)';
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Arrow head
        const headSize = 6;
        ctx.beginPath();
        ctx.moveTo(x, vwapY);
        if (vwapY < yPoc) { // Upward effort
          ctx.lineTo(x - headSize / 2, vwapY + headSize);
          ctx.moveTo(x, vwapY);
          ctx.lineTo(x + headSize / 2, vwapY + headSize);
        } else { // Downward effort
          ctx.lineTo(x - headSize / 2, vwapY - headSize);
          ctx.moveTo(x, vwapY);
          ctx.lineTo(x + headSize / 2, vwapY - headSize);
        }
        ctx.stroke();
      }

      // 3. POC Gravity Core
      const radius = 3 + (d.total_capital / maxCapital) * 12;

      ctx.beginPath();
      ctx.arc(x, yPoc, radius, 0, Math.PI * 2);
      ctx.fillStyle = '#f8fafc';
      ctx.shadowBlur = 10;
      ctx.shadowColor = 'rgba(255, 255, 255, 0.5)';
      ctx.fill();
      ctx.shadowBlur = 0; // Reset shadow for next elements

      // 4. Buyer Pressure Halo
      if (showHalo) {
        const buyerRatio = d.buyer_capital / d.total_capital;
        const haloThickness = 2 + buyerRatio * 8;
        let haloColor = 'rgba(148, 163, 184, 0.4)'; // Neutral
        if (buyerRatio > 0.52) {
          haloColor = `rgba(34, 197, 94, ${0.4 + (buyerRatio - 0.5) * 2})`;
        } else if (buyerRatio < 0.48) {
          haloColor = `rgba(239, 68, 68, ${0.4 + (0.5 - buyerRatio) * 2})`;
        }

        ctx.beginPath();
        ctx.arc(x, yPoc, radius + haloThickness / 2 + 2, 0, Math.PI * 2);
        ctx.strokeStyle = haloColor;
        ctx.lineWidth = haloThickness;
        ctx.stroke();
      }
    });

    ctx.restore();
  }, [dimensions, data, scrollOffset, priceRange, visibleRange, showVwap, showHalo, showVa, dpr]);

  const handleMouseDown = (e: React.MouseEvent) => {
    setIsDragging(true);
    setDragStart(e.clientX);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (isDragging) {
      const delta = dragStart - e.clientX;
      setScrollOffset(prev => Math.max(0, Math.min(data.length * TOTAL_BAR_WIDTH - dimensions.width + BAR_SPACING, prev + delta)));
      setDragStart(e.clientX);
      setHoverData(null);
    } else {
      const barIdx = Math.floor((x + scrollOffset) / TOTAL_BAR_WIDTH);
      if (barIdx >= 0 && barIdx < data.length) {
        setHoverData({ d: data[barIdx], x, y });
      } else {
        setHoverData(null);
      }
    }
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  const handleMouseLeave = () => {
    setIsDragging(false);
    setHoverData(null);
  };

  const handleWheel = (e: React.WheelEvent) => {
    const delta = e.deltaX !== 0 ? e.deltaX : e.deltaY;
    setScrollOffset(prev => Math.max(0, Math.min(data.length * TOTAL_BAR_WIDTH - dimensions.width + BAR_SPACING, prev + delta)));
  };

  return (
    <div
      ref={containerRef}
      className="w-full h-full bg-slate-950 relative overflow-hidden cursor-crosshair select-none"
      onMouseDown={handleMouseDown}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseLeave}
      onWheel={handleWheel}
    >
      <canvas
        ref={canvasRef}
        width={dimensions.width * dpr}
        height={dimensions.height * dpr}
        style={{ width: dimensions.width, height: dimensions.height }}
        className="absolute inset-0"
      />

      <div className="absolute top-4 left-4 flex gap-2 z-20">
        <button
          onClick={() => setShowVa(!showVa)}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${showVa ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/20' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'}`}
        >
          Value Area Field
        </button>
        <button
          onClick={() => setShowHalo(!showHalo)}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${showHalo ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/20' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'}`}
        >
          Buyer Pressure Halo
        </button>
        <button
          onClick={() => setShowVwap(!showVwap)}
          className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${showVwap ? 'bg-blue-600 text-white shadow-lg shadow-blue-900/20' : 'bg-slate-800 text-slate-400 hover:bg-slate-700'}`}
        >
          VWAP Effort Vector
        </button>
      </div>

      {hoverData && (
        <div
          className="absolute z-30 p-3 bg-slate-900/95 border border-slate-700 rounded-lg shadow-2xl text-xs text-slate-200 pointer-events-none backdrop-blur-sm"
          style={{
            left: Math.min(hoverData.x + 20, dimensions.width - 180),
            top: Math.min(hoverData.y + 20, dimensions.height - 150)
          }}
        >
          <div className="font-bold text-sm mb-2 border-b border-slate-700 pb-1.5 flex justify-between items-center gap-4">
            <span>{hoverData.d.date}</span>
            <span className="text-[10px] text-slate-500 font-normal">Capital Gravity</span>
          </div>
          <div className="grid grid-cols-2 gap-x-6 gap-y-1.5">
            <span className="text-slate-400">POC:</span> <span className="text-right font-mono font-medium">{hoverData.d.capital_poc.toFixed(2)}</span>
            <span className="text-slate-400">VAH / VAL:</span> <span className="text-right font-mono text-[10px]">{hoverData.d.capital_vah.toFixed(2)} / {hoverData.d.capital_val.toFixed(2)}</span>
            <span className="text-slate-400">VWAP:</span> <span className="text-right font-mono font-medium">{hoverData.d.vwap.toFixed(2)}</span>
            <div className="col-span-2 border-t border-slate-800 my-1"></div>
            <span className="text-slate-400">Total Cap:</span> <span className="text-right font-mono">{(hoverData.d.total_capital / 1000).toFixed(1)}k</span>
            <span className="text-slate-400">Buyer Pressure:</span> <span className={`text-right font-bold ${(hoverData.d.buyer_capital / hoverData.d.total_capital) > 0.52 ? 'text-green-400' : (hoverData.d.buyer_capital / hoverData.d.total_capital) < 0.48 ? 'text-red-400' : 'text-slate-300'}`}>
              {(hoverData.d.buyer_capital / hoverData.d.total_capital * 100).toFixed(1)}%
            </span>
          </div>
        </div>
      )}

      <div className="absolute bottom-4 right-4 text-[10px] text-slate-600 font-medium tracking-wider uppercase pointer-events-none">
        The Capital Gravity Chart
      </div>
    </div>
  );
};

export default CapitalGravityChart;
