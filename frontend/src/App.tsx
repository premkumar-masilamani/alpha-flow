import {useEffect, useState} from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import Chart from './components/Chart';
import {type DailyCandleData, getCandleData, getTickers, type Ticker} from './services/api';
import {Loader2, ChevronLeft, ChevronRight, TrendingUp, TrendingDown} from 'lucide-react';



function App() {
    const [tickers, setTickers] = useState<Ticker[]>([]);
    const [selectedTicker, setSelectedTicker] = useState<string | null>(null);
    const [dailyCandleData, setDailyCandleData] = useState<DailyCandleData[]>([]);
    const [loading, setLoading] = useState(false);

    // Layout states
    const [sidebarOpen, setSidebarOpen] = useState(true);
    const [activeTab, setActiveTab] = useState<'overview' | 'charts'>('overview');

    useEffect(() => {
        const fetchTickers = async () => {
            try {
                const data = await getTickers();
                setTickers(data);
                if (data.length > 0) {
                    const btcUsd = data.find(t => t.symbol === 'BTC-USD');
                    setSelectedTicker(btcUsd ? btcUsd.symbol : data[0].symbol);
                }
            } catch (error) {
                console.error('Failed to fetch tickers:', error);
            }
        };
        fetchTickers();
    }, []);

    // Fetch daily candle data when selectedTicker changes
    useEffect(() => {
        const fetchDailyData = async () => {
            if (selectedTicker) {
                setLoading(true);
                try {
                    const data = await getCandleData(selectedTicker);
                    setDailyCandleData(data);
                } catch (error) {
                    console.error('Failed to fetch daily data:', error);
                    setDailyCandleData([]);
                } finally {
                    setLoading(false);
                }
            }
        };
        fetchDailyData();
    }, [selectedTicker]);

    const renderOverview = (sortedDataDesc: DailyCandleData[]) => {
        if (sortedDataDesc.length === 0) return null;

        const latest = sortedDataDesc[0];

        const priceChange = latest.close - latest.open;
        const priceChangePct = (priceChange / latest.open) * 100;
        const range = latest.high - latest.low;

        return (
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
                {/* Metrics Cards Grid */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                    {/* Price Card */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-5 shadow-lg backdrop-blur">
                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Close Price</div>
                        <div className="mt-2 flex items-baseline gap-2">
                            <span className="text-3xl font-extrabold text-white">
                                {Number(latest.close).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 4})}
                            </span>
                            <span className={`text-xs font-semibold flex items-center ${priceChange >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                                {priceChange >= 0 ? <TrendingUp size={12} className="mr-0.5" /> : <TrendingDown size={12} className="mr-0.5" />}
                                {priceChangePct.toFixed(2)}%
                            </span>
                        </div>
                        <div className="mt-1 text-slate-500 text-[10px]">Open: {Number(latest.open).toFixed(2)}</div>
                    </div>

                    {/* Spread Card */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-5 shadow-lg backdrop-blur">
                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Daily High/Low</div>
                        <div className="mt-2 text-2xl font-extrabold text-white">
                            {Number(range).toLocaleString(undefined, {minimumFractionDigits: 2, maximumFractionDigits: 2})}
                        </div>
                        <div className="mt-1 text-slate-500 text-[10px] flex justify-between">
                            <span>Low: {Number(latest.low).toFixed(2)}</span>
                            <span>High: {Number(latest.high).toFixed(2)}</span>
                        </div>
                    </div>

                    {/* Volume Card */}
                    <div className="bg-slate-900/60 border border-slate-800 rounded-lg p-5 shadow-lg backdrop-blur">
                        <div className="text-xs font-bold text-slate-500 uppercase tracking-widest">Volume</div>
                        <div className="mt-2 text-2xl font-extrabold text-white">
                            {Number(latest.vol).toLocaleString(undefined, {maximumFractionDigits: 0})}
                        </div>
                    </div>
                </div>

                {/* Tabbed Data Tables */}
                <div className="bg-slate-900/40 border border-slate-800 rounded-lg overflow-hidden shadow-lg">
                    {/* Table Selection */}
                    <div className="flex border-b border-slate-800 bg-slate-900/70">
                        <div className="px-6 py-3 text-sm font-bold text-blue-400 border-b-2 border-blue-400 bg-blue-400/5">
                            Recent Candlestick Prices
                        </div>
                    </div>

                    {/* Table View */}
                    <div className="overflow-x-auto">
                        <table className="w-full text-left border-collapse text-sm">
                            <thead>
                                <tr className="bg-slate-900/90 text-slate-400 font-bold border-b border-slate-800">
                                    <th className="p-3.5">Date</th>
                                    <th className="p-3.5">Open</th>
                                    <th className="p-3.5">High</th>
                                    <th className="p-3.5">Low</th>
                                    <th className="p-3.5">Close</th>
                                    <th className="p-3.5">Price Change</th>
                                    <th className="p-3.5">Volume</th>
                                </tr>
                            </thead>
                            <tbody>
                                {sortedDataDesc.slice(0, 15).map((row) => {
                                    const change = row.close - row.open;
                                    const pct = (change / row.open) * 100;
                                    return (
                                        <tr key={row.date} className="border-b border-slate-800/50 hover:bg-slate-900/30 transition-colors">
                                            <td className="p-3.5 font-medium text-slate-300">{row.date}</td>
                                            <td className="p-3.5 text-slate-400">{Number(row.open).toFixed(2)}</td>
                                            <td className="p-3.5 text-slate-400">{Number(row.high).toFixed(2)}</td>
                                            <td className="p-3.5 text-slate-400">{Number(row.low).toFixed(2)}</td>
                                            <td className="p-3.5 text-slate-100 font-semibold">{Number(row.close).toFixed(2)}</td>
                                            <td className={`p-3.5 font-bold ${change >= 0 ? 'text-green-400' : 'text-red-400'}`}>
                                                {change >= 0 ? '+' : ''}{change.toFixed(2)} ({change >= 0 ? '+' : ''}{pct.toFixed(2)}%)
                                            </td>
                                            <td className="p-3.5 text-slate-400 font-mono">{Number(row.vol).toLocaleString()}</td>
                                        </tr>
                                    );
                                })}
                            </tbody>
                        </table>
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
        const sortedDailyDesc = [...dailyCandleData].sort((a, b) => b.date.localeCompare(a.date));

        return (
            <>
                {/* Ticker Header & Tab bar */}
                <div className="p-4 border-b border-slate-800 flex flex-col md:flex-row md:items-center justify-between gap-4">
                    <div className="flex items-center gap-3">
                        {/* Sidebar Toggle */}
                        <button
                            onClick={() => setSidebarOpen(!sidebarOpen)}
                            className="p-1.5 rounded bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
                            title={sidebarOpen ? 'Collapse Left Menu' : 'Expand Left Menu'}
                        >
                            {sidebarOpen ? <ChevronLeft size={20}/> : <ChevronRight size={20}/>}
                        </button>
                        <div>
                            <h2 className="text-2xl font-bold text-white">{selectedTicker}</h2>
                            <p className="text-slate-400 text-sm">
                                {tickers.find((t) => t.symbol === selectedTicker)?.name}
                            </p>
                        </div>
                    </div>

                    <div className="flex items-center gap-3">
                        {/* Tab Navigation */}
                        <div className="flex bg-slate-900 border border-slate-800 p-0.5 rounded-lg">
                            <button
                                onClick={() => setActiveTab('overview')}
                                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${
                                    activeTab === 'overview' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'
                                }`}
                            >
                                Technical Analysis
                            </button>
                            <button
                                onClick={() => setActiveTab('charts')}
                                className={`px-4 py-1.5 text-xs font-bold rounded-md transition-all ${
                                    activeTab === 'charts' ? 'bg-blue-600 text-white shadow-md' : 'text-slate-400 hover:text-slate-200'
                                }`}
                            >
                                Technical Chart
                            </button>
                        </div>
                    </div>

                    <div className="flex items-center gap-4">
                        {loading && <Loader2 className="animate-spin text-blue-500"/>}
                    </div>
                </div>

                {/* Dashboard Tab Content */}
                {activeTab === 'overview' ? (
                    hasDailyData ? (
                        renderOverview(sortedDailyDesc)
                    ) : (
                        <div className="flex-1 flex items-center justify-center text-slate-500">
                            {loading ? 'Loading data...' : 'No data available for this ticker'}
                        </div>
                    )
                ) : (
                    <div className="flex-1 relative overflow-hidden p-4">
                        {hasDailyData ? (
                            <Chart data={dailyCandleData} />
                        ) : (
                            <div className="absolute inset-0 flex items-center justify-center text-slate-500">
                                {loading ? 'Loading data...' : 'No data available for this ticker'}
                            </div>
                        )}
                    </div>
                )}
            </>
        );
    };

    return (
        <div className="flex flex-col h-screen bg-slate-900 overflow-hidden">
            <Header/>
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
