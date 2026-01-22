import { useState, useEffect } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Chart from './components/Chart';
import Controls from './components/Controls';
import type { IndicatorConfig } from './components/Controls';
import { getTickers, type Ticker, type MarketData } from './services/api';
import { Loader2 } from 'lucide-react';

const generateMockMarketData = (symbol: string): MarketData[] => {
  const data: MarketData[] = [];
  const now = new Date();
  let basePrice = symbol === 'BTCUSDT' ? 60000 : symbol === 'ETHUSDT' ? 3000 : 500;

  for (let i = 0; i < 100; i++) {
    const date = new Date(now);
    date.setDate(date.getDate() - (100 - i));
    const dateStr = date.toISOString().split('T')[0];

    const open = basePrice + Math.random() * 100 - 50;
    const high = open + Math.random() * 50;
    const low = open - Math.random() * 50;
    const close = (high + low) / 2 + Math.random() * 20 - 10;
    const vol = Math.random() * 1000 + 500;

    data.push({
      date: dateStr,
      open,
      high,
      low,
      close,
      vol,
      vwap: (high + low + close) / 3,
      poc: (high + low) / 2,
      vah: high - 5,
      val: low + 5,
      bvs: Math.random(),
      bcs: Math.random(),
    });

    basePrice = close;
  }
  return data;
};

function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const [marketData, setMarketData] = useState<MarketData[]>([]);
  const [loading, setLoading] = useState(false);
  const [indicators, setIndicators] = useState<IndicatorConfig[]>([
    { id: 'vwap', label: 'VWAP', color: '#f59e0b', visible: true },
    { id: 'poc', label: 'POC', color: '#8b5cf6', visible: true },
    { id: 'vah', label: 'VAH', color: '#10b981', visible: false },
    { id: 'val', label: 'VAL', color: '#ef4444', visible: false },
    { id: 'bvs', label: 'Buyer Vol Share', color: '#3b82f6', visible: false },
    { id: 'bcs', label: 'Buyer Cap Share', color: '#ec4899', visible: false },
  ]);

  useEffect(() => {
    const fetchTickers = async () => {
      try {
        const data = await getTickers();
        setTickers(data);
        if (data.length > 0) {
          setSelectedTicker(data[0].symbol);
        }
      } catch (error) {
        console.error('Failed to fetch tickers:', error);
      }
    };
    fetchTickers();
  }, []);

  useEffect(() => {
    if (selectedTicker) {
      setLoading(true);
      const timer = setTimeout(() => {
        setMarketData(generateMockMarketData(selectedTicker));
        setLoading(false);
      }, 500);
      return () => clearTimeout(timer);
    }
  }, [selectedTicker]);

  const handleToggleIndicator = (id: string) => {
    setIndicators((prev) =>
      prev.map((ind) => (ind.id === id ? { ...ind, visible: !ind.visible } : ind))
    );
  };

  return (
    <div className="flex flex-col h-screen bg-slate-900 overflow-hidden">
      <Header />
      <div className="flex flex-1 overflow-hidden">
        <Sidebar
          tickers={tickers}
          selectedTicker={selectedTicker}
          onSelectTicker={setSelectedTicker}
        />
        <main className="flex-1 flex flex-col bg-slate-950 overflow-hidden">
          {selectedTicker ? (
            <>
              <div className="p-4 border-b border-slate-800 flex items-center justify-between">
                <div>
                  <h2 className="text-2xl font-bold text-white">{selectedTicker}</h2>
                  <p className="text-slate-400 text-sm">
                    {tickers.find((t) => t.symbol === selectedTicker)?.name}
                  </p>
                </div>
                {loading && <Loader2 className="animate-spin text-blue-500" />}
              </div>
              <Controls indicators={indicators} onToggle={handleToggleIndicator} />
              <div className="flex-1 relative overflow-hidden p-4">
                {marketData.length > 0 ? (
                  <Chart data={marketData} indicators={indicators} />
                ) : (
                  <div className="absolute inset-0 flex items-center justify-center text-slate-500">
                    {loading ? 'Loading data...' : 'No data available for this ticker'}
                  </div>
                )}
              </div>
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-slate-500 italic">
              Select a ticker to view analysis
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
