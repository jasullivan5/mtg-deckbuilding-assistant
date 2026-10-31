import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';
import type {Run, Evidence, CommanderView, CardImage} from '../src/api';

test.skip(!process.env.EXPLORER_URL, 'Set EXPLORER_URL to the running Kotlin Explorer for genuine discovery.');

test('real objective changes and commander selection keep genuine execution and blockers', async ({page}) => {
  const runs: Run[] = [];
  const errors: string[] = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('response', async r => {if (r.url().endsWith('/api/v1/discover') && r.ok()) runs.push(await r.json());});
  await page.goto('/');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  await page.getByLabel('Find within these findings').fill('Bitterblossom');
  await page.getByRole('button', {name: 'Bitterblossom', exact: true}).click();
  const selected = page.getByRole('region', {name: 'Result inspection'});
  await expect(selected.getByRole('heading', {name: 'Bitterblossom', exact: true})).toBeVisible();
  await expect(selected.getByRole('group', {name: 'Evidence and verification'})).toBeVisible();
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('reanimate-own-creatures');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect.poll(() => runs.length, {timeout: 130_000}).toBe(2);
  expect(runs[1].runId).not.toBe(runs[0].runId);
  expect(runs[1].objective).toBe('reanimate-own-creatures');
  await page.getByLabel('Commander’s exact name').fill('Oviya Pashiri, Sage Lifecrafter');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect.poll(() => runs.length, {timeout: 130_000}).toBe(3);
  expect(runs[2].commander).toBe('Oviya Pashiri, Sage Lifecrafter');
  expect(runs[2].allowedIdentity).toEqual(['GREEN']);
  expect(runs[2].argentumCommit).toMatch(/^[a-f0-9]{40}$/);
  await page.getByLabel('Commander’s exact name').fill('Murder');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('Selection blocked', {exact: false}).first()).toBeVisible();
  expect(runs).toHaveLength(3);
  expect(errors).toEqual([]);
});

test('real evidence controls and commander inspection work with keyboard and a 390px screen', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await expect(page.getByRole('button', {name: 'Inspect commander'})).toBeEnabled();
  await page.keyboard.press('Tab');
  await expect(page.getByLabel('Commander’s exact name')).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('combobox', {name: 'Objective', exact: true})).toBeFocused();
  await page.keyboard.press('Tab');
  await expect(page.getByRole('button', {name: 'Inspect commander'})).toBeFocused();
  await page.keyboard.press('Enter');
  const commander = page.getByRole('region', {name: 'Selected commander'});
  await expect(commander).toContainText('can be your commander · Black and red');
  await expect(commander).toContainText('Whenever a nontoken creature you control dies');
  await expect(commander.getByText('Single fixed-identity commander', {exact: false})).not.toBeVisible();
  const disclosure = commander.getByText('Commander evidence', {exact: true});
  await disclosure.focus(); await page.keyboard.press('Enter');
  const independent = commander.getByRole('button', {name: /Independent review/});
  await independent.focus(); await page.keyboard.press('Enter');
  await expect(independent).toHaveAttribute('aria-expanded', 'true');
  await expect(commander.getByRole('region', {name: 'Independent review evidence'})).toContainText('No current independent approval');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/mobile-commander-evidence.png', fullPage: true});
});

