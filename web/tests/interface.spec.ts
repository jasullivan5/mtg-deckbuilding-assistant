import {expect, test} from '@playwright/test';
import type {CommanderView, Evidence, EvidenceIndicator, EvidenceState, Inspection} from '../src/api';

// Synthetic transport fixtures exercise interface behavior, not Magic correctness.
const commander: Inspection = {requestedName: 'Fixture commander', selectable: true, colorIdentity: ['BLACK', 'RED'], availability: 'APPLICATION_EXTENSION', blockers: [], knownLimitations: [], mechanicalCaveat: 'Fixture caveat', constructionScope: 'Fixture construction', argentumCommit: 'fixture-engine'};
const objectives = [{id: 'own-nontoken-creature-deaths', label: 'My nontoken creatures die', explanation: 'Legacy paragraph hidden by the new view.', capabilities: ['Fixture sacrifice capability.', 'Fixture death capability.', 'Fixture payoff capability.']},
  {id: 'reanimate-own-creatures', label: 'Return our creatures', explanation: '', capabilities: ['Fixture return.']},
  {id: 'create-creature-tokens', label: 'Create creature tokens', explanation: '', capabilities: ['Fixture tokens.']}];
function indicator(id: string, label: string, state: EvidenceState, status: string, fields: EvidenceIndicator['fields'] = []): EvidenceIndicator {
  return {id, label, state, status, scope: 'Exact fixture scope', explanation: 'Fixture technical explanation', established: [], limits: ['Fixture methodological caveat'], nextSteps: [], references: [], recordedEngine: null, currentEngine: 'fixture-current', historical: false, reading: 'Legacy paragraph should not dominate the evidence view.', fields};
}
function indicators() { return [indicator('mechanic', 'Discovery', 'ESTABLISHED', 'Creature sacrifice cost identified', [{label: 'Coverage', value: 'The payment, not the whole ability'}]),
  indicator('source', 'Rules comparison', 'NOT_RECORDED', 'No review recorded'), indicator('execution', 'Engine tests', 'NOT_RECORDED', 'No test results recorded'), indicator('independent', 'Independent verification', 'NOT_RECORDED', 'Not recorded')]; }
function finding(index = 0, card = 'Fixture card', state = 'IDENTIFIED') { return {index, card, browsingState: state, recordedConcern: false, pathways: {MATCH: state === 'IDENTIFIED' ? 1 : 0, UNKNOWN: state === 'UNRESOLVED' ? 1 : 0, NO_MATCH: 0}, mechanics: ['Sacrifice'], branches: [], unsupportedRecords: 0, partialPathways: 0, reason: 'Legacy repeated card explanation'}; }
function summary(cards: ReturnType<typeof finding>[] = [finding()]) { return {schemaVersion: 1, execution: 'FRESH_STRUCTURAL_DISCOVERY', runId: 'fixture-run', startedAt: '2026-11-02T12:00:00Z', durationMs: 25, applicationRevision: 'fixture', argentumCommit: 'fixture-engine', engineMode: 'test-fixture', commander: commander.requestedName, objective: objectives[0].id, allowedIdentity: ['BLACK', 'RED'], counts: {mechanical_input_cards_after_conventional_search: cards.length, unsupported_structure_records: 1, format_legality_unknown_cards: 0}, pathwayCounts: {MATCH: 1, UNKNOWN: 0, NO_MATCH: 0}, limitations: [], cards}; }
function detail(card = 'Fixture card'): Evidence { return {runId: 'fixture-run', card, authoredCard: {manaCost: '{B}', typeLine: 'Fixture type', text: 'Fixture cost: Fixture effect.'}, results: [], actions: [], facts: [], unsupported: [], structuralSupport: [], eligibility: [], translationAssurance: [], explanations: [], concerns: [], evidenceSummary: indicators(), rules: [{text: 'Fixture cost: Fixture effect.', actionId: 'fixture-activation', relevance: 'MATCH'}],
  abilities: [{id: 'fixture-activation', title: 'Legacy repeated ability heading', face: null, verdict: 'MATCH', contribution: 'Legacy repeated contribution', costs: ['Legacy repeated cost'], effects: ['Legacy repeated effect'], conditions: [], tradeoffs: [], uncertainties: [], evidence: indicators(), questions: [], resultIndexes: [], actionId: 'fixture-activation', objectiveNotes: ['A fixture condition beyond the printed text.'], unresolvedNotes: []}]}; }
function view(inspection: Inspection = commander): CommanderView { return {inspection, authoredCard: {manaCost: '{1}{B}{R}', typeLine: 'Fixture commander type', text: 'Fixture static ability.\nFixture death trigger.'}, headline: inspection.selectable ? 'Black and red' : 'Selection blocked', evidenceSummary: indicators().filter(i => i.id !== 'mechanic'), concerns: [], objectiveConnection: 'Fixture sacrifice connection.', image: null, rules: [{text: 'Fixture static ability.', actionId: null, relevance: null}, {text: 'Fixture death trigger.', actionId: 'fixture-death', relevance: 'MATCH'}]}; }

