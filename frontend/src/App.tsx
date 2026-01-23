import { useState, useEffect, useMemo } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Chart from './components/Chart';
import RenkoChart from './components/RenkoChart';
import { getTickers, getMarketData, getRenkoData, type Ticker, type MarketData, type RenkoData } from './services/api';
import { Loader2 } from 'lucide-react';

const TABS = ['Candlestick', 'CCS', 'Renko'];

function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const [marketData, setMarketData] = useState<MarketData[]>([]);
  const [renkoData, setRenkoData] = useState<RenkoData | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('Candlestick');

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
    const fetchData = async () => {
      if (selectedTicker) {
        setLoading(true);
        try {
          const [mData, rData] = await Promise.all([
            getMarketData(selectedTicker),
            getRenkoData(selectedTicker)
          ]);
          setMarketData(mData);
          setRenkoData(rData);
        } catch (error) {
          console.error('Failed to fetch data:', error);
          setMarketData([]);
          setRenkoData(null);
        } finally {
          setLoading(false);
        }
      }
    };
    fetchData();
  }, [selectedTicker]);

  const displayData = useMemo(() => {
    if (activeTab === 'CCS') {
      return marketData.map((d) => ({
        ...d,
        open: d.poc,
        close: d.vwap,
        high: d.high,
        low: d.low,
      }));
    }
    return marketData;
  }, [marketData, activeTab]);

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

              <div className="flex border-b border-slate-800 bg-slate-900/50">
                {TABS.map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`px-6 py-3 text-sm font-medium transition-all relative ${
                      activeTab === tab
                        ? 'text-blue-400 border-b-2 border-blue-400 bg-blue-400/5'
                        : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800/50'
                    }`}
                  >
                    {tab}
                  </button>
                ))}
              </div>

              <div className="flex-1 relative overflow-hidden p-4">
                {activeTab === 'Candlestick' || activeTab === 'CCS' ? (
                  marketData.length > 0 ? (
                    <Chart data={displayData} />
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center text-slate-500">
                      {loading ? 'Loading data...' : 'No data available for this ticker'}
                    </div>
                  )
                ) : activeTab === 'Renko' ? (
                  renkoData && renkoData.bricks.length > 0 ? (
                    <RenkoChart data={renkoData} />
                  ) : (
                    <div className="absolute inset-0 flex items-center justify-center text-slate-500">
                      {loading ? 'Loading data...' : 'No Renko data available for this ticker'}
                    </div>
                  )
                ) : (
                  <div className="absolute inset-0 flex flex-col items-center justify-center text-slate-500">
                    <div className="text-xl font-semibold mb-2">{activeTab}</div>
                    <div className="px-4 py-2 border border-slate-700 rounded-md bg-slate-800/50">
                      Under Construction
                    </div>
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
