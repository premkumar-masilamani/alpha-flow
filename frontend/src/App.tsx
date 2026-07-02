import { useEffect, useState, useRef } from "react";
import axios from "axios";
import Header from "./components/Header";
import Sidebar from "./components/Sidebar";
import Chart from "./components/Chart";
import IndicatorControls from "./components/IndicatorControls";
import {
  type DailyCandleData,
  getCandleData,
  getIndicatorConfigs,
  getIndicatorSeries,
  getTickers,
  type IndicatorConfig,
  type IndicatorSeries,
  getTechnicalAnalysis,
  indicatorKey,
  type Ticker,
  type Timeframe,
  type TechnicalAnalysisData,
  getSupportResistance,
  type SupportResistanceLine,
} from "./services/api";
import {
  Loader2,
  ChevronLeft,
  ChevronRight,
  Info,
  AlertCircle,
} from "lucide-react";

// Percentage change relative to a base price. Returns 0 when the base is zero or
// non-finite, so the UI never renders NaN/Infinity for malformed or zero-open data.
const pctChange = (change: number, base: number): number => {
  if (!base || !Number.isFinite(base)) return 0;
  return (change / base) * 100;
};



function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const [analysisData, setAnalysisData] = useState<TechnicalAnalysisData | null>(null);
  const [analysisError, setAnalysisError] = useState<"stale" | "server" | null>(null);
  const [loading, setLoading] = useState(false);

  // Timeframe and Indicators
  const [timeframe, setTimeframe] = useState<Timeframe>("DAILY");
  const [indicatorConfigs, setIndicatorConfigs] = useState<IndicatorConfig[]>(
    [],
  );
  const [chartIndicators, setChartIndicators] = useState<IndicatorSeries[]>([]);
  const [chartSrLines, setChartSrLines] = useState<SupportResistanceLine[]>([]);
  const [showDailySR, setShowDailySR] = useState(true);
  const [showWeeklySR, setShowWeeklySR] = useState(false);
  const [enabledIndicators, setEnabledIndicators] = useState<Set<string>>(
    new Set(),
  );
  const [chartCandleData, setChartCandleData] = useState<DailyCandleData[]>([]);
  const [loadedSymbol, setLoadedSymbol] = useState<string | null>(null);
  const [loadedTimeframe, setLoadedTimeframe] = useState<Timeframe>("DAILY");
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);
  const [loadingOlder, setLoadingOlder] = useState(false);

  // Layout states
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [activeTab, setActiveTab] = useState<"overview" | "charts">("overview");

  const selectedTickerRef = useRef(selectedTicker);
  const timeframeRef = useRef(timeframe);

  useEffect(() => {
    selectedTickerRef.current = selectedTicker;
  }, [selectedTicker]);

  useEffect(() => {
    timeframeRef.current = timeframe;
  }, [timeframe]);

  const toggleIndicator = (key: string) => {
    setEnabledIndicators((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  };

  useEffect(() => {
    const fetchTickers = async () => {
      try {
        const data = await getTickers();
        setTickers(data);
        if (data.length > 0) {
          const btcUsd = data.find((t) => t.symbol === "BTC-USD");
          setSelectedTicker(btcUsd ? btcUsd.symbol : data[0].symbol);
        }
      } catch (error) {
        console.error("Failed to fetch tickers:", error);
      }
    };
    fetchTickers();
  }, []);

  // Discover the configured indicators once
  useEffect(() => {
    getIndicatorConfigs()
      .then(setIndicatorConfigs)
      .catch((error) =>
        console.error("Failed to fetch indicator configs:", error),
      );
  }, []);

  // Set default enabled indicators when timeframe or configs load/change
  useEffect(() => {
    if (indicatorConfigs.length === 0) return;
    if (timeframe === "WEEKLY") {
      const weeklyMacdConfig = indicatorConfigs.find(
        (c) => c.timeframe === "WEEKLY" && c.type === "MACD",
      );
      if (weeklyMacdConfig) {
        setEnabledIndicators(new Set([indicatorKey(weeklyMacdConfig)]));
      } else {
        setEnabledIndicators(new Set());
      }
    } else {
      const dailyEmaConfigs = indicatorConfigs.filter(
        (c) => c.timeframe === "DAILY" && c.type === "EMA",
      );
      const dailyVolSmaConfig = indicatorConfigs.find(
        (c) =>
          c.timeframe === "DAILY" &&
          c.type === "SMA" &&
          c.source === "VOLUME" &&
          c.params === "period=20",
      );
      const defaultEnabled = dailyEmaConfigs.map(indicatorKey);
      if (dailyVolSmaConfig) {
        defaultEnabled.push(indicatorKey(dailyVolSmaConfig));
      }
      setEnabledIndicators(new Set(defaultEnabled));
    }
  }, [timeframe, indicatorConfigs]);

  // Set SR visibility defaults when timeframe changes
  useEffect(() => {
    if (timeframe === "DAILY") {
      setShowDailySR(true);
      setShowWeeklySR(true);
    } else {
      setShowDailySR(false);
      setShowWeeklySR(true);
    }
  }, [timeframe]);

  // Fetch technical analysis data when selectedTicker changes
  useEffect(() => {
    let active = true;
    const fetchData = async () => {
      if (selectedTicker) {
        setLoading(true);
        setAnalysisData(null);
        setAnalysisError(null);

        try {
          const analysis = await getTechnicalAnalysis(selectedTicker);
          if (active) {
            setAnalysisData(analysis);
          }
        } catch (error) {
          console.error("Failed to fetch technical analysis data:", error);
          if (active) {
            setAnalysisData(null);
            const status = axios.isAxiosError(error) ? error.response?.status : undefined;
            setAnalysisError(status === 404 ? "stale" : "server");
          }
        } finally {
          if (active) {
            setLoading(false);
          }
        }
      }
    };
    fetchData();
    return () => {
      active = false;
    };
  }, [selectedTicker]);

  // Fetch chart candle data and indicator series when selectedTicker or timeframe changes
  useEffect(() => {
    let active = true;
    const fetchChartData = async () => {
      if (!selectedTicker) {
        setChartCandleData([]);
        setChartIndicators([]);
        setLoadedSymbol(null);
        return;
      }
      setLoading(true);
      setChartCandleData([]);
      setChartIndicators([]);
      setChartSrLines([]);
      setLoadedSymbol(null);
      setPage(0);
      setHasMore(true);
      setLoadingOlder(false);
      try {
        let candles, indicators, srLines = [];
        if (timeframe === "DAILY") {
            const res = await Promise.all([
              getCandleData(selectedTicker, timeframe, 0),
              getIndicatorSeries(selectedTicker, timeframe, 0),
              getSupportResistance(selectedTicker)
            ]);
            candles = res[0]; indicators = res[1]; srLines = res[2];
        } else {
            const res = await Promise.all([
              getCandleData(selectedTicker, timeframe, 0),
              getIndicatorSeries(selectedTicker, timeframe, 0),
              getSupportResistance(selectedTicker)
            ]);
            candles = res[0]; indicators = res[1]; srLines = res[2];
        }

        if (active) {
          setChartCandleData(candles);
          setChartIndicators(indicators);
          setChartSrLines(srLines);
          setLoadedSymbol(selectedTicker);
          setLoadedTimeframe(timeframe);
        }
      } catch (error) {
        console.error(
          `Failed to fetch chart data for ${selectedTicker} (${timeframe}):`,
          error,
        );
        if (active) {
          setChartCandleData([]);
          setChartIndicators([]);
          setChartSrLines([]);
        }
      } finally {
        if (active) {
          setLoading(false);
        }
      }
    };
    fetchChartData();
    return () => {
      active = false;
    };
  }, [selectedTicker, timeframe]);

  const handleLoadOlderData = async () => {
    if (loadingOlder || !hasMore || !loadedSymbol) return;
    setLoadingOlder(true);

    const targetTicker = loadedSymbol;
    const targetTimeframe = loadedTimeframe;
    const nextPage = page + 1;

    try {
      const nextCandles = await getCandleData(
        targetTicker,
        targetTimeframe,
        nextPage,
      );
      if (
        selectedTickerRef.current !== targetTicker ||
        timeframeRef.current !== targetTimeframe
      ) {
        return; // Discard stale request
      }

      if (nextCandles.length === 0) {
        setHasMore(false);
        setLoadingOlder(false);
        return;
      }

      const nextIndicators = await getIndicatorSeries(
        targetTicker,
        targetTimeframe,
        nextPage,
      );
      if (
        selectedTickerRef.current !== targetTicker ||
        timeframeRef.current !== targetTimeframe
      ) {
        return; // Discard stale request
      }

      setChartCandleData((prev) => {
        const merged = [...nextCandles, ...prev];
        const unique = Array.from(
          new Map(merged.map((item) => [item.date, item])).values(),
        );
        return unique.sort((a, b) => a.date.localeCompare(b.date));
      });

      setChartIndicators((prev) => {
        return prev.map((oldSeries) => {
          const newSeries = nextIndicators.find(
            (ns) =>
              ns.type === oldSeries.type &&
              ns.source === oldSeries.source &&
              ns.params === oldSeries.params,
          );
          if (!newSeries) return oldSeries;
          const mergedPoints = [...newSeries.points, ...oldSeries.points];
          const uniquePoints = Array.from(
            new Map(mergedPoints.map((p) => [p.date, p])).values(),
          );
          return {
            ...oldSeries,
            points: uniquePoints.sort((a, b) => a.date.localeCompare(b.date)),
          };
        });
      });

      setPage(nextPage);
    } catch (error) {
      console.error("Failed to fetch older data:", error);
    } finally {
      if (
        selectedTickerRef.current === targetTicker &&
        timeframeRef.current === targetTimeframe
      ) {
        setLoadingOlder(false);
      }
    }
  };

  const formatLabel = (label: string): string => {
    return label.replace(/^([A-Za-z]+)\((.*)\)$/, '$1 ($2)');
  };

  const renderOverview = () => {
    if (analysisError === "stale") {
      return (
        <div className="flex-1 flex flex-col items-center justify-center p-8">
          <div className="bg-amber-900/20 border border-amber-500/30 rounded-xl p-6 max-w-md w-full text-center">
            <AlertCircle className="text-amber-500 mx-auto mb-4" size={48} />
            <h3 className="text-lg font-bold text-white">Technical Analysis Not Yet Complete</h3>
            <p className="text-amber-200/70 mt-2 text-sm leading-relaxed">
              Technical analysis data has not yet been computed or is currently out-of-date for <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>.
            </p>
          </div>
        </div>
      );
    }

    if (analysisError === "server") {
      return (
        <div className="flex-1 flex flex-col items-center justify-center p-6 bg-slate-950 text-center">
          <div className="max-w-md p-6 bg-slate-900 border border-rose-900/50 rounded-xl shadow-xl space-y-4">
            <div className="mx-auto flex items-center justify-center w-12 h-12 rounded-full bg-rose-500/10 text-rose-400">
              <Info size={24} />
            </div>
            <h3 className="text-lg font-bold text-white">Service Unavailable</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Could not reach the backend service for <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>.
            </p>
          </div>
        </div>
      );
    }

    if (!analysisData || !analysisData.candle) {
      return (
        <div className="flex-1 flex flex-col items-center justify-center text-slate-500 gap-3">
          <Loader2 className="animate-spin text-blue-500" size={32} />
          <p className="text-sm font-semibold text-slate-400">
            Fetching technical analysis...
          </p>
        </div>
      );
    }

    const { candle, dailyIndicators, weeklyIndicators, srLines } = analysisData;
    const priceChange = candle.close - candle.open;
    const priceChangePct = pctChange(priceChange, candle.open);

    const renderIndicatorTable = (title: string, date: string, indicators: IndicatorSeries[]) => (
      <div className="bg-slate-900/40 border border-slate-800 rounded-xl overflow-hidden shadow-xl backdrop-blur mb-6">
        <div className="px-6 py-4 bg-slate-900/80 border-b border-slate-800 flex justify-between items-center">
          <h2 className="text-base font-bold text-white">{title}</h2>
          <span className="text-xs px-2.5 py-1 rounded-md bg-slate-800/80 border border-slate-700 font-semibold text-slate-300 font-mono">
            {date || 'N/A'}
          </span>
        </div>
        <div className="overflow-x-auto">
          {indicators.length === 0 ? (
            <div className="p-6 text-center text-slate-500 text-sm italic">
              No indicators available for this timeframe.
            </div>
          ) : (
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-950 text-slate-400 font-bold border-b border-slate-800">
                  <th className="p-4 w-1/3">Indicator</th>
                  <th className="p-4 w-2/3">Values</th>
                </tr>
              </thead>
              <tbody>
                {indicators.map((ind, idx) => {
                  const pt = ind.points.length > 0 ? ind.points[0] : null;
                  return (
                    <tr key={`${ind.type}-${ind.params}-${idx}`} className="border-b border-slate-800/40 hover:bg-slate-900/20 transition-colors">
                      <td className="p-4 font-bold text-slate-300">
                        {formatLabel(ind.label || `${ind.type} (${ind.params})`)}
                      </td>
                      <td className="p-4 text-slate-400 font-mono">
                        {pt ? (
                          <div className="flex flex-wrap gap-4">
                            {Object.entries(pt.values).map(([k, v]) => (
                              <div key={k} className="flex gap-2 items-center">
                                <span className="text-[10px] text-slate-500 uppercase tracking-wider">{k}:</span>
                                <span className="text-slate-200">
                                  {Number(v).toLocaleString(undefined, { maximumFractionDigits: 4 })}
                                </span>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <span className="text-slate-600 italic">No data</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      </div>
    );

    const dailyDate = dailyIndicators[0]?.points[0]?.date || candle.date;
    const weeklyDate = weeklyIndicators[0]?.points[0]?.date || '';

    const renderSRTable = () => {
      if (!srLines || srLines.length === 0) return null;

      const closePrice = candle.close;

      const valid = [...srLines].filter(sr => sr.currentPrice !== undefined && sr.currentPrice !== null);

      // 3 closest lines strictly above close price (sorted ascending by distance = price desc)
      const above = valid
          .filter(sr => sr.currentPrice > closePrice)
          .sort((a, b) => a.currentPrice - b.currentPrice)  // closest first = lowest price first
          .slice(0, 3)
          .sort((a, b) => b.currentPrice - a.currentPrice); // display: highest on top

      // 3 closest lines strictly below close price (sorted ascending by distance = price desc)
      const below = valid
          .filter(sr => sr.currentPrice < closePrice)
          .sort((a, b) => b.currentPrice - a.currentPrice)  // closest first = highest price first
          .slice(0, 3);

      if (above.length === 0 && below.length === 0) return null;

      // Build rows: above rows, then close price sentinel, then below rows
      type SRRow = { kind: 'sr'; sr: typeof valid[0] } | { kind: 'close' };
      const rows: SRRow[] = [
          ...above.map(sr => ({ kind: 'sr' as const, sr })),
          { kind: 'close' as const },
          ...below.map(sr => ({ kind: 'sr' as const, sr })),
      ];


      return (
        <div className="bg-slate-900/40 border border-slate-800 rounded-xl overflow-hidden shadow-xl backdrop-blur mb-6">
          <div className="px-6 py-4 bg-slate-900/80 border-b border-slate-800 flex justify-between items-center">
            <h2 className="text-base font-bold text-white">Support &amp; Resistance</h2>
            <span className="text-xs px-2.5 py-1 rounded-md bg-slate-800/80 border border-slate-700 font-semibold text-slate-300 font-mono">
              {candle.date}
            </span>
          </div>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-950 text-slate-400 font-bold border-b border-slate-800">
                  <th className="p-4 w-1/4">Type</th>
                  <th className="p-4 w-1/4">Current Price</th>
                  <th className="p-4 w-1/4">% Away</th>
                  <th className="p-4 w-1/4">Timeframe</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {rows.map((row, idx) => {
                  if (row.kind === 'close') {
                    return (
                      <tr key="close-price" className="bg-slate-800/60 border-y border-slate-600">
                        <td className="p-4 font-bold text-slate-300 text-xs uppercase tracking-widest">Close Price</td>
                        <td className="p-4 text-white font-mono font-bold">{closePrice.toFixed(2)}</td>
                        <td className="p-4 font-mono text-slate-500">0.00%</td>
                        <td className="p-4"></td>
                      </tr>
                    );
                  }
                  const { sr } = row;
                  const pctAway = ((sr.currentPrice - closePrice) / closePrice) * 100;
                  const isAbove = pctAway >= 0;
                  return (
                    <tr key={`sr-${idx}`} className="border-b border-slate-800/40 hover:bg-slate-900/20 transition-colors">
                      <td className="p-4 font-bold">
                        <span className={sr.currentType === 'SUPPORT' ? 'text-emerald-400' : 'text-rose-400'}>
                          {sr.currentType}
                        </span>
                      </td>
                      <td className="p-4 text-slate-300 font-mono">
                        {sr.currentPrice.toFixed(2)}
                      </td>
                      <td className="p-4 font-mono font-semibold">
                        <span className={isAbove ? 'text-emerald-400' : 'text-rose-400'}>
                          {isAbove ? '+' : ''}{pctAway.toFixed(2)}%
                        </span>
                      </td>
                      <td className="p-4">
                        <span className={`text-xs px-2 py-1 rounded font-bold tracking-wider ${
                          sr.timeframe === 'DAILY' ? 'bg-blue-500/10 text-blue-400 border border-blue-500/20' : 'bg-purple-500/10 text-purple-400 border border-purple-500/20'
                        }`}>
                          {sr.timeframe}
                        </span>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      );
    };


    return (
      <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-950 text-slate-200">
        {/* Quick Stats Card */}
        <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-6 shadow-xl backdrop-blur">
          <div className="flex justify-between items-center mb-6">
            <div className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
              OHLCV Technical Analysis
            </div>
            <div className="text-xs px-2.5 py-1 rounded-md bg-slate-800 border border-slate-700 font-semibold text-slate-300 font-mono">
              {candle.date}
            </div>
          </div>
          <div className="flex items-baseline gap-2 mb-6">
            <span className="text-4xl font-extrabold text-white">
              {Number(candle.close).toLocaleString(undefined, {
                minimumFractionDigits: 2,
                maximumFractionDigits: 4,
              })}
            </span>
            <span
              className={`text-sm font-bold flex items-center px-2 py-0.5 rounded-full ${
                priceChange >= 0
                  ? "bg-emerald-500/10 text-emerald-400"
                  : "bg-rose-500/10 text-rose-400"
              }`}
            >
              {priceChange >= 0 ? "+" : ""}
              {priceChangePct.toFixed(2)}%
            </span>
          </div>
          <div className="border-t border-slate-800/80 pt-4 grid grid-cols-2 md:grid-cols-5 gap-4 text-sm">
            <div>
              <span className="text-slate-500 block text-xs uppercase tracking-wider mb-1">Open</span>
              <span className="text-slate-300 font-bold font-mono">
                {Number(candle.open).toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-slate-500 block text-xs uppercase tracking-wider mb-1">High</span>
              <span className="text-slate-300 font-bold font-mono">
                {Number(candle.high).toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-slate-500 block text-xs uppercase tracking-wider mb-1">Low</span>
              <span className="text-slate-300 font-bold font-mono">
                {Number(candle.low).toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-slate-500 block text-xs uppercase tracking-wider mb-1">Close</span>
              <span className="text-slate-300 font-bold font-mono">
                {Number(candle.close).toFixed(2)}
              </span>
            </div>
            <div>
              <span className="text-slate-500 block text-xs uppercase tracking-wider mb-1">Volume</span>
              <span className="text-slate-300 font-bold font-mono">
                {Number(candle.vol).toLocaleString()}
              </span>
            </div>
          </div>
        </div>

        {/* SR Lines Table */}
        {renderSRTable()}

        {/* Indicators Tables */}
        {renderIndicatorTable("Daily Indicators", dailyDate, dailyIndicators)}
        {renderIndicatorTable("Weekly Indicators", weeklyDate, weeklyIndicators)}
      </div>
    );
  };

  const renderContent = () => {
    if (!selectedTicker) {
      return (
        <div className="flex-1 flex items-center justify-center text-slate-500 italic">
          Select a ticker to view analysis
        </div>
      );
    }

    return (
      <>
        {/* Ticker Header & Tab bar */}
        <div className="p-4 border-b border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            {/* Sidebar Toggle */}
            <button
              onClick={() => setSidebarOpen(!sidebarOpen)}
              className="p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
              title={sidebarOpen ? "Collapse Left Menu" : "Expand Left Menu"}
            >
              {sidebarOpen ? (
                <ChevronLeft size={20} />
              ) : (
                <ChevronRight size={20} />
              )}
            </button>
            <div>
              <h2 className="text-2xl font-bold text-white">
                {selectedTicker}
              </h2>
              <p className="text-slate-400 text-sm">
                {tickers.find((t) => t.symbol === selectedTicker)?.name}
              </p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {/* Tab Navigation */}
            <div className="flex bg-slate-900 border border-slate-800 p-0.5 rounded-lg">
              <button
                onClick={() => setActiveTab("overview")}
                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${
                  activeTab === "overview"
                    ? "bg-blue-600 text-white shadow-md"
                    : "text-slate-400 hover:text-slate-200"
                }`}
              >
                Technical Analysis
              </button>
              <button
                onClick={() => setActiveTab("charts")}
                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${
                  activeTab === "charts"
                    ? "bg-blue-600 text-white shadow-md"
                    : "text-slate-400 hover:text-slate-200"
                }`}
              >
                Technical Chart
              </button>
            </div>
          </div>

          <div className="flex items-center gap-4">
            {loading && <Loader2 className="animate-spin text-blue-500" />}
          </div>
        </div>

        {/* Dashboard Tab Content */}
        {activeTab === "overview" ? (
          analysisData ? (
            renderOverview()
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500">
              {loading
                ? "Loading technical analysis..."
                : "No data available for this ticker"}
            </div>
          )
        ) : (
          <div className="flex-1 flex flex-col gap-4 p-4 overflow-hidden">
            {selectedTicker && (
              <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-3 shadow-lg backdrop-blur flex flex-col gap-3">
                <div className="flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
                  <IndicatorControls
                    configs={indicatorConfigs.filter(
                      (c) => c.timeframe === timeframe,
                    )}
                    enabled={enabledIndicators}
                    onToggle={toggleIndicator}
                  />
                  <div className="flex bg-slate-950 border border-slate-800 p-0.5 rounded-lg self-start sm:self-auto">
                    <button
                      onClick={() => setTimeframe("DAILY")}
                      className={`px-3 py-1.5 text-xs font-bold rounded-md transition-all ${
                        timeframe === "DAILY"
                          ? "bg-blue-600 text-white shadow-sm"
                          : "text-slate-400 hover:text-slate-200"
                      }`}
                    >
                      Daily
                    </button>
                    <button
                      onClick={() => setTimeframe("WEEKLY")}
                      className={`px-3 py-1.5 text-xs font-bold rounded-md transition-all ${
                        timeframe === "WEEKLY"
                          ? "bg-blue-600 text-white shadow-sm"
                          : "text-slate-400 hover:text-slate-200"
                      }`}
                    >
                      Weekly
                    </button>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 border-t border-slate-800 pt-2">
                  <span className="text-[10px] font-bold uppercase tracking-widest text-slate-500 mr-1">S/R Lines</span>
                  {timeframe === "DAILY" && (
                      <button
                        onClick={() => setShowDailySR(!showDailySR)}
                        className={`px-2.5 py-1 text-xs font-semibold rounded-md border transition-colors ${
                          showDailySR
                            ? "bg-blue-600 border-blue-500 text-white"
                            : "bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200 hover:border-slate-600"
                        }`}
                      >
                        Daily
                      </button>
                  )}
                  <button
                    onClick={() => setShowWeeklySR(!showWeeklySR)}
                    className={`px-2.5 py-1 text-xs font-semibold rounded-md border transition-colors ${
                      showWeeklySR
                        ? "bg-blue-600 border-blue-500 text-white"
                        : "bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200 hover:border-slate-600"
                    }`}
                  >
                    Weekly
                  </button>
                </div>
              </div>
            )}
            <div className="flex-1 relative min-h-0 bg-slate-950 border border-slate-800 rounded-lg overflow-hidden">
              {chartCandleData.length > 0 && loadedSymbol ? (
                <Chart
                  data={chartCandleData}
                  indicators={chartIndicators}
                  enabled={enabledIndicators}
                  configs={indicatorConfigs}
                  srLines={chartSrLines.filter(sr => {
                    if (loadedTimeframe === 'WEEKLY') return sr.timeframe === 'WEEKLY' && showWeeklySR;
                    if (loadedTimeframe === 'DAILY') {
                        if (sr.timeframe === 'DAILY' && showDailySR) return true;
                        if (sr.timeframe === 'WEEKLY' && showWeeklySR) return true;
                    }
                    return false;
                  })}
                  showSR={true}
                  symbol={loadedSymbol}
                  timeframe={loadedTimeframe}
                  onLoadOlderData={handleLoadOlderData}
                />
              ) : (
                <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-500 gap-3">
                  {loading ? (
                    <>
                      <Loader2 className="animate-spin text-blue-500" size={32} />
                      <p className="text-sm font-semibold text-slate-400">Loading chart data...</p>
                    </>
                  ) : (
                    "No data available for this ticker"
                  )}
                </div>
              )}
            </div>
          </div>
        )}
      </>
    );
  };

  return (
    <div className="flex flex-col h-screen bg-slate-900 overflow-hidden">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        {sidebarOpen && (
          <Sidebar
            tickers={tickers}
            selectedTicker={selectedTicker}
            onSelectTicker={setSelectedTicker}
          />
        )}
        <main className="flex-1 flex flex-col bg-slate-950 overflow-hidden">
          {renderContent()}
        </main>
      </div>
    </div>
  );
}

export default App;