test.beforeEach(async ({page}) => {
  await page.route('**/api/v1/objectives', r => r.fulfill({json: objectives}));
  await page.route('**/api/v1/health', r => r.fulfill({json: {applicationRevision: 'fixture', argentumCommit: 'fixture-engine', engineMode: 'test-fixture'}}));
  await page.route('**/api/v1/commander-view', r => r.fulfill({json: view()}));
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary()}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: detail()}));
  await page.route('**/api/v1/runs/*/images?*', r => r.fulfill({json: (new URL(r.request().url()).searchParams.get('indices') ?? '').split(',').map(index => ({index: Number(index), status: 'UNAVAILABLE', reason: 'Synthetic fallback', faces: []}))}));
});

test('search retains loading state and shows the card once with only additional conditions', async ({page}) => {
  let release!: () => void; const pending = new Promise<void>(resolve => {release = resolve;}); let body: unknown;
  await page.route('**/api/v1/discover', async r => {body = r.request().postDataJSON(); await pending; await r.fulfill({json: summary()});});
  await page.goto('/');
  try { await page.getByRole('button', {name: 'Find cards', exact: true}).click(); await expect(page.getByRole('button', {name: 'Find cards', exact: true})).toBeDisabled();
    await expect.poll(() => body).toEqual({commander: 'Judith, the Scourge Diva', objective: objectives[0].id});
  } finally {release();}
  const card = page.getByRole('region', {name: 'Result inspection'});
  await expect(card.getByText('Fixture cost: Fixture effect.', {exact: true})).toHaveCount(1);
  await expect(card.getByText('A fixture condition beyond the printed text.', {exact: true})).toBeVisible();
  await expect(card.getByRole('heading', {name: 'Legacy repeated ability heading'})).toHaveCount(0);
  await expect(card.getByText('Legacy repeated contribution')).toHaveCount(0);
  await card.getByRole('button', {name: /Discovery/}).click();
  await expect(card.getByRole('region', {name: 'Discovery evidence'}).getByText('The payment, not the whole ability')).toBeVisible();
  await expect(card.getByText('Fixture methodological caveat')).not.toBeVisible();
  await card.getByText('Technical evidence and recorded interactions', {exact: true}).click();
  await expect(card.getByText('Fixture methodological caveat')).toBeVisible();
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('reanimate-own-creatures');
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toHaveCount(0);
});

test('selection disclosures are symmetric collapsed and independent of discovery', async ({page}) => {
  let searches = 0; let inspections = 0;
  await page.route('**/api/v1/discover', r => {searches++; return r.fulfill({json: summary()});});
  await page.route('**/api/v1/commander-view', r => {inspections++; return r.fulfill({json: view()});});
  await page.goto('/');
  await expect(page.getByText('Fixture sacrifice capability.')).not.toBeVisible();
  await expect(page.getByRole('region', {name: 'Selected commander'})).not.toBeVisible();
  await page.getByText('About objective', {exact: true}).click();
  await expect(page.locator('.objective-help li')).toHaveText(objectives[0].capabilities);
  await expect(page.getByText('Legacy paragraph hidden by the new view.')).toHaveCount(0);
  await page.getByText('About commander', {exact: true}).click();
  const panel = page.getByRole('region', {name: 'Selected commander'});
  await expect(panel.locator('.card-properties')).toContainText('Black, Red');
  await expect(panel.getByText('Fixture death trigger.', {exact: true})).toHaveCount(1);
  await expect(panel.getByRole('img', {name: 'Relevant to this objective', exact: true})).toHaveCount(1);
  await expect(panel.getByRole('button', {name: /Discovery/})).toHaveCount(0);
  expect(searches).toBe(0); expect(inspections).toBe(1);
  await page.getByText('About commander', {exact: true}).click(); await page.getByText('About commander', {exact: true}).click();
  expect(inspections).toBe(1);
});

test('blocked commander is exposed without executing discovery or replacing the selection', async ({page}) => {
  let executions = 0;
  const blocked = {...commander, requestedName: 'Murder', selectable: false, blockers: [{code: 'INELIGIBLE_COMMANDER', reason: 'Not a supported commander.', nextAction: 'Choose a supported commander.'}]};
  await page.route('**/api/v1/commander-view', r => r.fulfill({json: view(blocked)}));
  await page.route('**/api/v1/discover', r => {executions++; return r.fulfill({json: summary()});});
  await page.goto('/'); await page.getByRole('combobox', {name: 'Commander', exact: true}).fill('Murder'); await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect(page.getByRole('region', {name: 'Selected commander'})).toContainText('Selection blocked');
  await expect(page.locator('p.warning').filter({hasText: 'Not a supported commander.'})).toBeVisible();
  expect(executions).toBe(0); await expect(page.getByRole('combobox', {name: 'Commander', exact: true})).toHaveValue('Murder');
});

