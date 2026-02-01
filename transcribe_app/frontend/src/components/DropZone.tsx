import React, { useCallback, useState } from 'react';
import { Upload, FileAudio, X } from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface DropZoneProps {
  onFileSelect: (file: File) => void;
}

export const DropZone: React.FC<DropZoneProps> = ({ onFileSelect }) => {
  const [isDragging, setIsDragging] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  }, []);

  const handleDragLeave = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);

    const files = e.dataTransfer.files;
    if (files.length > 0) {
      const file = files[0];
      if (file.type.startsWith('audio/') || file.type.startsWith('video/')) {
        setSelectedFile(file);
        onFileSelect(file);
      } else {
        alert('Please drop an audio or video file.');
      }
    }
  }, [onFileSelect]);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      const file = e.target.files[0];
      setSelectedFile(file);
      onFileSelect(file);
    }
  };

  const clearFile = () => {
    setSelectedFile(null);
  };

  return (
    <div className="w-full max-w-2xl mx-auto">
      {!selectedFile ? (
        <label
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          className={cn(
            "relative group cursor-pointer flex flex-col items-center justify-center w-full h-64 border-2 border-dashed rounded-2xl transition-all duration-300",
            isDragging
              ? "border-blue-500 bg-blue-500/10 scale-[1.02]"
              : "border-slate-700 bg-slate-800/50 hover:border-slate-500 hover:bg-slate-800"
          )}
        >
          <div className="flex flex-col items-center justify-center pt-5 pb-6">
            <div className={cn(
              "p-4 rounded-full mb-4 transition-colors duration-300",
              isDragging ? "bg-blue-500 text-white" : "bg-slate-700 text-slate-300 group-hover:bg-slate-600"
            )}>
              <Upload className="w-8 h-8" />
            </div>
            <p className="mb-2 text-xl font-semibold text-slate-200">
              {isDragging ? "Drop to upload" : "Click or drag & drop"}
            </p>
            <p className="text-sm text-slate-400">
              Audio or Video files (MP3, WAV, MP4, etc.)
            </p>
          </div>
          <input
            type="file"
            className="hidden"
            accept="audio/*,video/*"
            onChange={handleFileChange}
          />
        </label>
      ) : (
        <div className="relative p-6 bg-slate-800 border border-slate-700 rounded-2xl flex items-center gap-4 animate-in fade-in slide-in-from-bottom-4 duration-500">
          <div className="p-3 bg-blue-500/20 text-blue-400 rounded-lg">
            <FileAudio className="w-8 h-8" />
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-lg font-medium text-slate-200 truncate">
              {selectedFile.name}
            </p>
            <p className="text-sm text-slate-400">
              {(selectedFile.size / (1024 * 1024)).toFixed(2)} MB
            </p>
          </div>
          <button
            onClick={clearFile}
            className="p-2 text-slate-400 hover:text-slate-200 hover:bg-slate-700 rounded-full transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
      )}
    </div>
  );
};
