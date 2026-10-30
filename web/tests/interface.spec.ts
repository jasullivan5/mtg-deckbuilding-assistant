import {expect, test} from '@playwright/test';
import type {EvidenceIndicator, EvidenceState} from '../src/api';

// Explicitly synthetic transport records exercise UI states, not Magic correctness.
const commander = {requestedName: 'Judith, the Scourge Diva', selectable: true, colorIdentity: ['BLACK', 'RED'], availability: 'APPLICATION_EXTENSION', blockers: [], knownLimitations: []};
const objectives = [
  {id: 'own-nontoken-creature-deaths', label: 'My nontoken creatures die', explanation: 'Fixture objective.'},
  {id: 'reanimate-own-creatures', label: 'Return our creatures', explanation: 'Fixture return.'},
  {id: 'create-creature-tokens', label: 'Create creature tokens', explanation: 'Fixture creation.'},
];
function indicator(id: string, label: string, state: EvidenceState, status: string): EvidenceIndicator {
  return {id, label, state, status, scope: id === 'mechanic' ? 'Fixture sacrifice cost only' : 'Fixture card-level record, not whole-ability verification',
    explanation: `Synthetic ${label} explanation.`, established: [], limits: ['This does not verify the whole ability.'], nextSteps: ['Inspect the scoped supporting record.'], references: [], recordedEngine: null, currentEngine: 'fixture-current', historical: false};
}
function indicators() {
  return [indicator('mechanic', 'Mechanic identified', 'ESTABLISHED', 'Relevant part identified'),
    indicator('source', 'Source review', 'NOT_RECORDED', 'No comparison recorded'),
    indicator('execution', 'Engine scenarios', 'NOT_RECORDED', 'No run record'),
    indicator('independent', 'Independent review', 'NOT_RECORDED', 'No approval recorded')];
}
function finding(index = 0, card = 'Fixture card', state = 'IDENTIFIED') {
  return {index, card, browsingState: state, recordedConcern: false, pathways: {MATCH: state === 'IDENTIFIED' ? 1 : 0, UNKNOWN: state === 'UNRESOLVED' ? 1 : 0, NO_MATCH: 0},
    mechanics: ['Sacrifice'], branches: [], unsupportedRecords: 0, partialPathways: 0, reason: 'A synthetic creature-sacrifice relationship.'};
}
function summary(cards: ReturnType<typeof finding>[] = []) {
  return {schemaVersion: 1, execution: 'FRESH_STRUCTURAL_DISCOVERY', runId: 'fixture-run', startedAt: '2026-10-30T20:00:00Z', durationMs: 25,
    applicationRevision: 'fixture', argentumCommit: 'fixture-engine', engineMode: 'test-fixture', commander: commander.requestedName,
    objective: objectives[0].id, allowedIdentity: ['BLACK', 'RED'], counts: {mechanical_input_cards_after_conventional_search: cards.length, unsupported_structure_records: 1, format_legality_unknown_cards: 0},
    pathwayCounts: {MATCH: 1, UNKNOWN: 0, NO_MATCH: 0}, limitations: [], cards};
}
function detail(card = 'Fixture card') {
  return {runId: 'fixture-run', card, authoredCard: null, results: [], actions: [], facts: [], unsupported: [], structuralSupport: [], eligibility: [], translationAssurance: [], explanations: [], concerns: [], evidenceSummary: indicators(),
    abilities: [{id: 'fixture-activation', title: 'Activated ability 1', face: null, verdict: 'MATCH', contribution: 'A synthetic sacrifice cost contributes to the objective.', costs: ['Pay the fixture cost.'], effects: ['Perform the fixture effect.'], conditions: ['A legal fixture target is required.'], tradeoffs: [], uncertainties: [], evidence: indicators(), questions: [], resultIndexes: [], actionId: 'fixture-activation'}]};
}
function view(inspection = commander) { return {inspection, authoredCard: null, headline: inspection.selectable ? 'Judith can be your commander · Black and red' : 'Selection blocked', evidenceSummary: indicators(), concerns: []}; }

