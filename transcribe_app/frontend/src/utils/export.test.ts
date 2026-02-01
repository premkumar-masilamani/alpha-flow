import { describe, it, expect } from 'vitest';
import { formatTranscript } from './export';
import type { Utterance } from '../components/TranscriptDisplay';

describe('Export Utils', () => {
  it('should format transcript correctly for plain text', () => {
    const utterances: Utterance[] = [
      { id: '1', speaker: 'Coach', start: 0, end: 5, text: 'Hello' },
      { id: '2', speaker: 'Client', start: 6, end: 10, text: 'Hi there' }
    ];

    const result = formatTranscript(utterances);
    expect(result).toContain('[Coach]: Hello');
    expect(result).toContain('[Client]: Hi there');
  });
});
