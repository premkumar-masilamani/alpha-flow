export interface ChartPatternConfig {
    patternType: string;
    shortName: string;
    displayName: string;
    category: 'reversal' | 'continuation';
    sentiment: 'bullish' | 'bearish';
    color: string;
    description: string;
}

export interface ChartPatternDetail {
    id: string;
    patternType: string;
    title: string;
    shortName: string;
    category: 'reversal' | 'continuation';
    sentiment: 'bullish' | 'bearish';
    group: 'bullish-reversals' | 'bearish-reversals' | 'bullish-continuations' | 'bearish-continuations';
    groupName: string;
    color: string;
    description: string;
    structure: string;
    psychology: string;
    outcome: string;
    targetRule: string;
    svgMarkup: string;
}

export interface ChartPatternGroup {
    id: 'bullish-reversals' | 'bearish-reversals' | 'bullish-continuations' | 'bearish-continuations';
    title: string;
    subtitle: string;
    badgeClass: string;
}

export const CHART_PATTERN_GROUPS: ChartPatternGroup[] = [
    {
        id: 'bullish-reversals',
        title: 'Part I: Bullish Reversals',
        subtitle: 'Formed after a downtrend; indicates seller exhaustion and an impending upward trend reversal.',
        badgeClass: 'bg-emerald-500/15 text-emerald-400 border border-emerald-500/20',
    },
    {
        id: 'bearish-reversals',
        title: 'Part II: Bearish Reversals',
        subtitle: 'Formed after an uptrend; indicates buyer exhaustion and institutional distribution before a downward reversal.',
        badgeClass: 'bg-rose-500/15 text-rose-400 border border-rose-500/20',
    },
    {
        id: 'bullish-continuations',
        title: 'Part III: Bullish Continuations',
        subtitle: 'Formed during an established uptrend; indicates consolidation before upward resumption.',
        badgeClass: 'bg-blue-500/15 text-blue-400 border border-blue-500/20',
    },
    {
        id: 'bearish-continuations',
        title: 'Part IV: Bearish Continuations',
        subtitle: 'Formed during an established downtrend; indicates temporary pause before downward resumption.',
        badgeClass: 'bg-amber-500/15 text-amber-400 border border-amber-500/20',
    },
];

