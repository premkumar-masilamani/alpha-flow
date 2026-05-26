import React, {useState} from 'react';
import type {Ticker} from '../services/api';
import {ChevronDown, ChevronRight} from 'lucide-react';

interface SidebarProps {
    tickers: Ticker[];
    selectedTicker: string | null;
    onSelectTicker: (symbol: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({tickers, selectedTicker, onSelectTicker}) => {
    const [expandedCategories, setExpandedCategories] = useState<Record<string, boolean>>({});

    const toggleCategory = (category: string) => {
        setExpandedCategories(prev => ({
            ...prev,
            [category]: prev[category] === false ? true : false
        }));
    };

    const groupedTickers = tickers.reduce((acc, ticker) => {
        const type = ticker.type || 'CRYPTO';
        if (!acc[type]) acc[type] = [];
        acc[type].push(ticker);
        return acc;
    }, {} as Record<string, Ticker[]>);

    const categories = Object.keys(groupedTickers).sort((a, b) => {
        if (a === 'CRYPTO') return -1;
        if (b === 'CRYPTO') return 1;
        return a.localeCompare(b);
    });

    return (
        <div className="w-64 bg-slate-800 text-slate-300 flex flex-col h-full border-r border-slate-700 select-none">
            <div className="p-4 border-b border-slate-700">
                <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Tickers</h2>
            </div>
            <div className="flex-1 overflow-y-auto">
                {categories.map((category) => {
                    const categoryTickers = groupedTickers[category] || [];
                    const isExpanded = expandedCategories[category] !== false;

                    return (
                        <div key={category} className="border-b border-slate-700/50">
                            <button
                                onClick={() => toggleCategory(category)}
                                className="w-full flex items-center justify-between px-4 py-3 bg-slate-900/50 hover:bg-slate-700/50 transition-colors group"
                            >
                                <span className="text-xs font-bold uppercase tracking-widest text-slate-400 group-hover:text-slate-200">
                                    {category}
                                </span>
                                {isExpanded ? (
                                    <ChevronDown size={14} className="text-slate-500"/>
                                ) : (
                                    <ChevronRight size={14} className="text-slate-500"/>
                                )}
                            </button>
                            {isExpanded && (
                                <ul>
                                    {categoryTickers.map((ticker) => (
                                        <li key={ticker.symbol}>
                                            <button
                                                onClick={() => onSelectTicker(ticker.symbol)}
                                                className={`w-full text-left px-4 py-2.5 hover:bg-slate-700 transition-colors flex flex-col ${
                                                    selectedTicker === ticker.symbol ? 'bg-slate-700 text-white border-l-4 border-blue-500' : 'pl-5'
                                                }`}
                                            >
                                                <span className="font-medium text-sm">{ticker.symbol}</span>
                                                <span className="text-[10px] text-slate-500 truncate w-full">{ticker.name}</span>
                                            </button>
                                        </li>
                                    ))}
                                </ul>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default Sidebar;
