import {defineConfig} from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  workers: 1,
  timeout: 180_000,
  expect: {timeout: 10_000},
  outputDir: '../generated/explorer/playwright',
  reporter: [['list'], ['json', {outputFile: '../generated/explorer/browser-results.json'}]],
  use: {
    baseURL: process.env.EXPLORER_URL ?? 'http://127.0.0.1:5179',
    headless: true,
    channel: process.env.EXPLORER_BROWSER_CHANNEL,
    viewport: {width: 1365, height: 950},
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  webServer: process.env.EXPLORER_URL ? undefined : {
    command: 'pnpm exec vite preview --host 127.0.0.1 --port 5179 --strictPort',
    url: 'http://127.0.0.1:5179',
    reuseExistingServer: !process.env.CI,
  },
});
