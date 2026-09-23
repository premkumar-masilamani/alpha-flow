import React, { useState, useMemo } from 'react';
import { ChevronDown, ChevronRight } from 'lucide-react';
import type { Ticker, TickerType, Country } from '../services/api';

interface SidebarProps {
    tickers: Ticker[];
    selectedTicker: string | null;
    onSelectTicker: (symbol: string) => void;
}

interface SectionGroup {
    key: string;
    header: string;
    country: Country;
    type: TickerType;
    items: Ticker[];
}

const Sidebar: React.FC<SidebarProps> = ({ tickers, selectedTicker, onSelectTicker }) => {
    const [collapsedSections, setCollapsedSections] = useState<Record<string, boolean>>({});

    const toggleSection = (key: string) => {
        setCollapsedSections((prev) => ({
            ...prev,
            [key]: !prev[key]
        }));
    };

    const sections: SectionGroup[] = useMemo(() => {
        const groups = new Map<string, SectionGroup>();

        tickers.forEach((ticker) => {
            const country: Country = ticker.country || 'US';
            const type: TickerType = ticker.type || 'STOCK';
            const key = `${country} - ${type}`;

            if (!groups.has(key)) {
                groups.set(key, {
                    key,
                    header: `${country} - ${type}`,
                    country,
                    type,
                    items: []
                });
            }
            groups.get(key)!.items.push(ticker);
        });

        return Array.from(groups.values()).sort((a, b) => {
            // Show indices first, followed by stocks
            if (a.type !== b.type) {
                return a.type === 'INDEX' ? -1 : 1;
            }
            // For same type, sort by country/key
            return a.key.localeCompare(b.key);
        });
    }, [tickers]);

    const renderTickerList = (items: Ticker[]) => {
        if (items.length === 0) {
            return (
                <div className="px-5 py-2 text-xs text-slate-500 italic">
                    None available
                </div>
            );
        }

        return (
            <ul>
                {items.map((ticker) => (
                    <li key={ticker.symbol}>
                        <button
                            onClick={() => onSelectTicker(ticker.symbol)}
                            className={`w-full text-left px-4 py-2 hover:bg-slate-700 transition-colors flex flex-col ${
                                selectedTicker === ticker.symbol ? 'bg-slate-700 text-white border-l-4 border-blue-500' : 'pl-5'
                            }`}
                        >
                            <span className="font-medium text-sm">{ticker.symbol}</span>
                            <span className="text-[10px] text-slate-500 truncate w-full">{ticker.name}</span>
                        </button>
                    </li>
                ))}
            </ul>
        );
    };

    return (
        <div className="w-64 bg-slate-800 text-slate-300 flex flex-col h-full border-r border-slate-700 select-none">
            <div className="p-4 border-b border-slate-700">
                <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Tickers</h2>
            </div>
            <div className="flex-1 overflow-y-auto py-2">
                {sections.map((section) => {
                    const isExpanded = !collapsedSections[section.key];
                    return (
                        <div key={section.key} className="mb-2">
                            <button
                                onClick={() => toggleSection(section.key)}
                                className="w-full flex items-center justify-between px-4 py-2 text-xs font-semibold uppercase tracking-wider text-slate-400 hover:text-slate-200 transition-colors"
                                aria-expanded={isExpanded}
                            >
                                <div className="flex items-center gap-1.5">
                                    {isExpanded ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                                    <span>{section.header}</span>
                                </div>
                                <span className="bg-slate-700 text-slate-400 text-[10px] px-1.5 py-0.5 rounded-full font-mono">
                                    {section.items.length}
                                </span>
                            </button>
                            {isExpanded && renderTickerList(section.items)}
                        </div>
                    );
                })}
            </div>
        </div>
    );
};

export default Sidebar;