test('Attrition complete activation, scoped verification, unresolved land sacrifice and richer Judith records', async ({page}) => {
  const errors: string[] = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.goto('/');
  const completed = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button', {name: 'Run discovery'}).click();
  const run: Run = await (await completed).json();
  expect(run.objective).toBe('own-nontoken-creature-deaths');
  const nav = page.getByRole('navigation', {name: 'Finding categories', exact: true});
  const identified = run.cards.filter(c => c.pathways.MATCH > 0).length;
  const unresolvedOnly = run.cards.filter(c => !c.pathways.MATCH && c.pathways.UNKNOWN > 0).length;
  await expect(nav.getByRole('button', {name: /Identified mechanics/}).locator('.category-count')).toHaveText(identified.toLocaleString());
  await expect(nav.getByRole('button', {name: /Needs interpretation/}).locator('.category-count')).toHaveText(unresolvedOnly.toLocaleString());
  await expect(nav.getByRole('button', {name: /All findings/}).locator('.category-count')).toHaveText(run.cards.length.toLocaleString());
  await expect(page.getByText('May help', {exact: true})).toHaveCount(0);
  const detail = page.getByRole('region', {name: 'Result inspection'});
  await page.getByLabel('Find within these findings').fill('Attrition');
  await page.getByRole('button', {name: 'Attrition', exact: true}).click();
  await expect(detail.getByRole('heading', {name: 'Attrition', exact: true})).toBeVisible();
  await expect(detail.getByText('Destroy target nonblack creature.', {exact: true})).toBeVisible();
  await expect(detail.getByText('Pay {B}.', {exact: true})).toBeVisible();
  await expect(detail.getByText('cannot activate it merely', {exact: false})).toBeVisible();
  await expect(detail.getByRole('heading', {name: /Activated ability/})).toHaveCount(1);
  await expect(detail.getByRole('heading', {name: /front/})).toHaveCount(0);
  await expect(detail.getByText('Application card text.', {exact: false})).toHaveCount(0);
  await detail.getByRole('button', {name: /Mechanic identified/}).click();
  await expect(detail.getByRole('region', {name: 'Mechanic identified evidence'})).toContainText('cost');
  await detail.getByRole('button', {name: /Source review/}).click();
  await expect(detail.getByRole('region', {name: 'Source review evidence'})).toContainText('No applicable registered source comparison');
  await detail.getByRole('button', {name: /Engine scenarios/}).click();
  await expect(detail.getByRole('region', {name: 'Engine scenarios evidence'})).toContainText('does not mean the engine cannot execute');
  await expect(detail.getByRole('button', {name: /Engine scenarios/})).not.toContainText('✓');
  const art = detail.getByRole('img', {name: /^Attrition/});
  await expect(art).toBeVisible({timeout: 30_000});
  await expect.poll(() => art.evaluate((img: HTMLImageElement) => img.naturalWidth), {timeout: 30_000}).toBeGreaterThan(0);
  expect(await art.getAttribute('src')).toContain('e3eb615b-249d-433f-a521-8310e8784b5d');
  await page.screenshot({path: '../generated/explorer/attrition-inspection.png', fullPage: true});

  await page.getByLabel('Find within these findings').fill('Acolyte of Aclazotz');
  await expect(detail.getByText('Each opponent loses 1 life.', {exact: true})).toBeVisible();
  await expect(detail.getByText('You gain 1 life.', {exact: true})).toBeVisible();
  await expect(detail.getByText('other than this source', {exact: false})).toBeVisible();
  await expect(detail.getByText('ordinary noncreature permanent does not meet', {exact: false})).toBeVisible();
  await expect(detail.getByRole('button', {name: /Mechanic identified/})).toContainText('Relevant part identified');
  await expect(detail.getByRole('button', {name: /Engine scenarios/})).toContainText('No run record');

  await nav.getByRole('button', {name: /Needs interpretation/}).click();
  await page.getByLabel('Find within these findings').fill('Agent of Shauku');
  await expect(detail.getByRole('heading', {name: 'Agent of Shauku', exact: true})).toBeVisible();
  await expect(detail.getByText('has not established a land becoming a creature', {exact: false})).toBeVisible();
  await expect(detail.getByRole('button', {name: /Mechanic identified/})).toContainText('Relationship unresolved');
  await page.screenshot({path: '../generated/explorer/agent-investigation.png', fullPage: true});

  const commander = page.getByRole('region', {name: 'Selected commander'});
  await expect(commander).toContainText('can be your commander · Black and red');
  await commander.getByText('Commander evidence', {exact: true}).click();
  await expect(commander.getByRole('button', {name: /Source review/})).toContainText('Author review recorded');
  await commander.getByRole('button', {name: /Engine scenarios/}).click();
  const enginePanel = commander.getByRole('region', {name: 'Engine scenarios evidence'});
  await expect(enginePanel).toContainText('not a fresh run');
  await expect(enginePanel).toContainText('does not identify the outcome of each test');
  await expect(commander.getByRole('button', {name: /Independent review/})).toContainText('No approval recorded');
  await page.screenshot({path: '../generated/explorer/commander-evidence.png', fullPage: true});

  await page.getByText('Research and diagnostic views', {exact: true}).click();
  const diagnostic = page.getByRole('navigation', {name: 'Diagnostic finding categories'});
  await expect(diagnostic.getByRole('button', {name: /Has unresolved checks/})).toContainText(`${run.cards.filter(c => c.pathways.UNKNOWN > 0).length.toLocaleString()} cards`);
  await expect(diagnostic.getByRole('button', {name: /Has nonqualifying checks/})).toContainText(`${run.cards.filter(c => c.pathways.NO_MATCH > 0).length.toLocaleString()} cards`);
  await nav.getByRole('button', {name: /Identified mechanics/}).click();
  await page.getByLabel('Find within these findings').fill('Attrition');
  await page.setViewportSize({width: 390, height: 844});
  await page.getByRole('link', {name: 'Read selected card: Attrition ↓'}).click();
  const review = detail.getByRole('button', {name: /Source review/});
  await review.focus(); await page.keyboard.press('Enter');
  await expect(review).toHaveAttribute('aria-expanded', 'true');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/mobile-evidence.png', fullPage: true});

  const attrition = run.cards.find(c => c.card === 'Attrition')!;
  const evidence: Evidence = await (await page.request.get(`/api/v1/runs/${run.runId}/cards/${attrition.index}`)).json();
  const view: CommanderView = await (await page.request.post('/api/v1/commander-view', {data: {commander: run.commander, objective: run.objective}})).json();
  const images: CardImage[] = await (await page.request.get(`/api/v1/runs/${run.runId}/images?indices=${attrition.index}`)).json();
  expect(evidence.abilities).toHaveLength(1);
  expect(evidence.results.map(r => r.verdict)).toEqual(['MATCH', 'NO_MATCH']);
  expect(images[0].status).toBe('VERIFIED');
  expect(errors).toEqual([]);
  await mkdir('../generated/explorer', {recursive: true});
  await writeFile('../generated/explorer/evidence-observation.json', JSON.stringify({observedAt: new Date().toISOString(), applicationRevision: run.applicationRevision,
    engineRevision: run.argentumCommit, runId: run.runId, objective: run.objective, commander: run.commander, eligibleCards: run.counts.mechanical_input_cards_after_conventional_search,
    indexedCards: run.cards.length, identified, unresolvedOnly, overlappingUnknown: run.cards.filter(c => c.pathways.UNKNOWN > 0).length, pathwayCounts: run.pathwayCounts,
    attritionAbility: evidence.abilities[0], commanderEvidence: view.evidenceSummary, verifiedImage: images[0], pageErrors: errors}, null, 2));
});
