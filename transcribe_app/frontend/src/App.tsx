import { useState } from 'react';
import { DropZone } from './components/DropZone';
import { TranscriptDisplay } from './components/TranscriptDisplay';
import type { Utterance } from './components/TranscriptDisplay';
import { Download, Languages, FileText, Loader2, Sparkles } from 'lucide-react';
import { exportToMarkdown, exportToPDF } from './utils/export';

function App() {
  const [file, setFile] = useState<File | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [transcript, setTranscript] = useState<Utterance[] | null>(null);
  const [progress, setProgress] = useState(0);

  const handleFileSelect = (selectedFile: File) => {
    setFile(selectedFile);
    processFile(selectedFile);
  };

  const processFile = async (selectedFile: File) => {
    console.log('Processing file:', selectedFile.name);
    setIsProcessing(true);
    setTranscript(null);
    setProgress(0);

    // Mock processing simulation
    const steps = 10;
    for (let i = 1; i <= steps; i++) {
      await new Promise(resolve => setTimeout(resolve, 400));
      setProgress(i * 10);
    }

    const mockTranscript: Utterance[] = [
      {
        id: '1',
        speaker: 'Coach',
        start: 0,
        end: 12,
        text: "Good morning! It's great to see you again. Today, I'd like to focus on your progress with the leadership transition. How has the last week been for you?"
      },
      {
        id: '2',
        speaker: 'Client',
        start: 13,
        end: 28,
        text: "Thanks, Coach. It's been a bit of a rollercoaster, to be honest. I tried implementing the delegating framework we discussed, but I found myself hovering over the team more than I intended."
      },
      {
        id: '3',
        speaker: 'Coach',
        start: 29,
        end: 45,
        text: "That's a very common experience when you're first letting go. What do you think was driving that urge to hover? Was it a lack of trust in the output, or perhaps a feeling of losing control?"
      },
      {
        id: '4',
        speaker: 'Client',
        start: 46,
        end: 62,
        text: "I think it was a bit of both. I'm so used to being the one who ensures every detail is perfect. Seeing someone else take a different approach makes me anxious, even if I know their way might work too."
      }
    ];

    setTranscript(mockTranscript);
    setIsProcessing(false);
  };

  return (
    <div className="min-h-screen bg-[#0f172a] text-slate-200 p-8 font-sans">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <header className="flex flex-col md:flex-row justify-between items-center mb-12 gap-6">
          <div className="flex items-center gap-3">
            <div className="bg-blue-600 p-3 rounded-2xl shadow-lg shadow-blue-500/20">
              <Languages className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white">
                Lumina <span className="text-slate-400">Transcribe</span>
              </h1>
              <p className="text-slate-500 font-medium">Offline AI Coaching Companion</p>
            </div>
          </div>

          {transcript && (
            <div className="flex items-center gap-3">
              <button
                onClick={() => exportToMarkdown(transcript, file?.name || 'transcript')}
                className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl transition-all font-medium text-sm"
              >
                <FileText className="w-4 h-4" />
                Export MD
              </button>
              <button
                onClick={() => exportToPDF(transcript, file?.name || 'transcript')}
                className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-500 text-white rounded-xl transition-all font-medium shadow-lg shadow-blue-600/20 text-sm"
              >
                <Download className="w-4 h-4" />
                Download PDF
              </button>
            </div>
          )}
        </header>

        {/* Main Content */}
        <main className="space-y-12">
          {!transcript && !isProcessing && (
            <div className="text-center space-y-8 py-12">
              <div className="space-y-4">
                <h2 className="text-4xl font-extrabold text-white tracking-tight">
                  Transcribe with total privacy.
                </h2>
                <p className="text-xl text-slate-400 max-w-2xl mx-auto">
                  Drag and drop your coaching recordings. All processing happens locally.
                  No data ever leaves your machine.
                </p>
              </div>
              <DropZone onFileSelect={handleFileSelect} />
            </div>
          )}

          {isProcessing && (
            <div className="flex flex-col items-center justify-center space-y-6 py-24">
              <div className="relative">
                <Loader2 className="w-16 h-16 text-blue-500 animate-spin" />
                <Sparkles className="absolute -top-2 -right-2 w-6 h-6 text-yellow-500 animate-pulse" />
              </div>
              <div className="text-center space-y-2">
                <p className="text-2xl font-semibold text-white">Analyzing Conversation...</p>
                <p className="text-slate-400">Running diarization and alignment models</p>
              </div>
              <div className="w-64 h-2 bg-slate-800 rounded-full overflow-hidden">
                <div
                  className="h-full bg-blue-500 transition-all duration-300 ease-out"
                  style={{ width: `${progress}%` }}
                ></div>
              </div>
            </div>
          )}

          {transcript && (
            <div className="animate-in fade-in slide-in-from-bottom-8 duration-700">
              <div className="flex items-center gap-2 mb-8 text-slate-400 px-4">
                <div className="h-px flex-1 bg-slate-800"></div>
                <span className="text-sm font-semibold uppercase tracking-widest">Transcription Results</span>
                <div className="h-px flex-1 bg-slate-800"></div>
              </div>
              <TranscriptDisplay utterances={transcript} />
            </div>
          )}
        </main>

        {/* Footer */}
        <footer className="mt-24 pt-8 border-t border-slate-800 text-center text-slate-600 text-sm">
          <p>© 2025 Lumina AI. Secure. Private. Native.</p>
        </footer>
      </div>
    </div>
  );
}

export default App;
