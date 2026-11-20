import { Component, type ErrorInfo, type ReactNode } from 'react';

export interface ErrorBoundaryProps {
    children: ReactNode;
    fallback?: ReactNode | ((props: { error: Error | null; resetError: () => void }) => ReactNode);
    onReset?: () => void;
}

export interface ErrorBoundaryState {
    hasError: boolean;
    error: Error | null;
    errorInfo: ErrorInfo | null;
}

export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = {
            hasError: false,
            error: null,
            errorInfo: null,
        };
    }

    static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
        return {
            hasError: true,
            error,
        };
    }

    componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
        console.error('ErrorBoundary caught an unhandled error:', error, errorInfo);
        this.setState({ errorInfo });
    }

    handleReset = (): void => {
        if (this.props.onReset) {
            try {
                this.props.onReset();
            } catch (resetErr) {
                console.error('ErrorBoundary onReset callback threw:', resetErr);
            }
        }
        this.setState({
            hasError: false,
            error: null,
            errorInfo: null,
        });
    };

    render(): ReactNode {
        if (this.state.hasError) {
            if (typeof this.props.fallback === 'function') {
                return this.props.fallback({
                    error: this.state.error,
                    resetError: this.handleReset,
                });
            }

            if (this.props.fallback) {
                return this.props.fallback;
            }

            return (
                <div
                    role="alert"
                    className="flex flex-col items-center justify-center p-6 text-center bg-slate-900 border border-slate-800 rounded-lg w-full h-full min-h-[280px] text-slate-100"
                >
                    <div className="w-12 h-12 mb-3 flex items-center justify-center rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20">
                        <svg
                            className="w-6 h-6"
                            fill="none"
                            stroke="currentColor"
                            viewBox="0 0 24 24"
                            xmlns="http://www.w3.org/2000/svg"
                        >
                            <path
                                strokeLinecap="round"
                                strokeLinejoin="round"
                                strokeWidth={2}
                                d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                            />
                        </svg>
                    </div>
                    <h3 className="text-base font-semibold text-slate-200 mb-1">
                        An unexpected error occurred
                    </h3>
                    <p className="text-xs text-slate-400 mb-4 max-w-md">
                        The view encountered an error while rendering. You can try recovering by clicking below.
                    </p>

                    {this.state.error && (
                        <div className="w-full max-w-md mb-4 text-left">
                            <span className="block text-[10px] uppercase font-bold tracking-wider text-slate-400 mb-1">
                                Error details
                            </span>
                            <pre className="text-xs font-mono text-rose-300 bg-slate-950 border border-slate-800 rounded p-3 overflow-x-auto whitespace-pre-wrap break-words max-h-40">
                                {this.state.error.message || String(this.state.error)}
                            </pre>
                        </div>
                    )}

                    <button
                        type="button"
                        onClick={this.handleReset}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white font-medium text-xs rounded-lg transition-colors cursor-pointer shadow-sm"
                    >
                        Reload / Try Again
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