test.beforeEach(async ({page}) => {
  await page.route('**/api/v1/objectives', r => r.fulfill({json: objectives}));
  await page.route('**/api/v1/health', r => r.fulfill({json: {applicationRevision: 'fixture', argentumCommit: 'fixture-engine', engineMode: 'test-fixture'}}));
  await page.route('**/api/v1/commander-view', r => r.fulfill({json: view()}));
  await page.route('**/api/v1/runs/*/images?*', r => r.fulfill({json: (new URL(r.request().url()).searchParams.get('indices') ?? '').split(',').map(index => ({index: Number(index), status: 'UNAVAILABLE', reason: 'Synthetic image fallback.', faces: []}))}));
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([finding()])}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: detail()}));
});

test('loading, objective submission and clickable scoped evidence work without decorative positive labels', async ({page}) => {
  let release!: () => void;
  const pending = new Promise<void>(resolve => {release = resolve;});
  let body: unknown;
  await page.route('**/api/v1/discover', async r => {body = r.request().postDataJSON(); await pending; await r.fulfill({json: summary([finding()])});});
  await page.goto('/');
  try {
    await page.getByRole('button', {name: 'Run discovery'}).click({timeout: 5000});
    await expect(page.getByRole('button', {name: 'Run discovery'})).toBeDisabled();
    await expect.poll(() => body).toEqual({commander: commander.requestedName, objective: 'own-nontoken-creature-deaths'});
  } finally { release(); }
  const card = page.getByRole('region', {name: 'Result inspection'});
  await expect(card.getByRole('heading', {name: 'Fixture card', exact: true})).toBeVisible();
  await expect(card.getByText('Pay the fixture cost.')).toBeVisible();
  await expect(card.getByText('Perform the fixture effect.')).toBeVisible();
  await expect(card.getByText('May help', {exact: false})).toHaveCount(0);
  const mechanic = card.getByRole('button', {name: /Mechanic identified/});
  await mechanic.click();
  await expect(card.getByRole('region', {name: 'Mechanic identified evidence'})).toContainText('Fixture sacrifice cost only');
  await card.getByRole('button', {name: /Engine scenarios/}).click();
  await expect(card.getByRole('region', {name: 'Engine scenarios evidence'})).toContainText('Synthetic Engine scenarios explanation.');
  await expect(card.getByRole('button', {name: /Engine scenarios/})).not.toContainText('✓');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('reanimate-own-creatures');
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toHaveCount(0);
  await expect(card.getByRole('heading', {name: 'Fixture card', exact: true})).toHaveCount(0);
});

test('blocked selection never executes discovery or silently replaces the commander', async ({page}) => {
  let executions = 0;
  const blocked = {...commander, requestedName: 'Murder', selectable: false, blockers: [{code: 'INELIGIBLE_COMMANDER', reason: 'Not a supported commander.', nextAction: 'Choose a supported commander.'}]};
  await page.route('**/api/v1/commander-view', r => r.fulfill({json: view(blocked as typeof commander)}));
  await page.route('**/api/v1/discover', r => {executions++; return r.fulfill({json: summary()});});
  await page.goto('/');
  await page.getByLabel('Commander’s exact name').fill('Murder');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('Selection blocked', {exact: true})).toBeVisible();
  await expect(page.locator('p.warning').filter({hasText: 'Not a supported commander.'})).toBeVisible();
  expect(executions).toBe(0);
  await expect(page.getByLabel('Commander’s exact name')).toHaveValue('Murder');
});

test('empty discoveries transport errors and expired evidence remain explicit', async ({page}) => {
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary()}));
  await page.goto('/'); await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('No findings in this view.', {exact: false})).toBeVisible();
  await page.route('**/api/v1/discover', r => r.fulfill({status: 409, json: {message: 'The worker is busy.'}}));
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('alert')).toHaveText('The worker is busy.');
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([finding()])}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({status: 410, json: {message: 'Run expired; run discovery again.'}}));
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('alert')).toHaveText('Run expired; run discovery again.');
  await expect(page.getByRole('region', {name: 'Result inspection'}).getByRole('group', {name: 'Evidence and verification'})).toHaveCount(0);
});

