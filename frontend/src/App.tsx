import { useState, useEffect } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Chart from './components/Chart';
import { getTickers, getMarketData, type Ticker, type MarketData } from './services/api';
import { Loader2 } from 'lucide-react';

function App() {
  const [tickers, setTickers] = useState<Ticker[]>([]);
  const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
  const [marketData, setMarketData] = useState<MarketData[]>([]);
  const [loading, setLoading] = useState(false);

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
    const fetchMarketData = async () => {
      if (selectedTicker) {
        setLoading(true);
        try {
          const data = await getMarketData(selectedTicker);
          setMarketData(data);
        } catch (error) {
          console.error('Failed to fetch market data:', error);
          setMarketData([]);
        } finally {
          setLoading(false);
        }
      }
    };
    fetchMarketData();
  }, [selectedTicker]);

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
              <div className="flex-1 relative overflow-hidden p-4">
                {marketData.length > 0 ? (
                  <Chart data={marketData} />
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
