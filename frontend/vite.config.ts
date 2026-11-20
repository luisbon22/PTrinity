/// <reference types="vitest/config" />
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // En desarrollo /api va al backend local; en Docker lo enruta Nginx
    proxy: { '/api': 'http://localhost:8080' },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
    coverage: {
      provider: 'v8',
      include: ['src/**/*.{ts,tsx}'],
      exclude: ['src/main.tsx', 'src/setupTests.ts', 'src/**/*.test.{ts,tsx}', 'src/pruebas/**','src/vite-env.d.ts'],
      reporter: ['text', 'html', 'lcov'],
      thresholds: { lines: 85, statements: 85, functions: 85, branches: 85 },
    },
  },
})