test('recognized cards stay in ordinary discovery while overlapping negatives remain optional diagnostics', async ({page}) => {
  const mixed = {...finding(0, 'Mixed fixture'), pathways: {MATCH: 2, UNKNOWN: 1, NO_MATCH: 1}, unsupportedRecords: 1};
  const unresolved = {...finding(1, 'Unresolved fixture', 'UNRESOLVED'), mechanics: ['Destruction']};
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([mixed, unresolved])}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: detail(r.request().url().endsWith('/1') ? 'Unresolved fixture' : 'Mixed fixture')}));
  await page.goto('/'); await page.getByRole('button', {name: 'Run discovery'}).click();
  const nav = page.getByRole('navigation', {name: 'Finding categories', exact: true});
  await expect(nav.getByRole('button', {name: /Identified mechanics/}).locator('.category-count')).toHaveText('1');
  await expect(nav.getByRole('button', {name: /Needs interpretation/}).locator('.category-count')).toHaveText('1');
  await expect(page.getByRole('button', {name: 'Mixed fixture', exact: true})).toBeVisible();
  await expect(page.getByRole('button', {name: /Has nonqualifying checks/})).not.toBeVisible();
  await page.getByText('Research and diagnostic views', {exact: true}).click();
  await expect(page.getByRole('button', {name: /Has unresolved checks/})).toContainText('2 cards');
  await expect(page.getByRole('button', {name: /Has nonqualifying checks/})).toContainText('1 cards');
  await nav.getByRole('button', {name: /Needs interpretation/}).click();
  await page.getByRole('combobox', {name: 'Mechanic', exact: true}).selectOption('Destruction');
  await expect(page.getByRole('button', {name: 'Mixed fixture', exact: true})).toHaveCount(0);
  await expect(page.getByRole('button', {name: 'Unresolved fixture', exact: true})).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('link', {name: 'Read selected card: Unresolved fixture ↓'}).click();
  await expect(page.getByRole('region', {name: 'Result inspection'})).toBeFocused();
});

test('historical evidence is clickable and distinguishes stale records from a known problem', async ({page}) => {
  const data = detail();
  const execution = {...indicator('execution', 'Engine scenarios', 'OUTDATED', 'Prior evidence not applicable'), historical: true, recordedEngine: 'old-engine', explanation: 'A historical result is not a fresh pass on this build.'};
  data.abilities[0].evidence = [indicators()[0], indicator('source', 'Source review', 'KNOWN_PROBLEM', 'Recorded disagreement'), execution, indicators()[3]];
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: data}));
  await page.goto('/'); await page.getByRole('button', {name: 'Run discovery'}).click();
  const card = page.getByRole('region', {name: 'Result inspection'});
  await card.getByRole('button', {name: /Engine scenarios/}).click();
  await expect(card.getByRole('region', {name: 'Engine scenarios evidence'})).toContainText('not a fresh pass');
  await expect(card.getByRole('button', {name: /Engine scenarios/})).not.toContainText('✓');
  await card.getByRole('button', {name: /Source review/}).click();
  await expect(card.getByRole('region', {name: 'Source review evidence'})).toContainText('Recorded disagreement');
  await expect(card.getByRole('button', {name: /Source review/})).toHaveClass(/KNOWN_PROBLEM/);
  await expect(card.getByRole('button', {name: /Independent review/})).toContainText('No approval recorded');
});

test('evidence controls remain keyboard accessible and contained at 390 pixels', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/'); await page.getByRole('button', {name: 'Run discovery'}).click();
  const card = page.getByRole('region', {name: 'Result inspection'});
  const button = card.getByRole('button', {name: /Source review/});
  await button.focus(); await page.keyboard.press('Enter');
  await expect(button).toHaveAttribute('aria-expanded', 'true');
  await expect(card.getByRole('region', {name: 'Source review evidence'})).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.keyboard.press('Enter');
  await expect(button).toHaveAttribute('aria-expanded', 'false');
});
