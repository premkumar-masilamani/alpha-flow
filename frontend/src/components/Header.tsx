import React, { useState, useEffect } from 'react';
import { TrendingUp, Clock, Calendar } from 'lucide-react';

const Header: React.FC = () => {
    const [currentTime, setCurrentTime] = useState(new Date());

    useEffect(() => {
        const timer = setInterval(() => setCurrentTime(new Date()), 1000);
        return () => clearInterval(timer);
    }, []);

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
        <header className="bg-slate-900 text-white px-4 py-3 shadow-md flex items-center justify-between border-b border-slate-800">
            <div className="flex items-center gap-2">
                <TrendingUp className="text-blue-400" />
                <h1 className="text-xl font-bold tracking-tight" id="app-title">Alpha Flow</h1>
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
