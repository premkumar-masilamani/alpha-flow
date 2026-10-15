import '@testing-library/jest-dom';
import { vi } from 'vitest';

// Mock ResizeObserver which is used by lightweight-charts and container resizing
class MockResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}
window.ResizeObserver = MockResizeObserver;

// Mock localStorage for tests
const createLocalStorageMock = () => {
    let store: Record<string, string> = {};
    return {
        getItem: vi.fn((key: string) => store[key] ?? null),
        setItem: vi.fn((key: string, value: string) => {
            store[key] = String(value);
        }),
        removeItem: vi.fn((key: string) => {
            delete store[key];
        }),
        clear: vi.fn(() => {
            store = {};
        }),
    };
};

const localStorageMock = createLocalStorageMock();
Object.defineProperty(window, 'localStorage', {
    value: localStorageMock,
    writable: true,
});
Object.defineProperty(globalThis, 'localStorage', {
    value: localStorageMock,
    writable: true,
});
