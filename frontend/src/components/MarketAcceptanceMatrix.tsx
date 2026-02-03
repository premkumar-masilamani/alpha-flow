import React, {useEffect, useMemo, useRef, useState} from 'react';
import type {MarketData} from '../services/api';

interface MarketAcceptanceMatrixProps {
    data: MarketData[];
}

const BAR_WIDTH = 40;
const BAR_SPACING = 20;
const TOTAL_BAR_WIDTH = BAR_WIDTH + BAR_SPACING;

const MarketAcceptanceMatrix: React.FC<MarketAcceptanceMatrixProps> = ({data}) => {
    const containerRef = useRef<HTMLDivElement>(null);
    const canvasRef = useRef<HTMLCanvasElement>(null);
    const dpr = typeof window !== 'undefined' ? window.devicePixelRatio || 1 : 1;
    const [scrollOffset, setScrollOffset] = useState(0);
    const [dimensions, setDimensions] = useState({width: 0, height: 0});
    const [isDragging, setIsDragging] = useState(false);
    const [dragStart, setDragStart] = useState(0);
    const [hoverData, setHoverData] = useState<{ d: MarketData, x: number, y: number } | null>(null);

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
    }, [dimensions.width, data.length, scrollOffset]);

    const visibleRange = useMemo(() => {
        const startIdx = Math.max(0, Math.floor(scrollOffset / TOTAL_BAR_WIDTH));
        const endIdx = Math.min(data.length - 1, Math.ceil((scrollOffset + dimensions.width) / TOTAL_BAR_WIDTH));
        return {startIdx, endIdx};
    }, [scrollOffset, dimensions.width, data.length]);

    const priceRange = useMemo(() => {
        if (data.length === 0) return {min: 0, max: 100};
        const visibleData = data.slice(visibleRange.startIdx, visibleRange.endIdx + 1);
        if (visibleData.length === 0) return {min: 0, max: 100};

        let min = Infinity;
        let max = -Infinity;
        visibleData.forEach(d => {
            min = Math.min(min, d.low);
            max = Math.max(max, d.high);
        });

        const range = max - min;
        const padding = range * 0.1 || 10;
        return {min: min - padding, max: max + padding};
    }, [data, visibleRange]);

    const getY = (price: number) => {
        if (priceRange.max === priceRange.min) return dimensions.height / 2;
        // Leave some space at the bottom for the pressure indicator
        const chartHeight = dimensions.height - 40;
        return chartHeight - ((price - priceRange.min) / (priceRange.max - priceRange.min)) * chartHeight + 10;
    };

    const maxTotalCapital = useMemo(() => {
        return data.reduce((max, d) => Math.max(max, d.total_capital), 0);
    }, [data]);

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

        // Draw grid
        ctx.strokeStyle = '#1e293b';
        ctx.lineWidth = 1;
        const gridCount = 8;
        for (let i = 0; i <= gridCount; i++) {
            const y = getY(priceRange.min + (i / gridCount) * (priceRange.max - priceRange.min));
            ctx.beginPath();
            ctx.moveTo(0, y);
            ctx.lineTo(dimensions.width, y);
            ctx.stroke();

            const price = priceRange.min + (i / gridCount) * (priceRange.max - priceRange.min);
            ctx.fillStyle = '#64748b';
            ctx.font = '10px Inter, sans-serif';
            ctx.fillText(price.toFixed(2), dimensions.width - 50, y - 5);
        }

        data.forEach((d, i) => {
            if (i < visibleRange.startIdx || i > visibleRange.endIdx) return;

            const x = i * TOTAL_BAR_WIDTH - scrollOffset + BAR_SPACING / 2 + BAR_WIDTH / 2;

            const yHigh = getY(d.high);
            const yLow = getY(d.low);
            const yVah = getY(d.capital_vah);
            const yVal = getY(d.capital_val);
            const yPoc = getY(d.capital_poc);
            const yVwap = getY(d.vwap);

            // 1. Price Exploration Spine
            ctx.strokeStyle = '#475569';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(x, yLow);
            ctx.lineTo(x, yHigh);
            ctx.stroke();

            // 2. Acceptance Zone
            const opacity = (d.total_capital / maxTotalCapital) * 0.6 + 0.1;
            ctx.fillStyle = `rgba(59, 130, 246, ${opacity})`;
            ctx.fillRect(x - BAR_WIDTH / 2, yVah, BAR_WIDTH, yVal - yVah);

            // Horizontal tick at capital_poc
            ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)';
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(x - BAR_WIDTH / 2, yPoc);
            ctx.lineTo(x + BAR_WIDTH / 2, yPoc);
            ctx.stroke();

            // 3. Effort Marker
            const isInsideVa = d.vwap >= d.capital_val && d.vwap <= d.capital_vah;
            if (isInsideVa) {
                ctx.strokeStyle = 'rgba(255, 255, 255, 0.4)';
            } else if (d.vwap > d.capital_vah) {
                ctx.strokeStyle = '#22c55e'; // Green
            } else {
                ctx.strokeStyle = '#ef4444'; // Red
            }
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.moveTo(x - BAR_WIDTH / 4, yVwap);
            ctx.lineTo(x + BAR_WIDTH / 4, yVwap);
            ctx.stroke();

            // 4. Pressure Indicator
            const buyerRatio = d.buyer_capital / d.total_capital;
            const indicatorWidth = buyerRatio * BAR_WIDTH;
            ctx.fillStyle = buyerRatio > 0.5 ? '#22c55e' : '#ef4444';
            // Place it at the bottom of the chart
            const yPressure = dimensions.height - 15;
            ctx.fillRect(x - indicatorWidth / 2, yPressure, indicatorWidth, 4);
        });

        ctx.restore();
    }, [dimensions, data, scrollOffset, priceRange, visibleRange, dpr, maxTotalCapital]);

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
                setHoverData({d: data[barIdx], x, y});
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
                style={{width: dimensions.width, height: dimensions.height}}
                className="absolute inset-0"
            />

            {hoverData && (
                <div
                    className="absolute z-30 p-3 bg-slate-900/95 border border-slate-700 rounded-lg shadow-2xl text-xs text-slate-200 pointer-events-none backdrop-blur-sm"
                    style={{
                        left: Math.min(hoverData.x + 20, dimensions.width - 200),
                        top: Math.min(hoverData.y + 20, dimensions.height - 180)
                    }}
                >
                    <div className="font-bold text-sm mb-2 border-b border-slate-700 pb-1.5 flex justify-between items-center gap-4">
                        <span>{hoverData.d.date}</span>
                        <span className="text-[10px] text-slate-500 font-normal">Market Acceptance Matrix</span>
                    </div>
                    <div className="grid grid-cols-2 gap-x-6 gap-y-1.5">
                        <span className="text-slate-400">VA Range:</span>
                        <span className="text-right font-mono text-[10px]">{hoverData.d.capital_vah.toFixed(2)} - {hoverData.d.capital_val.toFixed(2)}</span>

                        <span className="text-slate-400">POC:</span>
                        <span className="text-right font-mono font-medium">{hoverData.d.capital_poc.toFixed(2)}</span>

                        <span className="text-slate-400">VWAP:</span>
                        <span className="text-right font-mono font-medium">{hoverData.d.vwap.toFixed(2)}</span>

                        <div className="col-span-2 border-t border-slate-800 my-1"></div>

                        <span className="text-slate-400">Buyer %:</span>
                        <span className={`text-right font-bold ${(hoverData.d.buyer_capital / hoverData.d.total_capital) > 0.5 ? 'text-green-400' : 'text-red-400'}`}>
              {(hoverData.d.buyer_capital / hoverData.d.total_capital * 100).toFixed(1)}%
            </span>

                        <span className="text-slate-400">Total Capital:</span>
                        <span className="text-right font-mono">{(hoverData.d.total_capital / 1000).toFixed(1)}k</span>
                    </div>
                </div>
            )}

            <div className="absolute bottom-4 right-4 text-[10px] text-slate-600 font-medium tracking-wider uppercase pointer-events-none">
                The Market Acceptance Matrix
            </div>
        </div>
    );
};

export default MarketAcceptanceMatrix;
