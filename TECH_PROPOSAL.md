# Tech Stack Proposal: Offline Transcription & Diarization Desktop App

## Overview
The goal is to build a cross-platform (macOS, Windows, Linux) desktop application that allows users to transcribe and diarize coaching conversations completely offline.

## Proposed Tech Stack

### 1. Application Framework: **Tauri 2.0**
- **Language**: Rust (Backend), React (Frontend).
- **Why**:
  - **Performance**: Rust provides native performance for AI inference.
  - **Security**: Hardened by default, no data leaks.
  - **Resource Efficiency**: Significantly smaller binary size and memory footprint compared to Electron.
  - **Native Feel**: Uses system-native webviews (WebView2 on Windows, WebKit on macOS/Linux).

### 2. Frontend: **React + Tailwind CSS + Lucide Icons**
- **Why**:
  - **Productivity**: Fast development of modern, responsive interfaces.
  - **Look & Feel**: Tailwind allows for a "fresh", clean UI with minimal effort.
  - **Component Architecture**: Easy to manage complex transcription views.

### 3. AI Engine (Offline):
- **Transcription**: **whisper-rs** (Rust bindings for `whisper.cpp`).
  - **Feature**: Supports various model sizes (Tiny to Large-v3) in GGUF format. High performance on both CPU and GPU.
- **Diarization**: **sherpa-onnx**.
  - **Feature**: Provides speaker segmentation and identification using ONNX models. Works perfectly offline and integrates well with Rust.

### 4. Data Handling & Export:
- **Formats**: Supports dragging and dropping common audio/video formats (WAV, MP3, MP4, etc.).
- **Export**:
  - **Markdown**: Direct string generation.
  - **PDF**: Generated via the browser's print-to-PDF or specialized Rust libraries like `genpdf`.

## Privacy & Security
- **100% Local**: No internet connection required after initial setup/model download.
- **Zero Data Leakage**: All processing happens on the user's machine.

## Implementation Roadmap
1. **Frontend Prototype**: Build the drag-and-drop UI and transcription visualization. (Current Task)
2. **Rust Backend Integration**: Integrate `whisper-rs` and `sherpa-onnx`.
3. **Model Management**: Implement a secure way to bundle or download AI models locally.
4. **Export Engine**: Finalize PDF/MD generation logic.
