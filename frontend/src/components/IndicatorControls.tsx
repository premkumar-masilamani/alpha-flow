import React from 'react';
import {type IndicatorConfig, indicatorKey} from '../services/api';

interface IndicatorControlsProps {
    configs: IndicatorConfig[];
    enabled: Set<string>;
    onToggle: (key: string) => void;
}

// Toggle pills for the daily indicators, built from the discovery endpoint so the UI follows config.
const IndicatorControls: React.FC<IndicatorControlsProps> = ({configs, enabled, onToggle}) => {
    if (configs.length === 0) return null;

    const formatLabel = (label: string): string => {
        return label.replace(/([A-Za-z]+)\(([^)]+)\)/, "$1 ($2)");
    };

    return (
        <div className="flex flex-wrap items-center gap-2">
            <span className="text-xs font-bold text-slate-400 mr-1">Indicators</span>
            {configs.map((config) => {
                const key = indicatorKey(config);
                const on = enabled.has(key);
                return (
                    <button
                        key={key}
                        onClick={() => onToggle(key)}
                        aria-pressed={on}
                        className={`px-2.5 py-1 text-xs font-semibold rounded-md border transition-colors ${
                            on
                                ? 'bg-blue-600 border-blue-500 text-white'
                                : 'bg-slate-900 border-slate-700 text-slate-400 hover:text-slate-200 hover:border-slate-600'
                        }`}
                    >
                        {formatLabel(config.label)}
                    </button>
                );
            })}
        </div>
    );
};

export default IndicatorControls;
