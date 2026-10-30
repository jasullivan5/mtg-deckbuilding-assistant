import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';
import type {Run, CardImage} from '../src/api';

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
  await expect(page.getByRole('heading', {name: 'What needs to happen'}).first()).toBeVisible();
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


test('real sacrifice findings explain choices, land uncertainty, counts and verified printing imagery', async ({page}) => {
  const pageErrors: string[] = [];
  page.on('pageerror', error => pageErrors.push(error.message));
  await page.goto('/');
  await expect(page.getByRole('heading', {name: 'Commander Discovery', exact: true})).toBeVisible();
  const completed = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button', {name: 'Run discovery'}).click();
  const run: Run = await (await completed).json();
  expect(run.objective).toBe('own-nontoken-creature-deaths');
  const actualCounts = Object.fromEntries(['MATCH', 'UNKNOWN', 'NO_MATCH'].map(key => [key, run.cards.filter(card => card.pathways[key] > 0).length]));
  const nav = page.getByRole('navigation', {name: 'Finding categories'});
  for (const [label, key] of [['May help', 'MATCH'], ['Needs investigation', 'UNKNOWN'], ['Abilities ruled out', 'NO_MATCH']]) {
    await expect(nav.getByRole('button', {name: new RegExp(label)}).locator('.category-count')).toHaveText(actualCounts[key].toLocaleString());
  }
  await expect(nav.getByRole('button', {name: /Not fully understood/}).locator('.category-count')).toHaveText(run.cards.filter(c => c.unsupportedRecords > 0).length.toLocaleString());
  await expect(nav.getByRole('button', {name: /All findings/}).locator('.category-count')).toHaveText(run.cards.length.toLocaleString());
  const detail = page.getByRole('region', {name: 'Result inspection'});
  await page.getByLabel('Find within these findings').fill('Acolyte of Aclazotz');
  await expect(detail.getByRole('heading', {name: 'Acolyte of Aclazotz', exact: true})).toBeVisible();
  await expect(detail.getByText('Tap this source;', {exact: false})).toBeVisible();
  await expect(detail.getByText('The source itself cannot be sacrificed', {exact: false})).toBeVisible();
  const art = detail.getByRole('img', {name: /^Acolyte of Aclazotz/});
  await expect(art).toBeVisible({timeout: 30_000});
  await expect.poll(() => art.evaluate((img: HTMLImageElement) => img.naturalWidth), {timeout: 30_000}).toBeGreaterThan(0);
  expect(await art.getAttribute('src')).toContain('99009400-ffa4-40af-8305-78295ae605ac');
  await page.screenshot({path: '../generated/explorer/acolyte-discovery.png', fullPage: true});

  await nav.getByRole('button', {name: /Needs investigation/}).click();
  await page.getByLabel('Find within these findings').fill('Agent of Shauku');
  await expect(detail.getByRole('heading', {name: 'Agent of Shauku', exact: true})).toBeVisible();
  await expect(detail.getByText('a land as a cost', {exact: false})).toBeVisible();
  await expect(detail.getByText('has not established a land becoming a creature', {exact: false})).toBeVisible();
  await expect(detail.getByText('Needs investigation', {exact: true})).toBeVisible();
  await page.screenshot({path: '../generated/explorer/agent-investigation.png', fullPage: true});

  await nav.getByRole('button', {name: /May help/}).click();
  await page.getByLabel('Find within these findings').fill('Accursed Centaur');
  await expect(detail.getByRole('heading', {name: 'Accursed Centaur', exact: true})).toBeVisible();
  await expect(detail.getByText('required sacrifice', {exact: false})).toBeVisible();
  await expect(detail.getByText('when this source enters', {exact: false})).toBeVisible();
  await expect(detail.getByText('does not exclude this source', {exact: false})).toBeVisible();
  await nav.getByRole('button', {name: /Abilities ruled out/}).click();
  await page.getByLabel('Mechanical relationship').selectOption('ALL');
  await page.getByLabel('Find within these findings').fill('Murder');
  await page.getByRole('button', {name: 'Murder', exact: true}).click();
  await expect(detail.getByRole('heading', {name: 'Murder', exact: true})).toBeVisible();
  await expect(detail.getByText("excluded by this question's selection rules", {exact: false})).toBeVisible();
  await nav.getByRole('button', {name: /All findings/}).click();
  await page.getByLabel('Find within these findings').fill('');
  await page.getByLabel('Mechanical relationship').selectOption('Sacrifice');
  const sacrificeCards = run.cards.filter(c => c.mechanics.includes('Sacrifice')).length;
  await expect(page.getByText(`${sacrificeCards.toLocaleString()} cards in this view`, {exact: false})).toBeVisible();
  await page.getByLabel('Find within these findings').fill('Acolyte of Aclazotz');
  await page.setViewportSize({width: 390, height: 844});
  await page.getByRole('link', {name: 'Read selected card: Acolyte of Aclazotz ↓'}).click();
  await expect(detail).toBeFocused();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/mobile-card-inspection.png', fullPage: true});
  const imageResponse = await page.request.get(`/api/v1/runs/${run.runId}/images?indices=${run.cards.find(c => c.card === 'Acolyte of Aclazotz')!.index}`);
  const images: CardImage[] = await imageResponse.json();
  expect(images[0].status).toBe('VERIFIED');
  expect(images[0].oracleId).toBe('324d2aa8-6dea-4b0d-898d-1e52ed187565');
  expect(pageErrors).toEqual([]);
  await mkdir('../generated/explorer', {recursive: true});
  await writeFile('../generated/explorer/experience-observation.json', JSON.stringify({
    observedAt: new Date().toISOString(), applicationRevision: run.applicationRevision, engineRevision: run.argentumCommit,
    runId: run.runId, objective: run.objective, commander: run.commander,
    eligibleCards: run.counts.mechanical_input_cards_after_conventional_search, indexedCards: run.cards.length,
    overlappingCardCounts: actualCounts, pathwayCounts: run.pathwayCounts, unsupportedRecords: run.counts.unsupported_structure_records,
    sacrificeCards, policyExample: {card: 'Murder', exclusionIsSelectionRule: true}, verifiedImage: images[0], pageErrors,
  }, null, 2));
});
