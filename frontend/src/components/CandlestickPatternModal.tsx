import React, { useEffect, useCallback } from 'react';
import { BookOpen, X } from 'lucide-react';
import CandlestickPatternEncyclopedia from './CandlestickPatternEncyclopedia';

interface CandlestickPatternModalProps {
    isOpen: boolean;
    onClose: () => void;
    initialPatternId?: string | null;
}

const CandlestickPatternModal: React.FC<CandlestickPatternModalProps> = ({
    isOpen,
    onClose,
    initialPatternId,
}) => {
    useEffect(() => {
        if (!isOpen) return;

        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape') {
                onClose();
            }
        };

        const originalOverflow = document.body.style.overflow;
        document.body.style.overflow = 'hidden';

        window.addEventListener('keydown', handleKeyDown);
        return () => {
            document.body.style.overflow = originalOverflow;
            window.removeEventListener('keydown', handleKeyDown);
        };
    }, [isOpen, onClose]);

    const handleBackdropClick = useCallback(
        (e: React.MouseEvent<HTMLDivElement>) => {
            if (e.target === e.currentTarget) {
                onClose();
            }
        },
        [onClose]
    );

    if (!isOpen) return null;

    return (
        <div
            role="dialog"
            aria-modal="true"
            aria-labelledby="csp-modal-title"
            className="fixed inset-0 z-50 flex items-center justify-center p-3 md:p-6 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200"
            onClick={handleBackdropClick}
        >
            <div
                className="relative w-full max-w-6xl h-[90vh] bg-slate-900 border border-slate-700 rounded-xl shadow-2xl flex flex-col overflow-hidden"
                onClick={(e) => e.stopPropagation()}
            >
                <div className="flex items-center justify-between px-5 py-3.5 bg-slate-950 border-b border-slate-800 select-none">
                    <div className="flex items-center gap-2.5">
                        <BookOpen className="text-blue-400" size={18} />
                        <h2 id="csp-modal-title" className="text-sm md:text-base font-bold text-white tracking-tight">
                            Candlestick Patterns Encyclopedia
                        </h2>
                        <span className="text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-400 font-mono font-medium">
                            68 Patterns
                        </span>
                    </div>
                    <div className="flex items-center gap-3">
                        <span className="text-xs text-slate-500 hidden sm:inline-block">
                            Press{' '}
                            <kbd className="px-1.5 py-0.5 rounded bg-slate-800 border border-slate-700 font-mono text-[10px] text-slate-300">
                                ESC
                            </kbd>{' '}
                            or click outside to close
                        </span>
                        <button
                            onClick={onClose}
                            aria-label="Close modal"
                            className="p-1.5 rounded-md text-slate-400 hover:text-white hover:bg-slate-800 transition-colors cursor-pointer"
                        >
                            <X size={18} />
                        </button>
                    </div>
                </div>
                <div className="flex-1 w-full h-full bg-slate-950 overflow-hidden relative flex flex-col min-h-0">
                    <CandlestickPatternEncyclopedia initialPatternId={initialPatternId} />
                </div>
            </div>
        </div>
    );
};

export default CandlestickPatternModal;