test('empty results transport errors expired evidence and reopening commander information preserve state', async ({page}) => {
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([])}));
  await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click(); await expect(page.getByText('No cards in this view.')).toBeVisible();
  await page.getByText('About commander', {exact: true}).click(); await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible();
  await page.route('**/api/v1/discover', r => r.fulfill({status: 409, json: {message: 'The worker is busy.'}}));
  await page.getByRole('button', {name: 'Find cards', exact: true}).click(); await expect(page.getByRole('alert')).toHaveText('The worker is busy.');
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary()}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({status: 410, json: {message: 'Run expired; search again.'}}));
  await page.getByRole('button', {name: 'Find cards', exact: true}).click(); await expect(page.getByRole('alert')).toHaveText('Run expired; search again.');
});

test('recognized findings retain priority while diagnostics and mechanic filters remain available', async ({page}) => {
  const mixed = {...finding(0, 'Mixed fixture'), pathways: {MATCH: 2, UNKNOWN: 1, NO_MATCH: 1}, unsupportedRecords: 1};
  const unresolved = {...finding(1, 'Unresolved fixture', 'UNRESOLVED'), mechanics: ['Destruction']};
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([mixed, unresolved])}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: detail(r.request().url().endsWith('/1') ? 'Unresolved fixture' : 'Mixed fixture')}));
  await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const nav = page.getByRole('navigation', {name: 'Finding categories', exact: true});
  await expect(nav.getByRole('button', {name: /Identified mechanics/}).locator('.category-count')).toHaveText('1');
  await expect(nav.getByRole('button', {name: /Needs interpretation/}).locator('.category-count')).toHaveText('1');
  await page.getByText('Research and diagnostic views', {exact: true}).click();
  await expect(page.getByRole('button', {name: /Has unresolved checks/})).toContainText('2 cards');
  await nav.getByRole('button', {name: /Needs interpretation/}).click(); await page.getByRole('combobox', {name: 'Mechanic', exact: true}).selectOption('Destruction');
  await expect(page.getByRole('button', {name: 'Unresolved fixture', exact: true})).toBeVisible(); await expect(page.getByRole('button', {name: 'Mixed fixture', exact: true})).toHaveCount(0);
});

test('structured fields separate historical outcomes known problems and missing verification', async ({page}) => {
  const data = detail();
  data.abilities[0].evidence = [indicators()[0], indicator('source', 'Rules comparison', 'KNOWN_PROBLEM', 'Recorded disagreement', [{label: 'Concern', value: 'Synthetic source disagreement'}]),
    {...indicator('execution', 'Engine tests', 'OUTDATED', 'Earlier evidence outdated', [{label: 'Earlier outcome', value: 'Failure recorded'}, {label: 'Current outcome', value: 'Not established by this evidence'}]), historical: true, recordedEngine: 'old-engine'}, indicators()[3]];
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: data}));
  await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click(); const card = page.getByRole('region', {name: 'Result inspection'});
  await card.getByRole('button', {name: /Engine tests/}).click(); await expect(card.getByRole('region', {name: 'Engine tests evidence'})).toContainText('Failure recorded');
  await expect(card.getByRole('button', {name: /Engine tests/})).not.toContainText('✓');
  await card.getByRole('button', {name: /Rules comparison/}).click(); await expect(card.getByRole('region', {name: 'Rules comparison evidence'})).toContainText('Synthetic source disagreement');
  await expect(card.getByRole('button', {name: /Rules comparison/})).toHaveClass(/KNOWN_PROBLEM/);
  await expect(card.getByRole('button', {name: /Independent verification/})).not.toHaveClass(/KNOWN_PROBLEM/);
});

test('unmapped abilities use a truthful chooser and keep their own evidence', async ({page}) => {
  const data = detail(); data.rules = [{text: data.authoredCard!.text, actionId: null, relevance: null}];
  data.abilities.push({...data.abilities[0], id: 'second-ability', actionId: 'second-ability', title: 'Second fixture ability', evidence: [indicator('mechanic', 'Discovery', 'INCOMPLETE', 'Affected creature type unresolved'), ...indicators().slice(1)]});
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => r.fulfill({json: data}));
  await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click(); const card = page.getByRole('region', {name: 'Result inspection'});
  await expect(card.getByRole('img', {name: 'Relevant to this objective', exact: true})).toHaveCount(0);
  await card.getByRole('button', {name: /Discovery/}).click(); await card.getByRole('combobox', {name: 'Ability to inspect'}).selectOption('second-ability');
  await expect(card.getByRole('button', {name: /Discovery/})).toContainText('Affected creature type unresolved'); await expect(card.getByRole('region', {name: 'Discovery evidence'})).toHaveCount(0);
});

test('keyboard selection focuses the inspector without a visible jump link and fits 390 pixels', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844}); await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const cardButton = page.getByRole('button', {name: 'Fixture card', exact: true}); await cardButton.focus(); await page.keyboard.press('Enter');
  const card = page.getByRole('region', {name: 'Result inspection'}); await expect(card).toBeFocused();
  await expect(page.getByRole('link', {name: /Read selected card/})).toHaveCount(0);
  const source = card.getByRole('button', {name: /Rules comparison/}); await source.focus(); await page.keyboard.press('Enter'); await expect(source).toHaveAttribute('aria-expanded', 'true');
  await expect(card.getByRole('region', {name: 'Rules comparison evidence'})).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
});