export const CHART_PATTERNS: ChartPatternDetail[] = [
    {
        id: 'double-top',
        patternType: 'DOUBLE_TOP',
        title: 'Double Top',
        shortName: 'DTP',
        category: 'reversal',
        sentiment: 'bearish',
        group: 'bearish-reversals',
        groupName: 'Bearish Reversal',
        color: '#ef4444',
        description: 'Bearish reversal pattern formed by two consecutive peaks at roughly the same level with an intermediate trough.',
        structure: 'Two distinct price peaks at approximately the same resistance level separated by a moderate trough. Neckline is drawn horizontally across the intermediate valley low.',
        psychology: 'Buyers attempt to push prices to new highs twice, but encounter fierce institutional distribution at resistance. A break below the neckline confirms seller dominance.',
        outcome: 'Signal of an impending bearish trend reversal. Entry triggers upon a decisive close below the neckline with heightened volume.',
        targetRule: 'Target is calculated by measuring the vertical height from the peaks to the neckline, subtracted from the neckline breakout level.',
        svgMarkup: '<line x1="20" y1="90" x2="135" y2="90" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="35" x2="130" y2="35" stroke="#ef4444" stroke-opacity="0.4" stroke-dasharray="2,2" stroke-width="1"/><polyline points="25,95 45,35 75,90 105,35 125,90 135,115" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="45" cy="35" r="3" fill="#ef4444"/><circle cx="75" cy="90" r="3" fill="#94a3b8"/><circle cx="105" cy="35" r="3" fill="#ef4444"/><line x1="125" y1="90" x2="125" y2="120" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="125,124 121,116 129,116" fill="#38bdf8"/>',
    },
    {
        id: 'double-bottom',
        patternType: 'DOUBLE_BOTTOM',
        title: 'Double Bottom',
        shortName: 'DBM',
        category: 'reversal',
        sentiment: 'bullish',
        group: 'bullish-reversals',
        groupName: 'Bullish Reversal',
        color: '#22c55e',
        description: 'Bullish reversal pattern formed by two consecutive troughs at roughly the same level with an intermediate peak.',
        structure: 'Two distinct troughs at approximately the same support level separated by an intermediate peak. Neckline is drawn horizontally across the intermediate peak high.',
        psychology: 'Sellers attempt to drive prices lower twice, but encounter aggressive buying support. A break above the neckline confirms buyers taking control.',
        outcome: 'Signal of an impending bullish trend reversal. Entry triggers upon a decisive close above the neckline with strong volume.',
        targetRule: 'Target is calculated by measuring the vertical height from the neckline to the troughs, added to the neckline breakout level.',
        svgMarkup: '<line x1="20" y1="95" x2="130" y2="95" stroke="#2ecc71" stroke-opacity="0.4" stroke-dasharray="2,2" stroke-width="1"/><line x1="20" y1="40" x2="135" y2="40" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,35 45,95 75,40 105,95 125,40 135,15" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="45" cy="95" r="3" fill="#2ecc71"/><circle cx="75" cy="40" r="3" fill="#94a3b8"/><circle cx="105" cy="95" r="3" fill="#2ecc71"/><line x1="125" y1="40" x2="125" y2="10" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="125,6 121,14 129,14" fill="#38bdf8"/>',
    },
    {
        id: 'triple-top',
        patternType: 'TRIPLE_TOP',
        title: 'Triple Top',
        shortName: 'TTP',
        category: 'reversal',
        sentiment: 'bearish',
        group: 'bearish-reversals',
        groupName: 'Bearish Reversal',
        color: '#ef4444',
        description: 'Bearish reversal pattern consisting of three distinct peaks at approximately the same resistance price level.',
        structure: 'Three successive peaks testing a key resistance ceiling separated by two intervening troughs. Neckline connects the two trough lows.',
        psychology: 'Bulls repeatedly fail on three separate rallies to conquer overhead resistance, exhausting buying capital. Breakdown below the neckline unleashes strong liquidation.',
        outcome: 'Strong bearish reversal pattern. Confirmation occurs when price breaks down through the neckline support with expanding volume.',
        targetRule: 'Target is equal to the neckline breakout price minus the maximum vertical distance between peaks and the neckline.',
        svgMarkup: '<line x1="15" y1="35" x2="135" y2="35" stroke="#ef4444" stroke-opacity="0.4" stroke-dasharray="2,2" stroke-width="1"/><line x1="15" y1="90" x2="135" y2="90" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="20,95 38,35 58,90 78,35 98,90 118,35 130,90 136,115" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="38" cy="35" r="3" fill="#ef4444"/><circle cx="78" cy="35" r="3" fill="#ef4444"/><circle cx="118" cy="35" r="3" fill="#ef4444"/><line x1="130" y1="90" x2="130" y2="120" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="130,124 126,116 134,116" fill="#38bdf8"/>',
    },
    {
        id: 'triple-bottom',
        patternType: 'TRIPLE_BOTTOM',
        title: 'Triple Bottom',
        shortName: 'TBM',
        category: 'reversal',
        sentiment: 'bullish',
        group: 'bullish-reversals',
        groupName: 'Bullish Reversal',
        color: '#22c55e',
        description: 'Bullish reversal pattern consisting of three distinct troughs testing the same support level.',
        structure: 'Three successive troughs testing a key support floor separated by two intervening peaks. Neckline connects the two peak highs.',
        psychology: 'Bears test the support floor three times but cannot push price lower. Accumulation by institutional buyers drives a breakout above resistance.',
        outcome: 'Strong bullish reversal pattern. Confirmed on a high-volume breakout above the neckline resistance level.',
        targetRule: 'Target is equal to the neckline breakout price plus the vertical distance between the troughs and the neckline.',
        svgMarkup: '<line x1="15" y1="95" x2="135" y2="95" stroke="#2ecc71" stroke-opacity="0.4" stroke-dasharray="2,2" stroke-width="1"/><line x1="15" y1="40" x2="135" y2="40" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="20,35 38,95 58,40 78,95 98,40 118,95 130,40 136,15" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="38" cy="95" r="3" fill="#2ecc71"/><circle cx="78" cy="95" r="3" fill="#2ecc71"/><circle cx="118" cy="95" r="3" fill="#2ecc71"/><line x1="130" y1="40" x2="130" y2="10" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="130,6 126,14 134,14" fill="#38bdf8"/>',
    },
    {
        id: 'head-and-shoulders',
        patternType: 'HEAD_AND_SHOULDERS',
        title: 'Head and Shoulders',
        shortName: 'HNS',
        category: 'reversal',
        sentiment: 'bearish',
        group: 'bearish-reversals',
        groupName: 'Bearish Reversal',
        color: '#ef4444',
        description: 'Major bearish reversal pattern with a higher peak (head) between two roughly equal lower peaks (shoulders).',
        structure: 'Left shoulder peak, followed by a higher central peak (head), followed by a lower right shoulder peak. Neckline connects the two intervening troughs.',
        psychology: 'The trend transitions from higher highs to a lower high (right shoulder), signaling bull exhaustion. Breakdown below the neckline triggers heavy liquidation.',
        outcome: 'One of the most reliable bearish reversal patterns. Confirmed on a decisive close below the neckline support.',
        targetRule: 'Target is calculated by projecting the vertical distance from the peak of the head to the neckline downward from the breakout point.',
        svgMarkup: '<line x1="15" y1="90" x2="135" y2="90" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="15,95 35,55 55,90 75,25 95,90 115,55 125,90 135,118" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="35" cy="55" r="3" fill="#ef4444"/><circle cx="75" cy="25" r="3.5" fill="#ef4444"/><circle cx="115" cy="55" r="3" fill="#ef4444"/><circle cx="55" cy="90" r="2.5" fill="#94a3b8"/><circle cx="95" cy="90" r="2.5" fill="#94a3b8"/><line x1="125" y1="90" x2="125" y2="120" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="125,124 121,116 129,116" fill="#38bdf8"/>',
    },
    {
        id: 'inverse-head-and-shoulders',
        patternType: 'INVERSE_HEAD_AND_SHOULDERS',
        title: 'Inverse Head and Shoulders',
        shortName: 'IHS',
        category: 'reversal',
        sentiment: 'bullish',
        group: 'bullish-reversals',
        groupName: 'Bullish Reversal',
        color: '#22c55e',
        description: 'Major bullish reversal pattern with a lower trough (head) between two roughly equal higher troughs (shoulders).',
        structure: 'Left shoulder trough, followed by a deeper central trough (head), followed by a higher right shoulder trough. Neckline connects the intervening peaks.',
        psychology: 'Sellers fail to produce a new low at the right shoulder, showing supply exhaustion. Buyers seize momentum and push price above neckline resistance.',
        outcome: 'One of the most reliable bullish reversal patterns. Confirmed when price closes decisively above the neckline.',
        targetRule: 'Target is calculated by measuring the vertical distance from the bottom of the head to the neckline and projecting it upward from the breakout point.',
        svgMarkup: '<line x1="15" y1="40" x2="135" y2="40" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="15,35 35,75 55,40 75,105 95,40 115,75 125,40 135,12" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="35" cy="75" r="3" fill="#2ecc71"/><circle cx="75" cy="105" r="3.5" fill="#2ecc71"/><circle cx="115" cy="75" r="3" fill="#2ecc71"/><circle cx="55" cy="40" r="2.5" fill="#94a3b8"/><circle cx="95" cy="40" r="2.5" fill="#94a3b8"/><line x1="125" y1="40" x2="125" y2="10" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="125,6 121,14 129,14" fill="#38bdf8"/>',
    },
    {
        id: 'rising-wedge',
        patternType: 'RISING_WEDGE',
        title: 'Rising Wedge',
        shortName: 'RWG',
        category: 'reversal',
        sentiment: 'bearish',
        group: 'bearish-reversals',
        groupName: 'Bearish Reversal',
        color: '#ef4444',
        description: 'Bearish pattern where price narrows between upward-sloping converging support and resistance lines.',
        structure: 'Price makes higher highs and higher lows, but highs are rising more slowly than lows, causing upper resistance and lower support to converge upward.',
        psychology: 'Upward momentum wanes as buyers require increasingly higher volume to push marginal new highs. As buying momentum dries up, support fails precipitously.',
        outcome: 'Bearish breakdown signal. Confirmed when price breaches the lower ascending support trendline.',
        targetRule: 'Target is typically projected to the origin or base level of the wedge formation where the pattern began.',
        svgMarkup: '<line x1="20" y1="65" x2="125" y2="25" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="105" x2="125" y2="45" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,100 45,55 65,85 85,42 105,68 115,30 120,50 132,105" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><line x1="120" y1="50" x2="132" y2="100" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="134,106 127,100 134,94" fill="#38bdf8"/>',
    },
    {
        id: 'falling-wedge',
        patternType: 'FALLING_WEDGE',
        title: 'Falling Wedge',
        shortName: 'FWG',
        category: 'reversal',
        sentiment: 'bullish',
        group: 'bullish-reversals',
        groupName: 'Bullish Reversal',
        color: '#22c55e',
        description: 'Bullish pattern where price narrows between downward-sloping converging resistance and support lines.',
        structure: 'Price makes lower lows and lower highs, but lows are falling more slowly than highs, causing upper resistance and lower support lines to converge downward.',
        psychology: 'Selling momentum decelerates as bears exhaust their selling pressure. The compression leads to explosive demand once overhead resistance breaks.',
        outcome: 'Bullish breakout signal. Confirmed when price breaches the upper descending resistance trendline.',
        targetRule: 'Target is typically projected to the highest point or base origin of the wedge formation.',
        svgMarkup: '<line x1="20" y1="25" x2="125" y2="85" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="65" x2="125" y2="105" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,30 45,75 65,45 85,88 105,62 115,100 120,80 132,25" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><line x1="120" y1="80" x2="132" y2="30" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="134,24 127,30 134,36" fill="#38bdf8"/>',
    },
    {
        id: 'ascending-triangle',
        patternType: 'ASCENDING_TRIANGLE',
        title: 'Ascending Triangle',
        shortName: 'AST',
        category: 'continuation',
        sentiment: 'bullish',
        group: 'bullish-continuations',
        groupName: 'Bullish Continuation',
        color: '#22c55e',
        description: 'Bullish continuation pattern formed by a flat horizontal resistance line and a rising ascending support line.',
        structure: 'Horizontal upper resistance line with multiple touches at identical price levels, coupled with an ascending lower trendline with higher lows.',
        psychology: 'Buyers are steadily willing to purchase at progressively higher prices on dips, while sellers hold firm at a fixed ceiling. Eventually, sellers are absorbed.',
        outcome: 'Strong bullish continuation signal upon an upside breakout above horizontal resistance with expanding volume.',
        targetRule: 'Target is calculated by adding the widest vertical height of the triangle to the breakout price level.',
        svgMarkup: '<line x1="20" y1="35" x2="130" y2="35" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="105" x2="125" y2="35" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,100 45,35 65,80 85,35 105,60 118,35 130,15" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="45" cy="35" r="3" fill="#ef4444"/><circle cx="85" cy="35" r="3" fill="#ef4444"/><circle cx="25" cy="100" r="3" fill="#2ecc71"/><circle cx="65" cy="80" r="3" fill="#2ecc71"/><circle cx="105" cy="60" r="3" fill="#2ecc71"/><line x1="118" y1="35" x2="130" y2="12" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="132,8 125,15 133,18" fill="#38bdf8"/>',
    },
    {
        id: 'descending-triangle',
        patternType: 'DESCENDING_TRIANGLE',
        title: 'Descending Triangle',
        shortName: 'DST',
        category: 'continuation',
        sentiment: 'bearish',
        group: 'bearish-continuations',
        groupName: 'Bearish Continuation',
        color: '#ef4444',
        description: 'Bearish continuation pattern formed by a flat horizontal support line and a falling descending resistance line.',
        structure: 'Horizontal lower support line with multiple touches at the same level, combined with a descending upper resistance trendline with lower highs.',
        psychology: 'Sellers become increasingly aggressive, selling at lower prices on every rally, while buyers defend a static support floor until bids are fully exhausted.',
        outcome: 'Bearish continuation signal upon a downside breakdown below horizontal support with increased volume.',
        targetRule: 'Target is calculated by subtracting the widest vertical height of the triangle from the breakdown level.',
        svgMarkup: '<line x1="20" y1="95" x2="130" y2="95" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="25" x2="125" y2="95" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,30 45,95 65,50 85,95 105,70 118,95 130,115" fill="none" stroke="#ef4444" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="45" cy="95" r="3" fill="#2ecc71"/><circle cx="85" cy="95" r="3" fill="#2ecc71"/><circle cx="25" cy="30" r="3" fill="#ef4444"/><circle cx="65" cy="50" r="3" fill="#ef4444"/><circle cx="105" cy="70" r="3" fill="#ef4444"/><line x1="118" y1="95" x2="130" y2="118" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="132,122 125,115 133,112" fill="#38bdf8"/>',
    },
    {
        id: 'symmetrical-triangle',
        patternType: 'SYMMETRICAL_TRIANGLE',
        title: 'Symmetrical Triangle',
        shortName: 'SYT',
        category: 'continuation',
        sentiment: 'bullish',
        group: 'bullish-continuations',
        groupName: 'Bullish Continuation',
        color: '#3b82f6',
        description: 'Consolidation pattern characterized by converging trendlines connecting lower highs and higher lows.',
        structure: 'A descending upper trendline and an ascending lower trendline converging toward an apex, showing volatility compression.',
        psychology: 'Bulls and bears are in temporary equilibrium with volatility contracting. Breakout occurs when one side overcomes the other with strong conviction.',
        outcome: 'Continuation breakout in the direction of the prevailing trend (typically bullish in an uptrend).',
        targetRule: 'Target is projected by adding the maximum vertical height of the triangle base to the breakout point.',
        svgMarkup: '<line x1="20" y1="25" x2="125" y2="65" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><line x1="20" y1="105" x2="125" y2="65" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><polyline points="25,30 45,95 65,45 85,82 105,58 115,70 128,45 135,25" fill="none" stroke="#3b82f6" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><line x1="120" y1="60" x2="135" y2="25" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="137,20 129,26 136,31" fill="#38bdf8"/>',
    },
    {
        id: 'cup-and-handle',
        patternType: 'CUP_AND_HANDLE',
        title: 'Cup and Handle',
        shortName: 'CPH',
        category: 'continuation',
        sentiment: 'bullish',
        group: 'bullish-continuations',
        groupName: 'Bullish Continuation',
        color: '#22c55e',
        description: 'Bullish continuation pattern showing a rounded U-shaped cup recovery followed by a slight downward drift (handle).',
        structure: 'A rounded "U" shaped trough with equal left and right rim resistance peaks, followed by a shallow downward consolidation channel (the handle).',
        psychology: 'Gradual institutional accumulation forms the rounded base. The handle shakes out weak hands on lower volume before a surge through the rim.',
        outcome: 'Powerful bullish continuation breakout when price clears the rim resistance line with surging volume.',
        targetRule: 'Target is measured by calculating the depth of the cup from the rim level to the lowest bottom and adding it to the breakout level.',
        svgMarkup: '<line x1="20" y1="35" x2="135" y2="35" stroke="#94a3b8" stroke-dasharray="3,3" stroke-width="1.5"/><path d="M 25 35 C 35 105, 85 105, 95 35 L 105 50 L 115 45 L 125 35 L 135 15" fill="none" stroke="#2ecc71" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="25" cy="35" r="3" fill="#94a3b8"/><circle cx="95" cy="35" r="3" fill="#94a3b8"/><circle cx="60" cy="88" r="3" fill="#2ecc71"/><line x1="125" y1="35" x2="135" y2="15" stroke="#38bdf8" stroke-dasharray="2,2" stroke-width="1.5"/><polygon points="137,11 129,17 136,22" fill="#38bdf8"/>',
    },
];

