import { useEffect, useState, useRef, Fragment } from "react";
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
  indicatorKey,
  type Ticker,
  type Timeframe,
  type AnalysisResponse,
  getTechnicalAnalysis,
} from "./services/api";
import {
  Loader2,
  ChevronLeft,
  ChevronRight,
  TrendingUp,
  TrendingDown,
  Info,
} from "lucide-react";

// Percentage change relative to a base price. Returns 0 when the base is zero or
// non-finite, so the UI never renders NaN/Infinity for malformed or zero-open data.
const pctChange = (change: number, base: number): number => {
  if (!base || !Number.isFinite(base)) return 0;
  return (change / base) * 100;
};

interface PatternResult {
  signal: "BUY" | "SELL" | "HOLD";
  value: string;
}

const evaluateCandlestickPattern = (
  candles: DailyCandleData[],
): PatternResult => {
  if (candles.length < 3)
    return { signal: "HOLD", value: "Insufficient price data" };

  const c0 = candles[0]; // Latest (candles are sorted desc in sortedDailyDesc)
  const c1 = candles[1]; // Previous
  const c2 = candles[2]; // 2-bars ago

  const body0 = Math.abs(c0.close - c0.open);
  const range0 = c0.high - c0.low;
  const isGreen0 = c0.close > c0.open;
  const isRed0 = c0.close < c0.open;

  const body1 = Math.abs(c1.close - c1.open);
  const range1 = c1.high - c1.low;
  const isGreen1 = c1.close > c1.open;
  const isRed1 = c1.close < c1.open;

  const lowerShadow0 = Math.min(c0.open, c0.close) - c0.low;
  const upperShadow0 = c0.high - Math.max(c0.open, c0.close);

  const isHammer =
    lowerShadow0 > body0 * 2 && upperShadow0 < range0 * 0.15 && range0 > 0;
  const isInvertedHammer =
    upperShadow0 > body0 * 2 && lowerShadow0 < range0 * 0.15 && range0 > 0;

  const isBullishEngulf =
    isRed1 && isGreen0 && c0.open <= c1.close && c0.close >= c1.open;
  const isBearishEngulf =
    isGreen1 && isRed0 && c0.open >= c1.close && c0.close <= c1.open;

  const isBullishMarubozu = isGreen0 && range0 > 0 && body0 >= range0 * 0.95;
  const isBearishMarubozu = isRed0 && range0 > 0 && body0 >= range0 * 0.95;

  const c2Median = (c2.low + c2.high) / 2;
  const isMorningStar =
    c2.close < c2.open &&
    range1 > 0 &&
    body1 <= range1 * 0.3 &&
    isGreen0 &&
    c0.close > c2Median;
  const isEveningStar =
    c2.close > c2.open &&
    range1 > 0 &&
    body1 <= range1 * 0.3 &&
    isRed0 &&
    c0.close < c2Median;

  if (isMorningStar) return { signal: "BUY", value: "Morning Star" };
  if (isEveningStar) return { signal: "SELL", value: "Evening Star" };
  if (isBullishEngulf) return { signal: "BUY", value: "Bullish Engulfing" };
  if (isBearishEngulf) return { signal: "SELL", value: "Bearish Engulfing" };
  if (isHammer)
    return { signal: "BUY", value: "Hammer (Strong Buy at Bottom)" };
  if (isInvertedHammer)
    return { signal: "SELL", value: "Inverted Hammer (Strong Sell at Top)" };
  if (isBullishMarubozu)
    return { signal: "BUY", value: "Bullish Marubozu (Full Green)" };
  if (isBearishMarubozu)
    return { signal: "SELL", value: "Bearish Marubozu (Full Red)" };
  if (isGreen0) return { signal: "BUY", value: "Bullish Green Candle" };
  if (isRed0) return { signal: "SELL", value: "Bearish Red Candle" };

  return { signal: "HOLD", value: "Neutral" };
};

