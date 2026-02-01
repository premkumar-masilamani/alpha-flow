import React from 'react';
import { User, Clock } from 'lucide-react';

export interface Utterance {
  id: string;
  speaker: string;
  start: number;
  end: number;
  text: string;
}

interface TranscriptDisplayProps {
  utterances: Utterance[];
}

const formatTime = (seconds: number) => {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return `${h > 0 ? h + ':' : ''}${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
};

export const TranscriptDisplay: React.FC<TranscriptDisplayProps> = ({ utterances }) => {
  return (
    <div className="w-full max-w-4xl mx-auto space-y-6">
      {utterances.map((u) => (
        <div
          key={u.id}
          className="group flex flex-col md:flex-row gap-4 p-6 bg-slate-800/40 border border-slate-700/50 rounded-2xl hover:bg-slate-800/60 hover:border-slate-600 transition-all duration-300"
        >
          <div className="flex flex-row md:flex-col items-center md:items-start gap-3 md:w-32 flex-shrink-0">
            <div className="flex items-center gap-2 text-blue-400">
              <User className="w-4 h-4" />
              <span className="font-bold text-sm tracking-wide uppercase">{u.speaker}</span>
            </div>
            <div className="flex items-center gap-2 text-slate-500 text-xs font-mono">
              <Clock className="w-3 h-3" />
              <span>{formatTime(u.start)}</span>
            </div>
          </div>

          <div className="flex-1">
            <p className="text-slate-200 leading-relaxed text-lg">
              {u.text}
            </p>
          </div>
        </div>
      ))}
    </div>
  );
};
