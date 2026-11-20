import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
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
    });
});
