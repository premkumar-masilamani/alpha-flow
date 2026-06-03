import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Header from './Header';

describe('Header Component', () => {
    it('renders the header with the title and icon', () => {
        render(<Header />);
        
        // Check if application title is present
        const titleElement = screen.getByText('Alpha Flow');
        expect(titleElement).toBeInTheDocument();
        expect(titleElement).toHaveAttribute('id', 'app-title');
    });
});
