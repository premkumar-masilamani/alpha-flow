import React, { useEffect, useState } from 'react';
import { AlertTriangle, Loader2, Trash2, X, CheckCircle2 } from 'lucide-react';
import type { Ticker } from '../services/api';

interface StaleTickersModalProps {
    isOpen: boolean;
    staleTickers: Ticker[];
    onClose: () => void;
    onDeleteTickerData: (symbol: string) => Promise<void>;
}

const StaleTickersModal: React.FC<StaleTickersModalProps> = ({
    isOpen,
    staleTickers,
    onClose,
    onDeleteTickerData
}) => {
    const [deletingSymbols, setDeletingSymbols] = useState<Set<string>>(new Set());
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [clearedSymbols, setClearedSymbols] = useState<string[]>([]);

    useEffect(() => {
        if (!isOpen) return;
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };
        window.addEventListener('keydown', handleKeyDown);
        return () => window.removeEventListener('keydown', handleKeyDown);
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const handleDelete = async (symbol: string) => {
        setErrorMessage(null);
        setDeletingSymbols((prev) => new Set(prev).add(symbol));
        try {
            await onDeleteTickerData(symbol);
            setClearedSymbols((prev) => (prev.includes(symbol) ? prev : [...prev, symbol]));
        } catch (error) {
            console.error(`Failed to delete data for ${symbol}:`, error);
            setErrorMessage(`Failed to clear data for ${symbol}. Please try again.`);
        } finally {
            setDeletingSymbols((prev) => {
                const next = new Set(prev);
                next.delete(symbol);
                return next;
            });
        }
    };

    return (
        <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-sm p-4"
            onClick={onClose}
            role="dialog"
            aria-modal="true"
            aria-labelledby="stale-tickers-modal-title"
        >
            <div
                className="bg-slate-900 border border-slate-700 rounded-xl shadow-2xl w-full max-w-lg overflow-hidden"
                onClick={(event) => event.stopPropagation()}
            >
                <div className="flex items-center justify-between px-5 py-4 border-b border-slate-800 bg-slate-950/60">
                    <div className="flex items-center gap-2.5">
                        <div className="p-2 rounded-lg bg-amber-500/15 text-amber-400 border border-amber-500/30">
                            <AlertTriangle size={18} />
                        </div>
                        <div>
                            <h2
                                id="stale-tickers-modal-title"
                                className="text-base font-bold text-white"
                            >
                                Obsolete Ticker Data Detected
                            </h2>
                            <p className="text-xs text-slate-400">
                                Corporate actions or historical price changes were detected.
                            </p>
                        </div>
                    </div>
                    <button
                        type="button"
                        onClick={onClose}
                        aria-label="Close modal"
                        className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                    >
                        <X size={18} />
                    </button>
                </div>

                <div className="p-5 space-y-4">
                    <p className="text-xs text-slate-300 leading-relaxed">
                        Deleting a ticker&apos;s data removes all stored prices, indicators, support &amp; resistance zones, and patterns so it can be cleanly repopulated on the next backend restart or scheduled sync.
                    </p>

                    {errorMessage && (
                        <div className="px-3 py-2 rounded-lg bg-rose-950/60 border border-rose-500/40 text-xs text-rose-300">
                            {errorMessage}
                        </div>
                    )}

                    {clearedSymbols.length > 0 && (
                        <div className="px-3 py-2 rounded-lg bg-emerald-950/50 border border-emerald-500/40 text-xs text-emerald-300 flex items-center gap-2">
                            <CheckCircle2 size={14} className="shrink-0" />
                            <span>
                                Cleared data for {clearedSymbols.join(', ')}. Restart the backend to repopulate.
                            </span>
                        </div>
                    )}

                    {staleTickers.length === 0 ? (
                        <div className="py-8 text-center text-sm text-slate-400">
                            All tickers are up to date.
                        </div>
                    ) : (
                        <ul className="divide-y divide-slate-800 border border-slate-800 rounded-lg overflow-hidden max-h-72 overflow-y-auto">
                            {staleTickers.map((ticker) => {
                                const isDeleting = deletingSymbols.has(ticker.symbol);
                                return (
                                    <li
                                        key={ticker.symbol}
                                        className="flex items-center justify-between gap-3 px-4 py-3 bg-slate-950/40 hover:bg-slate-800/40 transition-colors"
                                    >
                                        <div className="min-w-0">
                                            <div className="font-mono text-sm font-bold text-white">
                                                {ticker.symbol}
                                            </div>
                                            <div className="text-xs text-slate-400 truncate">
                                                {ticker.name}
                                            </div>
                                        </div>
                                        <button
                                            type="button"
                                            disabled={isDeleting}
                                            onClick={() => handleDelete(ticker.symbol)}
                                            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold bg-rose-600/90 hover:bg-rose-500 disabled:opacity-50 disabled:cursor-not-allowed text-white transition-colors shrink-0 cursor-pointer"
                                        >
                                            {isDeleting ? (
                                                <>
                                                    <Loader2 size={13} className="animate-spin" />
                                                    <span>Deleting...</span>
                                                </>
                                            ) : (
                                                <>
                                                    <Trash2 size={13} />
                                                    <span>Delete Data</span>
                                                </>
                                            )}
                                        </button>
                                    </li>
                                );
                            })}
                        </ul>
                    )}
                </div>

                <div className="px-5 py-3 border-t border-slate-800 bg-slate-950/60 flex justify-end">
                    <button
                        type="button"
                        onClick={onClose}
                        className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-slate-800 hover:bg-slate-700 text-slate-200 transition-colors cursor-pointer"
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default StaleTickersModal;
