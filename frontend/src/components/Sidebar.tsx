import React from 'react';
import type { Ticker } from '../services/api';

interface SidebarProps {
  tickers: Ticker[];
  selectedTicker: string | null;
  onSelectTicker: (symbol: string) => void;
}

const Sidebar: React.FC<SidebarProps> = ({ tickers, selectedTicker, onSelectTicker }) => {
  return (
    <div className="w-64 bg-slate-800 text-slate-300 flex flex-col h-full border-r border-slate-700">
      <div className="p-4 border-b border-slate-700">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500">Tickers</h2>
      </div>
      <div className="flex-1 overflow-y-auto">
        <ul>
          {tickers.map((ticker) => (
            <li key={ticker.symbol}>
              <button
                onClick={() => onSelectTicker(ticker.symbol)}
                className={`w-full text-left px-4 py-3 hover:bg-slate-700 transition-colors flex items-center justify-between ${
                  selectedTicker === ticker.symbol ? 'bg-slate-700 text-white border-l-4 border-blue-500' : ''
                }`}
              >
                <span className="font-medium">{ticker.symbol}</span>
                <span className="text-xs text-slate-500 truncate ml-2 max-w-[100px]">{ticker.name}</span>
              </button>
            </li>
          ))}
        </ul>
      </div>
    </div>
  );
};

export default Sidebar;
