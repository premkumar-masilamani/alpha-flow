import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import IndicatorControls from './IndicatorControls';
import type { IndicatorConfig } from '../services/api';

describe('IndicatorControls Component', () => {
    const mockConfigs: IndicatorConfig[] = [
        { timeframe: 'DAILY', type: 'EMA', source: 'CLOSE', params: 'period=5', label: 'EMA(5)' },
        { timeframe: 'DAILY', type: 'SMA', source: 'CLOSE', params: 'period=20', label: 'SMA(20)' },
        { timeframe: 'DAILY', type: 'RSI', source: 'CLOSE', params: 'period=14', label: 'RSI(14)' }
    ];

    it('returns null when configs is empty', () => {
        const { container } = render(
            <IndicatorControls
                configs={[]}
                enabled={new Set()}
                onToggle={vi.fn()}
            />
        );
        expect(container.firstChild).toBeNull();
    });

    it('renders indicators with formatted labels', () => {
        render(
            <IndicatorControls
                configs={mockConfigs}
                enabled={new Set()}
                onToggle={vi.fn()}
            />
        );

        expect(screen.getByText('Indicators')).toBeInTheDocument();
        expect(screen.getByText('EMA (5)')).toBeInTheDocument();
        expect(screen.getByText('SMA (20)')).toBeInTheDocument();
        expect(screen.getByText('RSI (14)')).toBeInTheDocument();
    });

    it('sets aria-pressed and active styles on enabled indicators', () => {
        const enabledKeys = new Set(['EMA|CLOSE|period=5', 'RSI|CLOSE|period=14']);
        render(
            <IndicatorControls
                configs={mockConfigs}
                enabled={enabledKeys}
                onToggle={vi.fn()}
            />
        );

        const emaButton = screen.getByText('EMA (5)');
        const smaButton = screen.getByText('SMA (20)');
        const rsiButton = screen.getByText('RSI (14)');

        expect(emaButton).toHaveAttribute('aria-pressed', 'true');
        expect(smaButton).toHaveAttribute('aria-pressed', 'false');
        expect(rsiButton).toHaveAttribute('aria-pressed', 'true');

        expect(emaButton).toHaveClass('bg-blue-600', 'border-blue-500', 'text-white');
        expect(smaButton).toHaveClass('bg-slate-900', 'border-slate-700', 'text-slate-400');
    });

    it('calls onToggle with the indicator key when clicked', () => {
        const handleToggle = vi.fn();
        render(
            <IndicatorControls
                configs={mockConfigs}
                enabled={new Set()}
                onToggle={handleToggle}
            />
        );

        const smaButton = screen.getByText('SMA (20)');
        fireEvent.click(smaButton);

        expect(handleToggle).toHaveBeenCalledTimes(1);
        expect(handleToggle).toHaveBeenCalledWith('SMA|CLOSE|period=20');
    });
});
