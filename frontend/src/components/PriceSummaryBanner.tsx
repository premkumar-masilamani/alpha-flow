import React from 'react';
import { TrendingUp, TrendingDown, ChevronLeft, ChevronRight, Loader2 } from 'lucide-react';

export interface PriceSummaryBannerTraversal {
  canGoPrev: boolean;
  canGoNext: boolean;
  onPrev: () => void;
  onNext: () => void;
  loadingOlder?: boolean;
  isAtBoundary?: boolean;
}

export interface PriceSummaryBannerProps {
  ltp: number | null;
  change?: number | null;
  changePct?: number | null;
  changeSubtitle?: string;
  open: number | null;
  high: number | null;
  low: number | null;
  close: number | null;
  volume?: number | null;
  date?: string | null;
  traversal?: PriceSummaryBannerTraversal;
}

export const PriceSummaryBanner: React.FC<PriceSummaryBannerProps> = ({
  ltp,
  change = null,
  changePct = null,
  changeSubtitle,
  open,
  high,
  low,
  close,
  volume,
  date,
  traversal,
}) => {
  const hasValidChange =
    change !== null &&
    change !== undefined &&
    changePct !== null &&
    changePct !== undefined &&
    !isNaN(change) &&
    !isNaN(changePct);

  const isPositive = (change ?? 0) >= 0;

  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-4 shadow-lg backdrop-blur flex flex-wrap items-center justify-between gap-4">
      <div className="flex items-center gap-6">
        <div>
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Last Traded Price
          </div>
          <div className="text-2xl font-bold font-mono text-white mt-0.5">
            {ltp !== null && ltp !== undefined
              ? Number(ltp).toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })
              : "—"}
          </div>
        </div>
        <div>
          <div className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
            Change{" "}
            {changeSubtitle && (
              <span className="text-[10px] text-slate-500 font-normal lowercase">
                {changeSubtitle}
              </span>
            )}
          </div>
          {hasValidChange ? (
            <div
              className={`text-base font-bold font-mono flex items-center gap-1 mt-0.5 ${
                isPositive ? "text-emerald-400" : "text-rose-400"
              }`}
            >
              {isPositive ? <TrendingUp size={16} /> : <TrendingDown size={16} />}
              <span>
                {isPositive ? "+" : ""}
                {change!.toFixed(2)} ({isPositive ? "+" : ""}
                {changePct!.toFixed(2)}%)
              </span>
            </div>
          ) : (
            <div className="text-base font-bold font-mono text-slate-400 mt-0.5">—</div>
          )}
        </div>
      </div>

      <div className="flex flex-wrap items-center gap-4 text-xs font-mono">
        <div className="bg-slate-800/80 px-3 py-1.5 rounded border border-slate-700">
          <span className="text-slate-400 mr-1.5">Open:</span>
          <span className="text-slate-200 font-semibold">
            {open !== null && open !== undefined ? Number(open).toFixed(2) : "—"}
          </span>
        </div>
        <div className="bg-slate-800/80 px-3 py-1.5 rounded border border-slate-700">
          <span className="text-slate-400 mr-1.5">High:</span>
          <span className="text-slate-200 font-semibold">
            {high !== null && high !== undefined ? Number(high).toFixed(2) : "—"}
          </span>
        </div>
        <div className="bg-slate-800/80 px-3 py-1.5 rounded border border-slate-700">
          <span className="text-slate-400 mr-1.5">Low:</span>
          <span className="text-slate-200 font-semibold">
            {low !== null && low !== undefined ? Number(low).toFixed(2) : "—"}
          </span>
        </div>
        <div className="bg-slate-800/80 px-3 py-1.5 rounded border border-slate-700">
          <span className="text-slate-400 mr-1.5">Close:</span>
          <span className="text-slate-200 font-semibold">
            {close !== null && close !== undefined ? Number(close).toFixed(2) : "—"}
          </span>
        </div>
        {volume !== undefined && (
          <div className="bg-slate-800/80 px-3 py-1.5 rounded border border-slate-700">
            <span className="text-slate-400 mr-1.5">Vol:</span>
            <span className="text-slate-200 font-semibold">
              {volume !== null ? Number(volume).toLocaleString() : "—"}
            </span>
          </div>
        )}
        {date && date !== "N/A" && (
          traversal ? (
            <div className="flex items-center rounded bg-blue-900/40 border border-blue-800/50 text-blue-400">
              <button
                onClick={traversal.onPrev}
                disabled={!traversal.canGoPrev || (traversal.isAtBoundary && traversal.loadingOlder)}
                className="px-1.5 py-1 hover:text-white hover:bg-blue-800/40 disabled:opacity-25 disabled:cursor-not-allowed transition-colors rounded-l"
                title="Previous day"
                aria-label="Previous day"
              >
                {traversal.loadingOlder && traversal.isAtBoundary ? (
                  <Loader2 size={14} className="animate-spin" />
                ) : (
                  <ChevronLeft size={14} />
                )}
              </button>
              <span className="px-2.5 py-1 text-xs font-bold uppercase select-none border-x border-blue-800/40 font-mono">
                {date}
              </span>
              <button
                onClick={traversal.onNext}
                disabled={!traversal.canGoNext || traversal.loadingOlder}
                className="px-1.5 py-1 hover:text-white hover:bg-blue-800/40 disabled:opacity-25 disabled:cursor-not-allowed transition-colors rounded-r"
                title="Next day"
                aria-label="Next day"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          ) : (
            <span className="px-2.5 py-1 text-xs font-bold uppercase select-none rounded bg-blue-900/40 border border-blue-800/50 text-blue-300 font-mono">
              {date}
            </span>
          )
        )}
      </div>
    </div>
  );
};

export default PriceSummaryBanner;
