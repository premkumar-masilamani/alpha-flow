import type { Utterance } from '../components/TranscriptDisplay';
import { jsPDF } from 'jspdf';

export const formatTranscript = (utterances: Utterance[]): string => {
  return utterances
    .map(u => `[${u.speaker}]: ${u.text}`)
    .join('\n\n');
};

export const exportToMarkdown = (utterances: Utterance[], fileName: string) => {
  const content = utterances
    .map(u => `### ${u.speaker}\n${u.text}`)
    .join('\n\n');

  const blob = new Blob([content], { type: 'text/markdown' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `${fileName.split('.')[0]}_transcript.md`;
  a.click();
  URL.revokeObjectURL(url);
};

export const exportToPDF = (utterances: Utterance[], fileName: string) => {
  const doc = new jsPDF();

  doc.setFontSize(20);
  doc.text('Transcription Results', 10, 20);

  doc.setFontSize(12);
  let y = 30;

  utterances.forEach(u => {
    doc.setFont("helvetica", "bold");
    doc.text(`${u.speaker}:`, 10, y);
    y += 7;

    doc.setFont("helvetica", "normal");
    const lines = doc.splitTextToSize(u.text, 180);
    doc.text(lines, 10, y);
    y += (lines.length * 7) + 5;

    if (y > 280) {
      doc.addPage();
      y = 20;
    }
  });

  doc.save(`${fileName.split('.')[0]}_transcript.pdf`);
  console.log('PDF Export triggered for:', fileName);
};
