import React, { useState, useEffect } from 'react';
import { TrendingUp, Clock, Calendar } from 'lucide-react';
import { getCachedMarketHolidays, type MarketHoliday } from '../services/api';
import { getMarketStatus } from '../utils/marketStatus';

const Header: React.FC = () => {
    const [currentTime, setCurrentTime] = useState(new Date());
    const [holidays, setHolidays] = useState<MarketHoliday[]>([]);

    useEffect(() => {
        const timer = setInterval(() => setCurrentTime(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

    useEffect(() => {
        let isMounted = true;
        getCachedMarketHolidays()
            .then((data) => {
                if (isMounted) {
                    setHolidays(data);
                }
            })
            .catch((error) => {
                console.warn('Failed to load market holidays in header:', error);
            });
        return () => {
            isMounted = false;
        };
    }, []);

    const inStatus = getMarketStatus(currentTime, 'IN', holidays);
    const usStatus = getMarketStatus(currentTime, 'US', holidays);

    const formattedDate = currentTime.toLocaleDateString(undefined, {
        weekday: 'short',
        day: '2-digit',
        month: 'short',
        year: 'numeric'
    });

    const formattedTime = currentTime.toLocaleTimeString(undefined, {
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
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
                    title={inStatus.holidayName ? `Holiday: ${inStatus.holidayName}` : undefined}
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
                    title={usStatus.holidayName ? `Holiday: ${usStatus.holidayName}` : undefined}
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
