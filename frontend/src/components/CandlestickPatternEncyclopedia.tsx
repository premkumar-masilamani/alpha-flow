import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Search, X, Filter } from 'lucide-react';
import {
    CANDLESTICK_PATTERNS,
    CANDLESTICK_GROUPS,
    type CandlestickPatternDetail,
} from '../config/candlestickPatterns';

export interface CandlestickPatternEncyclopediaProps {
    initialPatternId?: string | null;
}

type GroupFilterType = 'all' | 'bullish-reversals' | 'bearish-reversals' | 'bullish-continuations' | 'bearish-continuations';

const CandlestickPatternEncyclopedia: React.FC<CandlestickPatternEncyclopediaProps> = ({
    initialPatternId,
}) => {
    const [searchQuery, setSearchQuery] = useState<string>('');
    const [selectedGroup, setSelectedGroup] = useState<GroupFilterType>('all');
    const [highlightedId, setHighlightedId] = useState<string | null>(initialPatternId || null);
    const scrollContainerRef = useRef<HTMLDivElement>(null);

    // Scroll to initial pattern if provided
    useEffect(() => {
        if (!initialPatternId) return;

        setHighlightedId(initialPatternId);

        const timer = setTimeout(() => {
            const targetElement = document.getElementById(`pattern-${initialPatternId}`);
            if (targetElement) {
                targetElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
            }
        }, 150);

        const clearHighlightTimer = setTimeout(() => {
            setHighlightedId(null);
        }, 3500);

        return () => {
            clearTimeout(timer);
            clearTimeout(clearHighlightTimer);
        };
    }, [initialPatternId]);

    // Group counts based on current search query
    const groupCounts = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();
        const counts: Record<string, number> = {
            all: 0,
            'bullish-reversals': 0,
            'bearish-reversals': 0,
            'bullish-continuations': 0,
            'bearish-continuations': 0,
        };

        for (const pattern of CANDLESTICK_PATTERNS) {
            let matches = true;
            if (query) {
                const searchableText = `${pattern.title} ${pattern.id} ${pattern.groupName} ${pattern.bars} bar bars ${pattern.structure} ${pattern.psychology} ${pattern.outcome}`.toLowerCase();
                matches = searchableText.includes(query);
            }
            if (matches) {
                counts.all += 1;
                counts[pattern.group] += 1;
            }
        }

        return counts;
    }, [searchQuery]);

    // Filter patterns
    const filteredPatternsByGroup = useMemo(() => {
        const query = searchQuery.trim().toLowerCase();

        const filtered = CANDLESTICK_PATTERNS.filter((pattern) => {
            if (selectedGroup !== 'all' && pattern.group !== selectedGroup) {
                return false;
            }
            if (!query) return true;
            const searchableText = `${pattern.title} ${pattern.id} ${pattern.groupName} ${pattern.bars} bar bars ${pattern.structure} ${pattern.psychology} ${pattern.outcome}`.toLowerCase();
            return searchableText.includes(query);
        });

        // Group into sections
        const grouped: Record<string, CandlestickPatternDetail[]> = {
            'bullish-reversals': [],
            'bearish-reversals': [],
            'bullish-continuations': [],
            'bearish-continuations': [],
        };

        for (const p of filtered) {
            grouped[p.group].push(p);
        }

        return grouped;
    }, [searchQuery, selectedGroup]);

    const totalFilteredCount = useMemo(() => {
        return Object.values(filteredPatternsByGroup).reduce((acc, list) => acc + list.length, 0);
    }, [filteredPatternsByGroup]);

    const handleResetFilters = () => {
        setSearchQuery('');
        setSelectedGroup('all');
    };

    const getGroupBadgeClass = (group: string) => {
        switch (group) {
            case 'bullish-reversals':
                return 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20';
            case 'bearish-reversals':
                return 'bg-rose-500/15 text-rose-400 border border-rose-500/20';
            case 'bullish-continuations':
                return 'bg-blue-500/15 text-blue-400 border border-blue-500/20';
            case 'bearish-continuations':
                return 'bg-amber-500/15 text-amber-400 border border-amber-500/20';
            default:
                return 'bg-slate-800 text-slate-300 border border-slate-700';
        }
    };

    return (
        <div className="flex flex-col h-full bg-slate-950 text-slate-200">
            {/* Header & Controls Bar */}
            <div className="shrink-0 bg-slate-900/90 border-b border-slate-800 px-4 md:px-6 py-4 backdrop-blur z-20 space-y-3">
                {/* Search Bar & Counter */}
                <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3">
                    <div className="relative flex-1">
                        <Search
                            className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate-500 pointer-events-none"
                            size={16}
                        />
                        <input
                            id="pattern-search"
                            type="text"
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            placeholder="Search patterns by name, structure, psychology, or outcome..."
                            className="w-full pl-10 pr-10 py-2 text-sm bg-slate-950 border border-slate-700 rounded-lg text-slate-200 placeholder-slate-500 focus:outline-none focus:border-blue-500 focus:ring-1 focus:ring-blue-500 transition-colors"
                        />
                        {searchQuery && (
                            <button
                                onClick={() => setSearchQuery('')}
                                aria-label="Clear search"
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white p-0.5 rounded transition-colors"
                            >
                                <X size={15} />
                            </button>
                        )}
                    </div>

                    {/* Results Counter */}
                    <div className="shrink-0 flex items-center justify-between sm:justify-end gap-2 text-xs font-mono text-slate-400 bg-slate-950 px-3 py-2 rounded-lg border border-slate-800">
                        <span>Showing</span>
                        <strong className="text-white font-bold">{totalFilteredCount}</strong>
                        <span>of {CANDLESTICK_PATTERNS.length} patterns</span>
                    </div>
                </div>

                {/* Filter Pills & Legend */}
                <div className="flex flex-wrap items-center justify-between gap-3 pt-1">
                    {/* Category Filter Pills */}
                    <div className="flex flex-wrap items-center gap-1.5" role="tablist" aria-label="Pattern category filters">
                        <button
                            role="tab"
                            aria-selected={selectedGroup === 'all'}
                            onClick={() => setSelectedGroup('all')}
                            className={`px-3 py-1 text-xs font-medium rounded-full transition-colors cursor-pointer ${
                                selectedGroup === 'all'
                                    ? 'bg-blue-600 text-white shadow-sm'
                                    : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200 border border-slate-700'
                            }`}
                        >
                            All ({groupCounts.all})
                        </button>

                        {CANDLESTICK_GROUPS.map((grp) => {
                            const isSelected = selectedGroup === grp.id;
                            const count = groupCounts[grp.id] || 0;
                            return (
                                <button
                                    key={grp.id}
                                    role="tab"
                                    aria-selected={isSelected}
                                    onClick={() => setSelectedGroup(grp.id as GroupFilterType)}
                                    className={`px-3 py-1 text-xs font-medium rounded-full transition-colors cursor-pointer ${
                                        isSelected
                                            ? 'bg-blue-600 text-white shadow-sm'
                                            : 'bg-slate-800 text-slate-400 hover:bg-slate-700 hover:text-slate-200 border border-slate-700'
                                    }`}
                                >
                                    {grp.title.replace(/^Part [IVX]+:\s*/, '')} ({count})
                                </button>
                            );
                        })}
                    </div>

                    {/* Color Legend */}
                    <div className="hidden lg:flex items-center gap-4 text-[11px] text-slate-400 bg-slate-950/60 px-3 py-1 rounded-full border border-slate-800">
                        <div className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-[#2ecc71]"></span>
                            <span>Bullish (Close &gt; Open)</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-[#e74c3c]"></span>
                            <span>Bearish (Close &lt; Open)</span>
                        </div>
                        <div className="flex items-center gap-1.5">
                            <span className="w-2 h-2 rounded-full bg-[#f1c40f]"></span>
                            <span>Doji (Close ≈ Open)</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Scrollable Patterns List */}
            <div
                ref={scrollContainerRef}
                className="flex-1 overflow-y-auto px-4 md:px-6 py-6 space-y-8 scroll-smooth"
            >
                {totalFilteredCount === 0 ? (
                    <div className="flex flex-col items-center justify-center py-16 text-center space-y-4">
                        <div className="w-12 h-12 rounded-full bg-slate-800 flex items-center justify-center text-slate-500">
                            <Filter size={24} />
                        </div>
                        <div className="space-y-1">
                            <h3 className="text-base font-bold text-slate-200">No matching patterns found</h3>
                            <p className="text-xs text-slate-400 max-w-sm">
                                {searchQuery
                                    ? `No patterns match "${searchQuery}" in ${selectedGroup === 'all' ? 'any category' : selectedGroup}.`
                                    : 'No patterns available in this category.'}
                            </p>
                        </div>
                        <button
                            onClick={handleResetFilters}
                            className="px-4 py-2 text-xs font-semibold bg-blue-600 hover:bg-blue-500 text-white rounded-lg transition-colors cursor-pointer shadow-md"
                        >
                            Reset All Filters
                        </button>
                    </div>
                ) : (
                    CANDLESTICK_GROUPS.map((group) => {
                        const patterns = filteredPatternsByGroup[group.id] || [];
                        if (patterns.length === 0) return null;

                        return (
                            <section
                                key={group.id}
                                id={`group-${group.id}`}
                                className="space-y-4 bg-slate-900/30 border border-slate-800/80 rounded-xl p-4 md:p-6"
                            >
                                {/* Group Header */}
                                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800 pb-3">
                                    <div>
                                        <div className="flex items-center gap-2.5">
                                            <h3 className="text-base md:text-lg font-bold text-white tracking-tight">
                                                {group.title}
                                            </h3>
                                            <span className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase tracking-wider ${group.badgeClass}`}>
                                                {patterns.length} {patterns.length === 1 ? 'Pattern' : 'Patterns'}
                                            </span>
                                        </div>
                                        <p className="text-xs text-slate-400 mt-1">
                                            {group.subtitle}
                                        </p>
                                    </div>
                                </div>

                                {/* Patterns Table */}
                                <div className="overflow-x-auto rounded-lg border border-slate-800 bg-slate-950">
                                    <table className="w-full text-left border-collapse text-sm">
                                        <thead>
                                            <tr className="bg-slate-900/90 text-slate-400 font-bold border-b border-slate-800 text-xs uppercase tracking-wider">
                                                <th scope="col" className="p-4 w-[170px] shrink-0 text-center">
                                                    Visual Pattern
                                                </th>
                                                <th scope="col" className="p-4">
                                                    Pattern Name &amp; Market Dynamics
                                                </th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-slate-800/60">
                                            {patterns.map((pattern) => {
                                                const isHighlighted = highlightedId === pattern.id;
                                                return (
                                                    <tr
                                                        key={pattern.id}
                                                        id={`pattern-${pattern.id}`}
                                                        className={`transition-all duration-300 ${
                                                            isHighlighted
                                                                ? 'bg-blue-950/40 ring-2 ring-blue-500/70'
                                                                : 'hover:bg-slate-900/40'
                                                        }`}
                                                    >
                                                        {/* Visual Pattern SVG */}
                                                        <td className="p-4 align-middle text-center shrink-0 w-[170px]">
                                                            <div className="flex items-center justify-center">
                                                                <svg
                                                                    className="w-[150px] h-[130px] rounded-lg shadow-md border border-slate-800 shrink-0"
                                                                    style={{ background: '#1e1e24' }}
                                                                    viewBox="0 0 150 130"
                                                                    aria-label={pattern.title}
                                                                    dangerouslySetInnerHTML={{ __html: pattern.svgMarkup }}
                                                                />
                                                            </div>
                                                        </td>

                                                        {/* Details Column */}
                                                        <td className="p-4 align-top">
                                                            <div className="space-y-2.5">
                                                                {/* Pattern Title & Badges */}
                                                                <div className="flex flex-wrap items-center gap-2">
                                                                    <h4 className="text-base font-extrabold text-white">
                                                                        {pattern.title}
                                                                    </h4>
                                                                    <span className={`text-[10px] px-2 py-0.5 rounded font-bold uppercase tracking-wider ${getGroupBadgeClass(pattern.group)}`}>
                                                                        {pattern.groupName}
                                                                    </span>
                                                                    <span className="bg-slate-800 text-slate-300 border border-slate-700 font-mono text-[10px] px-2 py-0.5 rounded font-medium">
                                                                        {pattern.bars} {pattern.bars === 1 ? 'Bar' : 'Bars'}
                                                                    </span>
                                                                </div>

                                                                {/* Dynamics */}
                                                                <div className="text-xs text-slate-300 space-y-1.5 leading-relaxed">
                                                                    <div>
                                                                        <span className="font-semibold text-slate-100 mr-1.5">Structure:</span>
                                                                        <span className="text-slate-300">{pattern.structure}</span>
                                                                    </div>
                                                                    <div>
                                                                        <span className="font-semibold text-slate-100 mr-1.5">Psychology:</span>
                                                                        <span className="text-slate-400">{pattern.psychology}</span>
                                                                    </div>
                                                                    <div>
                                                                        <span className="font-semibold text-slate-100 mr-1.5">Outcome:</span>
                                                                        <span className="text-slate-300">{pattern.outcome}</span>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                        </td>
                                                    </tr>
                                                );
                                            })}
                                        </tbody>
                                    </table>
                                </div>
                            </section>
                        );
                    })
                )}
            </div>
        </div>
    );
};

export default CandlestickPatternEncyclopedia;
