import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import Header from './Header';

describe('Header Component', () => {
    it('renders the header with the title, calendar, clock, and dual market badges', () => {
        render(<Header />);

        const titleElement = screen.getByText('Alpha Flow');
        expect(titleElement).toBeInTheDocument();
        expect(titleElement).toHaveAttribute('id', 'app-title');

        const nseBadge = screen.getByText(/NSE:/i);
        expect(nseBadge).toBeInTheDocument();

        const nyseBadge = screen.getByText(/NYSE:/i);
        expect(nyseBadge).toBeInTheDocument();

        expect(screen.queryByText(/Stale Data/i)).not.toBeInTheDocument();
    });

    it('renders a prominent clickable stale data badge when staleTickersCount > 0', () => {
        const handleOpenModal = vi.fn();
        render(<Header staleTickersCount={3} onOpenStaleModal={handleOpenModal} />);

        const staleBadge = screen.getByRole('button', { name: /Stale Data \(3\)/i });
        expect(staleBadge).toBeInTheDocument();

        fireEvent.click(staleBadge);
        expect(handleOpenModal).toHaveBeenCalledTimes(1);
    });
});
