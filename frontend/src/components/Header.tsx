import React, { useState, useEffect } from 'react';
import { TrendingUp, Clock, Calendar, AlertTriangle } from 'lucide-react';
import { getMarketStatus } from '../utils/marketStatus';

interface HeaderProps {
    staleTickersCount?: number;
    onOpenStaleModal?: () => void;
}

const Header: React.FC<HeaderProps> = ({ staleTickersCount = 0, onOpenStaleModal }) => {
    const [currentTime, setCurrentTime] = useState(new Date());

    useEffect(() => {
        const timer = setInterval(() => setCurrentTime(new Date()), 60_000);
        return () => clearInterval(timer);
    }, []);

    const inStatus = getMarketStatus(currentTime, 'IN');
    const usStatus = getMarketStatus(currentTime, 'US');

    const formattedDate = currentTime.toLocaleDateString(undefined, {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        year: 'numeric'
    });

    const formattedTime = currentTime.toLocaleTimeString(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        hour12: true
    });

    return (
        <header className="bg-slate-900 text-white px-4 py-3 shadow-md flex items-center justify-between border-b border-slate-800 gap-3 flex-wrap">
            <div className="flex items-center gap-2">
                <TrendingUp className="text-blue-400" />
                <h1 className="text-xl font-bold tracking-tight" id="app-title">Alpha Flow</h1>
            </div>

            <div className="flex items-center gap-2.5 flex-wrap">
                <div
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors ${
                        inStatus.isOpen
                            ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300'
                            : 'bg-rose-950/40 border-rose-500/40 text-rose-300'
                    }`}
                    id="nse-market-status"
                >
                    <span
                        className={`w-2 h-2 rounded-full inline-block ${
                            inStatus.isOpen
                                ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]'
                                : 'bg-rose-400 shadow-[0_0_8px_rgba(251,113,133,0.8)]'
                        }`}
                    />
                    <span>🇮🇳 NSE: {inStatus.displayMessage}</span>
                </div>

                <div
                    className={`flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium border transition-colors ${
                        usStatus.isOpen
                            ? 'bg-emerald-950/40 border-emerald-500/40 text-emerald-300'
                            : 'bg-rose-950/40 border-rose-500/40 text-rose-300'
                    }`}
                    id="nyse-market-status"
                >
                    <span
                        className={`w-2 h-2 rounded-full inline-block ${
                            usStatus.isOpen
                                ? 'bg-emerald-400 shadow-[0_0_8px_rgba(52,211,153,0.8)]'
                                : 'bg-rose-400 shadow-[0_0_8px_rgba(251,113,133,0.8)]'
                        }`}
                    />
                    <span>🇺🇸 NYSE: {usStatus.displayMessage}</span>
                </div>

                {staleTickersCount > 0 && (
                    <button
                        type="button"
                        id="stale-tickers-badge"
                        onClick={onOpenStaleModal}
                        className="flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border border-amber-500/60 bg-amber-950/60 text-amber-300 hover:bg-amber-900/70 hover:border-amber-400 transition-colors cursor-pointer shadow-sm"
                    >
                        <span className="w-2 h-2 rounded-full inline-block bg-amber-400 shadow-[0_0_8px_rgba(251,191,36,0.9)] animate-pulse" />
                        <AlertTriangle size={13} />
                        <span>Stale Data ({staleTickersCount})</span>
                    </button>
                )}
            </div>

            <div className="flex items-center gap-3 bg-slate-950/70 border border-slate-800 px-3 py-1.5 rounded-lg shadow-inner">
                <div className="flex items-center gap-1.5 text-xs text-slate-400 font-medium">
                    <Calendar size={14} className="text-blue-400" />
                    <span>{formattedDate}</span>
                </div>
                <span className="text-slate-700">|</span>
                <div className="flex items-center gap-1.5 text-xs font-mono font-bold text-slate-200">
                    <Clock size={14} className="text-emerald-400" />
                    <span>{formattedTime}</span>
                </div>
            </div>
        </header>
    );
};

export default Header;
