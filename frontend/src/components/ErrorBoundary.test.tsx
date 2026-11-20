import { useState } from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import ErrorBoundary from './ErrorBoundary';

const GoodChild = () => <div>Normal Content</div>;

const ThrowingChild = ({ shouldThrow, message }: { shouldThrow: boolean; message?: string }) => {
    if (shouldThrow) {
        throw new Error(message || 'Simulated child render error');
    }
    return <div>Normal Content</div>;
};

describe('ErrorBoundary Component', () => {
    let consoleErrorSpy: ReturnType<typeof vi.spyOn>;

    beforeEach(() => {
        consoleErrorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        consoleErrorSpy.mockRestore();
    });

    it('renders child components normally when no error occurs', () => {
        render(
            <ErrorBoundary>
                <GoodChild />
            </ErrorBoundary>
        );

        expect(screen.getByText('Normal Content')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
        expect(consoleErrorSpy).not.toHaveBeenCalled();
    });

    it('catches render error, logs it to console.error, and displays dark-slate fallback UI', () => {
        render(
            <ErrorBoundary>
                <ThrowingChild shouldThrow={true} message="Crash in widget" />
            </ErrorBoundary>
        );

        const alertContainer = screen.getByRole('alert');
        expect(alertContainer).toBeInTheDocument();
        expect(alertContainer.className).toContain('bg-slate-900');
        expect(alertContainer.className).toContain('border-slate-800');
        expect(screen.getByText(/An unexpected error occurred/i)).toBeInTheDocument();
        expect(screen.getByText('Crash in widget')).toBeInTheDocument();
        expect(consoleErrorSpy).toHaveBeenCalled();
    });

    it('renders a "Reload / Try Again" button and resets error state when clicked', () => {
        const onResetMock = vi.fn();

        const StatefulParent = () => {
            const [shouldThrow, setShouldThrow] = useState(true);
            return (
                <ErrorBoundary
                    onReset={() => {
                        onResetMock();
                        setShouldThrow(false);
                    }}
                >
                    <ThrowingChild shouldThrow={shouldThrow} message="Transient failure" />
                </ErrorBoundary>
            );
        };

        render(<StatefulParent />);

        expect(screen.getByText('Transient failure')).toBeInTheDocument();
        const reloadBtn = screen.getByRole('button', { name: /Reload \/ Try Again/i });
        expect(reloadBtn).toBeInTheDocument();

        fireEvent.click(reloadBtn);

        expect(onResetMock).toHaveBeenCalledTimes(1);
        expect(screen.getByText('Normal Content')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });

    it('renders custom ReactNode fallback if provided', () => {
        render(
            <ErrorBoundary fallback={<div data-testid="custom-fallback">Custom Error Message</div>}>
                <ThrowingChild shouldThrow={true} />
            </ErrorBoundary>
        );

        expect(screen.getByTestId('custom-fallback')).toBeInTheDocument();
        expect(screen.getByText('Custom Error Message')).toBeInTheDocument();
    });

    it('renders custom function fallback with error details and reset callback', () => {
        const customFallback = vi.fn(({ error, resetError }: { error: Error | null; resetError: () => void }) => (
            <div>
                <span>Custom: {error?.message}</span>
                <button type="button" onClick={resetError}>Custom Retry</button>
            </div>
        ));

        render(
            <ErrorBoundary fallback={customFallback}>
                <ThrowingChild shouldThrow={true} message="Function fallback error" />
            </ErrorBoundary>
        );

        expect(screen.getByText('Custom: Function fallback error')).toBeInTheDocument();
        expect(customFallback).toHaveBeenCalled();
    });
});
