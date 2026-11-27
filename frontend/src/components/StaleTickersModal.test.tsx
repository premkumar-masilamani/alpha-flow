import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import StaleTickersModal from './StaleTickersModal';
import type { Ticker } from '../services/api';

describe('StaleTickersModal Component', () => {
    const staleTickers: Ticker[] = [
        { id: 1, symbol: 'NVDA', name: 'NVIDIA Corporation', type: 'STOCK', country: 'US', isStale: true },
        { id: 2, symbol: 'TCS.NS', name: 'Tata Consultancy Services', type: 'STOCK', country: 'IN', isStale: true }
    ];

    it('renders nothing when closed', () => {
        render(
            <StaleTickersModal
                isOpen={false}
                staleTickers={staleTickers}
                onClose={vi.fn()}
                onDeleteTickerData={vi.fn()}
            />
        );

        expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    });

    it('lists stale tickers and invokes onDeleteTickerData when Delete Data is clicked', async () => {
        const handleDelete = vi.fn().mockResolvedValue(undefined);
        render(
            <StaleTickersModal
                isOpen={true}
                staleTickers={staleTickers}
                onClose={vi.fn()}
                onDeleteTickerData={handleDelete}
            />
        );

        expect(screen.getByText('NVDA')).toBeInTheDocument();
        expect(screen.getByText('TCS.NS')).toBeInTheDocument();

        const deleteButtons = screen.getAllByRole('button', { name: /Delete Data/i });
        expect(deleteButtons).toHaveLength(2);

        fireEvent.click(deleteButtons[0]);

        await waitFor(() => {
            expect(handleDelete).toHaveBeenCalledWith('NVDA');
        });
        expect(screen.getByText(/Cleared data for NVDA/i)).toBeInTheDocument();
    });

    it('displays an error message without closing when deletion fails', async () => {
        const consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
        const handleDelete = vi.fn().mockRejectedValue(new Error('Network error'));
        const handleClose = vi.fn();

        render(
            <StaleTickersModal
                isOpen={true}
                staleTickers={[staleTickers[0]]}
                onClose={handleClose}
                onDeleteTickerData={handleDelete}
            />
        );

        fireEvent.click(screen.getByRole('button', { name: /Delete Data/i }));

        await waitFor(() => {
            expect(screen.getByText(/Failed to clear data for NVDA/i)).toBeInTheDocument();
        });
        expect(handleClose).not.toHaveBeenCalled();
        consoleErrorSpy.mockRestore();
    });

    it('shows empty state when no stale tickers remain and closes on Escape or button click', () => {
        const handleClose = vi.fn();
        render(
            <StaleTickersModal
                isOpen={true}
                staleTickers={[]}
                onClose={handleClose}
                onDeleteTickerData={vi.fn()}
            />
        );

        expect(screen.getByText('All tickers are up to date.')).toBeInTheDocument();

        fireEvent.keyDown(window, { key: 'Escape' });
        expect(handleClose).toHaveBeenCalledTimes(1);

        fireEvent.click(screen.getByRole('button', { name: 'Close' }));
        expect(handleClose).toHaveBeenCalledTimes(2);
    });
});