export const CHART_PATTERNS_CONFIG: Record<string, ChartPatternConfig> = CHART_PATTERNS.reduce(
    (acc, pattern) => {
        acc[pattern.patternType] = {
            patternType: pattern.patternType,
            shortName: pattern.shortName,
            displayName: pattern.title,
            category: pattern.category,
            sentiment: pattern.sentiment,
            color: pattern.color,
            description: pattern.description,
        };
        return acc;
    },
    {} as Record<string, ChartPatternConfig>
);

const norm = (s: string): string => s.toLowerCase().replace(/[^a-z0-9]/g, '');

const LOOKUP_MAP = new Map<string, ChartPatternDetail>();

for (const p of CHART_PATTERNS) {
    LOOKUP_MAP.set(p.id, p);
    LOOKUP_MAP.set(norm(p.id), p);
    LOOKUP_MAP.set(norm(p.patternType), p);
    LOOKUP_MAP.set(norm(p.title), p);
    LOOKUP_MAP.set(norm(p.shortName), p);
}

export function getChartPatternDetails(
    pattern:
        | {
              patternType?: string;
              id?: string;
              displayName?: string;
              shortName?: string;
              longName?: string;
          }
        | string
        | null
        | undefined
): ChartPatternDetail | null {
    if (!pattern) return null;
    if (typeof pattern === 'string') {
        const direct = LOOKUP_MAP.get(norm(pattern));
        if (direct) return direct;
        return null;
    }
    if (pattern.patternType) {
        const found = LOOKUP_MAP.get(norm(pattern.patternType));
        if (found) return found;
    }
    if (pattern.id) {
        const found = LOOKUP_MAP.get(norm(pattern.id));
        if (found) return found;
    }
    if (pattern.displayName) {
        const found = LOOKUP_MAP.get(norm(pattern.displayName));
        if (found) return found;
    }
    if (pattern.shortName) {
        const found = LOOKUP_MAP.get(norm(pattern.shortName));
        if (found) return found;
    }
    if (pattern.longName) {
        const found = LOOKUP_MAP.get(norm(pattern.longName));
        if (found) return found;
    }
    return null;
}

export const getChartPatternColor = (patternType: string, sentiment: string): string => {
    if (CHART_PATTERNS_CONFIG[patternType]) {
        return CHART_PATTERNS_CONFIG[patternType].color;
    }
    return sentiment.startsWith('BULLISH') ? '#22c55e' : '#ef4444';
};
