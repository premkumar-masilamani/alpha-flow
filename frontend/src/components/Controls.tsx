import React from 'react';
import { Eye, EyeOff } from 'lucide-react';

export interface IndicatorConfig {
  id: string;
  label: string;
  color: string;
  visible: boolean;
}

interface ControlsProps {
  indicators: IndicatorConfig[];
  onToggle: (id: string) => void;
}

const Controls: React.FC<ControlsProps> = ({ indicators, onToggle }) => {
  return (
    <div className="flex flex-wrap gap-2 p-4 bg-slate-800 border-b border-slate-700">
      {indicators.map((indicator) => (
        <button
          key={indicator.id}
          onClick={() => onToggle(indicator.id)}
          className={`flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium transition-all ${
            indicator.visible
              ? 'bg-slate-700 text-white shadow-sm ring-1 ring-slate-600'
              : 'bg-slate-900 text-slate-500 opacity-60'
          }`}
        >
          <div
            className="w-3 h-3 rounded-full"
            style={{ backgroundColor: indicator.visible ? indicator.color : '#475569' }}
          />
          {indicator.label}
          {indicator.visible ? <Eye size={14} /> : <EyeOff size={14} />}
        </button>
      ))}
    </div>
  );
};

export default Controls;
