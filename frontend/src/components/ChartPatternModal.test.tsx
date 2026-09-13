import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi } from 'vitest';
import ChartPatternModal from './ChartPatternModal';

describe('ChartPatternModal', () => {
    it('does not render when isOpen is false', () => {
        const { container } = render(
            <ChartPatternModal isOpen={false} onClose={vi.fn()} />
        );
        expect(container).toBeEmptyDOMElement();
    });

    it('renders modal dialog and native encyclopedia when isOpen is true', () => {
        render(
            <ChartPatternModal isOpen={true} onClose={vi.fn()} initialPatternId="double-top" />
        );

        expect(screen.getByRole('dialog')).toBeInTheDocument();
        expect(screen.getByText('Chart Patterns Encyclopedia')).toBeInTheDocument();
        expect(screen.getByPlaceholderText(/Search chart patterns/i)).toBeInTheDocument();
        expect(screen.getByText('Double Top')).toBeInTheDocument();
    });

    it('calls onClose when close button is clicked', () => {
        const handleClose = vi.fn();
        render(<ChartPatternModal isOpen={true} onClose={handleClose} />);

        const closeBtn = screen.getByLabelText('Close modal');
        fireEvent.click(closeBtn);
        expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when Escape key is pressed', () => {
        const handleClose = vi.fn();
        render(<ChartPatternModal isOpen={true} onClose={handleClose} />);

        fireEvent.keyDown(window, { key: 'Escape' });
        expect(handleClose).toHaveBeenCalledTimes(1);
    });

    it('calls onClose when clicking outside on the backdrop', () => {
        const handleClose = vi.fn();
        render(<ChartPatternModal isOpen={true} onClose={handleClose} />);

        const dialog = screen.getByRole('dialog');
        fireEvent.click(dialog);
        expect(handleClose).toHaveBeenCalledTimes(1);
    });
});
