import '@testing-library/jest-dom/vitest';

// jsdom does not implement URL.createObjectURL / revokeObjectURL.
// Provide stubs so components that use them in tests do not blow up.
if (typeof URL.createObjectURL !== 'function') {
  (URL as unknown as { createObjectURL: (b: Blob) => string }).createObjectURL = () =>
    'blob:mock';
}
if (typeof URL.revokeObjectURL !== 'function') {
  (URL as unknown as { revokeObjectURL: (s: string) => void }).revokeObjectURL = () => {
    /* noop */
  };
}
