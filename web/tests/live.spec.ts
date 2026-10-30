import {expect, test} from '@playwright/test';

test.skip(!process.env.EXPLORER_URL, 'Set EXPLORER_URL to the running Kotlin Explorer for genuine discovery.');

test('real Judith discovery, changed objective/commander, inspected evidence and invalid selection', async ({page}) => {
  const pageErrors: string[] = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  const executions: {runId: string; objective: string; commander: string; argentumCommit: string; cards: unknown[]}[] = [];
  page.on('response', async response => {
    if (response.url().endsWith('/api/v1/discover') && response.ok()) executions.push(await response.json());
  });
  await page.goto('/');
  await page.getByLabel('Research objective').selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  await page.getByLabel('Find within these findings').fill('Bitterblossom');
  await page.getByRole('button', {name: /^Bitterblossom/}).click();
  await expect(page.getByRole('heading', {name: 'Bitterblossom', exact: true})).toBeVisible();
  await expect(page.getByRole('heading', {name: 'What must be true'}).first()).toBeVisible();
  await page.screenshot({path: '../generated/explorer/judith-tokens.png', fullPage: true});

  await page.getByLabel('Research objective').selectOption('reanimate-own-creatures');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  await expect.poll(() => executions.length).toBe(2);
  expect(executions[0].runId).not.toBe(executions[1].runId);
  expect(executions[1].objective).toBe('reanimate-own-creatures');

  await page.getByLabel('Commander’s exact name').fill('Oviya Pashiri, Sage Lifecrafter');
  await page.getByLabel('Research objective').selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toContainText('Oviya Pashiri', {timeout: 130_000});
  await expect.poll(() => executions.length).toBe(3);
  expect(executions[2].commander).toBe('Oviya Pashiri, Sage Lifecrafter');
  expect(executions[2].argentumCommit).toMatch(/^[a-f0-9]{40}$/);
  expect(executions[2].cards).not.toEqual(executions[0].cards);

  await page.getByLabel('Commander’s exact name').fill('Murder');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('Selection blocked', {exact: false})).toBeVisible();
  expect(executions).toHaveLength(3);
  expect(pageErrors).toEqual([]);
});

test('mobile layout and keyboard controls retain accurate blocked-selection feedback', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await expect(page.getByRole('button', {name: 'Run discovery'})).toBeEnabled();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('link', {name: 'MTG / RESEARCH LAB'})).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Commander’s exact name')).toBeFocused();
  await page.keyboard.press('Control+A');
  await page.keyboard.type('Murder');
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Research objective')).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', {name: 'Inspect commander'})).toBeFocused();
  await page.keyboard.press('Enter');
  await expect(page.getByText('Selection blocked', {exact: false})).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/mobile-selection.png', fullPage: true});
});