function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const [dailyCandleData, setDailyCandleData] = useState<DailyCandleData[]>([]);
  const [analysisData, setAnalysisData] = useState<AnalysisResponse | null>(
    null,
  );
  const [analysisError, setAnalysisError] = useState<"stale" | "server" | null>(null);
  const [loading, setLoading] = useState(false);

  // Timeframe and Indicators
  const [timeframe, setTimeframe] = useState<Timeframe>("DAILY");
  const [indicatorConfigs, setIndicatorConfigs] = useState<IndicatorConfig[]>(
    [],
  );
  const [chartIndicators, setChartIndicators] = useState<IndicatorSeries[]>([]);
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

  // Fetch daily candle data and technical analysis when selectedTicker changes
  useEffect(() => {
    let active = true;
    const fetchData = async () => {
      if (selectedTicker) {
        setLoading(true);
        setAnalysisData(null);
        setAnalysisError(null);
        setDailyCandleData([]);
        
        // Fetch daily candle data independently
        try {
          const candles = await getCandleData(selectedTicker);
          if (active) {
            setDailyCandleData(candles);
          }
        } catch (error) {
          console.error("Failed to fetch daily candles:", error);
          if (active) {
            setDailyCandleData([]);
          }
        }

        // Fetch technical analysis independently
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
        }
        if (active) {
          setLoading(false);
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
      setLoadedSymbol(null);
      setPage(0);
      setHasMore(true);
      setLoadingOlder(false);
      try {
        const [candles, indicators] = await Promise.all([
          getCandleData(selectedTicker, timeframe, 0),
          getIndicatorSeries(selectedTicker, timeframe, 0),
        ]);
        if (active) {
          setChartCandleData(candles);
          setChartIndicators(indicators);
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

  const renderOverview = (sortedDataDesc: DailyCandleData[]) => {
    if (analysisError === "stale") {
      return (
        <div className="flex-1 flex flex-col items-center justify-center p-6 bg-slate-950 text-center">
          <div className="max-w-md p-6 bg-slate-900 border border-slate-800 rounded-xl shadow-xl space-y-4">
            <div className="mx-auto flex items-center justify-center w-12 h-12 rounded-full bg-amber-500/10 text-amber-400">
              <Info size={24} />
            </div>
            <h3 className="text-lg font-bold text-white">Analysis Not Yet Complete</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Technical analysis has not yet been computed or is currently out-of-date for <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>. The daily and weekly scheduled update pipelines must run first to complete this.
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
              Could not reach the analysis service for <span className="font-mono text-blue-400 font-semibold">{selectedTicker}</span>. Please check that the backend is running and try again.
            </p>
          </div>
        </div>
      );
    }

    if (sortedDataDesc.length === 0 || !analysisData) {
      return (
        <div className="flex-1 flex flex-col items-center justify-center text-slate-500 gap-3">
          <Loader2 className="animate-spin text-blue-500" size={32} />
          <p className="text-sm font-semibold text-slate-400">
            Computing technical analysis checklist...
          </p>
        </div>
      );
    }

    const latest = sortedDataDesc[0];
    const priceChange = latest.close - latest.open;
    const priceChangePct = pctChange(priceChange, latest.open);

    const candlestick = evaluateCandlestickPattern(sortedDataDesc);

    const sections = [
      {
        name: "1. Setup",
        action: "HOLD",
        rows: [
          {
            slNo: 1,
            criteria: "Plot Support & Resistances",
            condition: "- Horizontal & Angular\n- Monthly to Wave",
            action: "MANDATORY",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
          {
            slNo: 2,
            criteria: "Wait for the Weapon Candle",
            condition:
              "- Bullish Candle closes above Resistance\n- Bearish Candle closes below Support",
            action: "MANDATORY",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
        ],
      },
      {
        name: "2. Double Screen",
        action: (() => {
          const buy =
            analysisData.macdSignal === "BUY" &&
            analysisData.stochasticSignal === "BUY" &&
            analysisData.rsiSignal === "BUY";
          const sell =
            analysisData.macdSignal === "SELL" &&
            analysisData.stochasticSignal === "SELL" &&
            analysisData.rsiSignal === "SELL";
          return buy ? "BUY" : sell ? "SELL" : "HOLD";
        })(),
        rows: [
          {
            slNo: 1,
            criteria: "MACD (12, 26) @ TIDE",
            condition: analysisData.macdValue,
            action: analysisData.macdSignal,
            automationStatus: "DONE" as const,
          },
          {
            slNo: 2,
            criteria: "Stochastic (14,3,3) @ WAVE\nUpper - 80 / Lower - 20",
            condition: analysisData.stochasticValue,
            action: analysisData.stochasticSignal,
            automationStatus: "DONE" as const,
          },
          {
            slNo: 3,
            criteria: "RSI (14) @ WAVE\nUpper - 70 / Lower - 30",
            condition: analysisData.rsiValue,
            action: analysisData.rsiSignal,
            automationStatus: "DONE" as const,
          },
        ],
      },
      {
        name: "3. Combined Checklist",
        action: (() => {
          const buy =
            candlestick.signal === "BUY" &&
            analysisData.volumeSignal === "BUY" &&
            analysisData.emaSignal === "BUY";
          const sell =
            candlestick.signal === "SELL" &&
            analysisData.volumeSignal === "SELL" &&
            analysisData.emaSignal === "SELL";
          return buy ? "BUY" : sell ? "SELL" : "HOLD";
        })(),
        rows: [
          {
            slNo: 1,
            criteria: "Candlestick Patterns",
            condition: candlestick.value,
            action: candlestick.signal,
            automationStatus: "IN_PROGRESS" as const,
          },
          {
            slNo: 2,
            criteria: "Volume",
            condition: analysisData.volumeValue,
            action: analysisData.volumeSignal,
            automationStatus: "DONE" as const,
          },
          {
            slNo: 3,
            criteria: "Moving Average (EMA)",
            condition: analysisData.emaValue,
            action: analysisData.emaSignal,
            automationStatus: "DONE" as const,
          },
          {
            slNo: 4,
            criteria: "Chart Pattern",
            condition:
              "Inverted Head & Shoulders / Double Bottom (Buy) or Head & Shoulders / Double Top (Sell)",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
          {
            slNo: 5,
            criteria: "Fibonacci Retracement",
            condition: "Up to 50% Retracement (Healthy) or 61.8%+ (Caution)",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
          {
            slNo: 6,
            criteria: "Divergence in Oscillators",
            condition:
              "Bullish Divergence (Buy) or Bearish Divergence (Sell/Caution)",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
          {
            slNo: 7,
            criteria: "Immediate Support/Resistance (Stop Loss)",
            condition:
              "Low of Weapon Candle (Buy Stop Loss) or High of Weapon Candle (Sell Stop Loss)",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
          {
            slNo: 8,
            criteria: "Major Resistance/Support (Target)",
            condition:
              "Based on Chart Patterns / Trend Lines from previous tops or bottoms",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
        ],
      },
      {
        name: "4. Risk / Reward Ratio",
        action: "HOLD",
        rows: [
          {
            slNo: 1,
            criteria: "Risk / Reward Ratio",
            condition:
              "1:3 or more (Proceed) / 1:2.5 or more (Reduce Quantity)",
            action: "HOLD",
            automationStatus: "TO_BE_IMPLEMENTED" as const,
          },
        ],
      },
    ];

    const doubleScreenAction = sections[1].action;
    const checklistAction = sections[2].action;
    const verdict =
      doubleScreenAction === "BUY" && checklistAction === "BUY"
        ? "BUY"
        : doubleScreenAction === "SELL" && checklistAction === "SELL"
          ? "SELL"
          : "HOLD";

    return (
      <div className="flex-1 overflow-y-auto p-6 space-y-6 bg-slate-950 text-slate-200">
        {/* Top Section: Overall Verdict & Quick Stats */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Overall Verdict Card */}
          <div
            className={`col-span-1 lg:col-span-2 rounded-xl border p-6 flex items-center justify-between shadow-xl backdrop-blur relative overflow-hidden transition-all duration-300 ${
              verdict === "BUY"
                ? "bg-emerald-950/20 border-emerald-500/30 text-emerald-100"
                : verdict === "SELL"
                  ? "bg-rose-950/20 border-rose-500/30 text-rose-100"
                  : "bg-slate-900/60 border-slate-800 text-slate-100"
            }`}
          >
            <div className="space-y-2 z-10">
              <span className="text-xs font-semibold uppercase tracking-widest text-slate-400">
                Technical Analysis Verdict
              </span>
              <div className="flex items-center gap-3">
                <h1
                  className={`text-5xl font-black uppercase tracking-tight ${
                    verdict === "BUY"
                      ? "text-emerald-400 drop-shadow-[0_0_15px_rgba(52,211,153,0.3)]"
                      : verdict === "SELL"
                        ? "text-rose-400 drop-shadow-[0_0_15px_rgba(251,113,133,0.3)]"
                        : "text-amber-400"
                  }`}
                >
                  {verdict}
                </h1>
                <span className="text-xs px-2.5 py-1 rounded-full bg-slate-800/80 border border-slate-700 font-semibold text-slate-300">
                  Consensus Signal
                </span>
              </div>
              <p className="text-slate-400 text-sm max-w-lg mt-2 leading-relaxed">
                {verdict === "BUY" &&
                  "All primary automated checks align to a strong BUY recommendation. Consider entering a long position."}
                {verdict === "SELL" &&
                  "All primary automated checks align to a strong SELL recommendation. Consider reducing exposure or shorting."}
                {verdict === "HOLD" &&
                  "Oscillators or trend indicators show mixed signals. Avoid trading until setup is clean."}
              </p>
            </div>
            <div
              className={`hidden sm:flex p-4 rounded-full z-10 ${
                verdict === "BUY"
                  ? "bg-emerald-500/10 text-emerald-400"
                  : verdict === "SELL"
                    ? "bg-rose-500/10 text-rose-400"
                    : "bg-slate-800 text-amber-400"
              }`}
            >
              {verdict === "BUY" ? (
                <TrendingUp size={48} />
              ) : verdict === "SELL" ? (
                <TrendingDown size={48} />
              ) : (
                <Loader2 size={48} className="animate-pulse text-amber-500" />
              )}
            </div>
            {/* Decorative glow */}
            <div
              className={`absolute -right-24 -bottom-24 w-64 h-64 rounded-full blur-3xl opacity-10 ${
                verdict === "BUY"
                  ? "bg-emerald-500"
                  : verdict === "SELL"
                    ? "bg-rose-500"
                    : "bg-amber-500"
              }`}
            />
          </div>

          {/* Quick Stats Card */}
          <div className="bg-slate-900/40 border border-slate-800 rounded-xl p-6 shadow-xl backdrop-blur flex flex-col justify-between">
            <div>
              <div className="text-xs font-semibold text-slate-400 uppercase tracking-widest">
                Market Status
              </div>
              <div className="mt-4 flex items-baseline gap-2">
                <span className="text-3xl font-extrabold text-white">
                  {Number(latest.close).toLocaleString(undefined, {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 4,
                  })}
                </span>
                <span
                  className={`text-xs font-bold flex items-center px-2 py-0.5 rounded-full ${
                    priceChange >= 0
                      ? "bg-emerald-500/10 text-emerald-400"
                      : "bg-rose-500/10 text-rose-400"
                  }`}
                >
                  {priceChange >= 0 ? "+" : ""}
                  {priceChangePct.toFixed(2)}%
                </span>
              </div>
            </div>
            <div className="mt-6 border-t border-slate-800/80 pt-4 grid grid-cols-2 gap-4 text-xs">
              <div>
                <span className="text-slate-500 block">24h High</span>
                <span className="text-slate-300 font-bold">
                  {Number(latest.high).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">24h Low</span>
                <span className="text-slate-300 font-bold">
                  {Number(latest.low).toFixed(2)}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">24h Volume</span>
                <span className="text-slate-300 font-bold">
                  {Number(latest.vol).toLocaleString()}
                </span>
              </div>
              <div>
                <span className="text-slate-500 block">Ticker</span>
                <span className="text-slate-300 font-bold font-mono">
                  {selectedTicker}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Table Checklist */}
        <div className="bg-slate-900/40 border border-slate-800 rounded-xl overflow-hidden shadow-xl backdrop-blur">
          <div className="px-6 py-4 bg-slate-900/80 border-b border-slate-800 flex justify-between items-center">
            <h2 className="text-base font-bold text-white">SMM Checklist</h2>
            <span className="text-xs text-slate-400">
              Automatic evaluation of daily and weekly indicators
            </span>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-950 text-slate-400 font-bold border-b border-slate-800">
                  <th className="p-4 w-16 text-center">Sl.No.</th>
                  <th className="p-4 w-1/4">Criteria</th>
                  <th className="p-4 w-5/12">Condition</th>
                  <th className="p-4 w-1/6 text-center">Action</th>
                  <th className="p-4 w-1/6 text-center">Automation Status</th>
                </tr>
              </thead>
              <tbody>
                {sections.map((section) => (
                  <Fragment key={section.name}>
                    {/* Section Header Row */}
                    <tr className="bg-slate-900/70 border-y border-slate-800/80">
                      <td colSpan={5} className="px-6 py-3.5">
                        <div className="flex justify-between items-center">
                          <span className="font-extrabold text-blue-400 uppercase tracking-wider text-xs">
                            {section.name}
                          </span>
                          <span
                            className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold border uppercase ${
                              section.action === "BUY"
                                ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                                : section.action === "SELL"
                                  ? "bg-rose-500/10 text-rose-400 border-rose-500/20"
                                  : section.action === "HOLD"
                                    ? "bg-slate-800 text-slate-400 border-slate-700"
                                    : "bg-blue-500/10 text-blue-400 border-blue-500/20"
                            }`}
                          >
                            Section Verdict: {section.action}
                          </span>
                        </div>
                      </td>
                    </tr>

                    {/* Rows under this section */}
                    {section.rows.map((row, rowIdx) => {
                      const actionVal = row.action;
                      const isBuy =
                        actionVal === "BUY" ||
                        actionVal === "PROCEED" ||
                        actionVal === "HEALTHY";
                      const isSell =
                        actionVal === "SELL" ||
                        actionVal === "CAUTION" ||
                        actionVal === "REDUCE_QUANTITY";
                      const isHold =
                        actionVal === "HOLD" || actionVal === "MANDATORY";

                      return (
                        <tr
                          key={row.criteria}
                          className="border-b border-slate-800/40 hover:bg-slate-900/20 transition-colors"
                        >
                          <td className="p-4 text-center text-slate-500 font-semibold font-mono">
                            {row.slNo || rowIdx + 1}
                          </td>
                          <td className="p-4 font-bold text-slate-300 whitespace-pre-line">
                            {row.criteria}
                          </td>
                          <td className="p-4 text-slate-400 whitespace-pre-line leading-relaxed">
                            {row.condition}
                          </td>
                          <td className="p-4 text-center">
                            <span
                              className={`inline-block px-3 py-1 rounded-full text-xs font-bold border ${
                                isBuy
                                  ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
                                  : isSell
                                    ? "bg-rose-500/10 text-rose-400 border-rose-500/20"
                                    : isHold
                                      ? "bg-slate-800/40 text-slate-400 border-slate-700/50"
                                      : "bg-slate-900 text-slate-500 border-slate-800"
                              }`}
                            >
                              {actionVal || "—"}
                            </span>
                          </td>
                          <td className="p-4 text-center">
                            <span
                              className={`inline-block px-2.5 py-0.5 rounded text-[10px] font-bold border uppercase tracking-wide ${
                                row.automationStatus === "DONE"
                                  ? "bg-blue-500/10 text-blue-400 border-blue-500/20"
                                  : row.automationStatus === "IN_PROGRESS"
                                    ? "bg-amber-500/10 text-amber-400 border-amber-500/20"
                                    : "bg-slate-950 text-slate-600 border-slate-800"
                              }`}
                            >
                              {row.automationStatus === "TO_BE_IMPLEMENTED"
                                ? "Manual Check"
                                : row.automationStatus}
                            </span>
                          </td>
                        </tr>
                      );
                    })}
                  </Fragment>
                ))}
              </tbody>
            </table>
          </div>

          <div className="p-4 bg-slate-950/80 border-t border-slate-800 text-[11px] text-slate-500 flex gap-2 items-center">
            <Info size={14} className="text-blue-500 flex-shrink-0" />
            <span>
              <strong>IMPORTANT:</strong> If MACD & Oscillators (Double Screen)
              give mixed signals, DO NOT proceed !!!
            </span>
          </div>
        </div>
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

    const hasDailyData = dailyCandleData.length > 0;
    const sortedDailyDesc = [...dailyCandleData].sort((a, b) =>
      b.date.localeCompare(a.date),
    );

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
          hasDailyData ? (
            renderOverview(sortedDailyDesc)
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500">
              {loading
                ? "Loading data..."
                : "No data available for this ticker"}
            </div>
          )
        ) : (
          <div className="flex-1 flex flex-col gap-4 p-4 overflow-hidden">
            {selectedTicker && (
              <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-3 shadow-lg backdrop-blur flex flex-col sm:flex-row sm:justify-between sm:items-center gap-3">
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
            )}
            <div className="flex-1 relative min-h-0 bg-slate-950 border border-slate-800 rounded-lg overflow-hidden">
              {chartCandleData.length > 0 && loadedSymbol ? (
                <Chart
                  data={chartCandleData}
                  indicators={chartIndicators}
                  enabled={enabledIndicators}
                  configs={indicatorConfigs}
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
