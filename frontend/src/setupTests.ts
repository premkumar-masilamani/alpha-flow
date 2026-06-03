import '@testing-library/jest-dom';

// Mock ResizeObserver which is used by lightweight-charts and container resizing
class MockResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
}
window.ResizeObserver = MockResizeObserver;
