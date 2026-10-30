import {expect, test} from '@playwright/test';

// Synthetic transport fixtures test UI states only. Live mechanics are tested separately.
const objectives = [
  {id: 'own-nontoken-creature-deaths', label: 'Our nontoken creatures die', explanation: 'Direct death generation.'},
  {id: 'reanimate-own-creatures', label: 'Return our creatures', explanation: 'Direct graveyard movement.'},
  {id: 'create-creature-tokens', label: 'Create creature tokens', explanation: 'Creature tokens under our control.'},
];
const inspection = {
  requestedName: 'Judith, the Scourge Diva', selectable: true, colorIdentity: ['BLACK', 'RED'],
  availability: 'APPLICATION_EXTENSION', blockers: [], knownLimitations: [],
  constructionScope: 'Single fixed-identity commander.', mechanicalCaveat: 'No whole-card certification.',
};
function summary(cards: unknown[] = []) {
  return {
    schemaVersion: 1, execution: 'FRESH_STRUCTURAL_DISCOVERY', runId: 'fixture-run',
    startedAt: '2026-10-29T20:00:00Z', durationMs: 25, applicationRevision: 'fixture',
    argentumCommit: 'fixture-engine', engineMode: 'test-fixture', commander: inspection.requestedName,
    objective: objectives[2].id, allowedIdentity: ['BLACK', 'RED'],
    counts: {mechanical_input_cards_after_conventional_search: 1, unsupported_structure_records: 1, format_legality_unknown_cards: 0},
    pathwayCounts: {MATCH: 1, UNKNOWN: 0, NO_MATCH: 0}, limitations: ['Fixture limitation.'], cards,
  };
}
test.beforeEach(async ({page}) => {
  await page.route('**/api/v1/runs/*/images?*', route => route.fulfill({json:
    (new URL(route.request().url()).searchParams.get('indices') ?? '').split(',').map(index => ({
      index: Number(index), status: 'UNAVAILABLE', reason: 'Synthetic fixture: image identity is not established.', faces: [],
    }))}));
  await page.route('**/api/v1/objectives', r => r.fulfill({json: objectives}));
  await page.route('**/api/v1/health', r => r.fulfill({json: {applicationRevision: 'fixture', argentumCommit: 'fixture', engineMode: 'test-fixture'}}));
  await page.route('**/api/v1/commander', r => r.fulfill({json: inspection}));
});

test('commander/objective submission, loading, card selection and evidence inspection', async ({page}) => {
  let release!: () => void;
  const wait = new Promise<void>(resolve => { release = resolve; });
  let submitted: unknown;
  await page.route('**/api/v1/discover', async r => {
    submitted = r.request().postDataJSON();
    await wait;
    await r.fulfill({json: summary([{
      index: 0, card: 'Fixture token card', pathways: {MATCH: 1, UNKNOWN: 0, NO_MATCH: 0},
      branches: ['CREATE_CREATURE_TOKEN'], unsupportedRecords: 1, partialPathways: 1, reason: 'Fixture creation decision.',
    }])});
  });
  await page.route('**/api/v1/runs/fixture-run/cards/0', r => r.fulfill({json: {
    runId: 'fixture-run', card: 'Fixture token card', authoredCard: null,
    results: [{
      card: 'Fixture token card', face: 'Fixture token card', actionId: 'fixture-action',
      abilityPath: 'fixture-path', branch: 'CREATE_CREATURE_TOKEN', operation: 'CreateToken',
      verdict: 'MATCH', interpretation: 'PARTIAL_ACTION_SUPPORTED_PATHWAY_NOT_GAMEPLAY_VERIFIED',
      prerequisites: ['Fixture resources must be available.'], decisions: [{stage: 'fixture', verdict: 'MATCH', reason: 'Fixture creation decision.'}],
      sourceReferences: [], circumstances: null,
    }],
    actions: [], facts: [], unsupported: [{reason: 'Fixture unsupported child.'}],
    structuralSupport: [], eligibility: [], translationAssurance: [],
    explanations: [{summary: 'Fixture creation explanation.', requirements: ['Fixture resources must be available.']}],
  }}));
  await page.goto('/');
  await page.getByLabel('Research objective').selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('Running discovery…', {exact: false})).toBeVisible();
  await expect(page.getByRole('button', {name: 'Run discovery'})).toBeDisabled();
  await expect.poll(() => submitted).toEqual({commander: inspection.requestedName, objective: 'create-creature-tokens'});
  release();
  await expect(page.getByRole('heading', {name: 'Fixture token card', exact: true})).toBeVisible();
  await page.getByRole('button', {name: /^Fixture token card/}).click();
  await expect(page.getByText('Fixture resources must be available.')).toBeVisible();
  await page.getByText('What else could the application not read?', {exact: false}).click();
  await expect(page.getByText('Fixture unsupported child.')).toBeVisible();
  await page.getByText('How much has this card been checked?').click();
  await expect(page.getByText('No separate application card-review record was supplied.')).toBeVisible();
  await page.getByLabel('Research objective').selectOption('reanimate-own-creatures');
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toHaveCount(0);
  await expect(page.getByRole('heading', {name: 'Fixture token card', exact: true})).toHaveCount(0);
});

test('blocked commander stays distinct from an empty discovery and never falls back', async ({page}) => {
  let discoveries = 0;
  await page.route('**/api/v1/commander', r => r.fulfill({json: {
    ...inspection, requestedName: 'Murder', selectable: false,
    blockers: [{code: 'INELIGIBLE_COMMANDER', reason: 'Not a supported commander.', nextAction: 'Choose a supported commander.'}],
  }}));
  await page.route('**/api/v1/discover', r => { discoveries++; return r.fulfill({json: summary()}); });
  await page.goto('/');
  await page.getByLabel('Commander’s exact name').fill('Murder');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('Selection blocked', {exact: false})).toBeVisible();
  await expect(page.locator('p.warning').filter({hasText: 'Not a supported commander.'})).toBeVisible();
  expect(discoveries).toBe(0);
  await expect(page.getByLabel('Commander’s exact name')).toHaveValue('Murder');
});

test('empty results and transport errors remain understandable', async ({page}) => {
  await page.route('**/api/v1/discover', r => r.fulfill({json: {...summary(), pathwayCounts: {MATCH: 0, UNKNOWN: 0, NO_MATCH: 0}}}));
  await page.goto('/');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByText('No findings in this view.', {exact: false})).toBeVisible();
  await page.route('**/api/v1/discover', r => r.fulfill({status: 409, json: {code: 'BUSY', message: 'Research is still running. Wait before retrying.'}}));
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('alert')).toHaveText('Research is still running. Wait before retrying.');
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toHaveCount(0);
});

test('expired evidence is reported instead of inventing a result', async ({page}) => {
  await page.route('**/api/v1/discover', r => r.fulfill({json: summary([{
    index: 0, card: 'Expired fixture', pathways: {MATCH: 0, UNKNOWN: 1, NO_MATCH: 0},
    branches: [], unsupportedRecords: 0, partialPathways: 1, reason: null,
  }])}));
  await page.route('**/api/v1/runs/fixture-run/cards/0', r => r.fulfill({
    status: 410, json: {code: 'RUN_EXPIRED', message: 'This run is no longer retained. Run discovery again.'},
  }));
  await page.goto('/');
  await page.getByRole('button', {name: 'Run discovery'}).click();
  await expect(page.getByRole('alert')).toHaveText('This run is no longer retained. Run discovery again.');
  await page.getByRole('button', {name: /Needs investigation/}).click();
  await expect(page.getByRole('button', {name: /^Expired fixture/})).toBeVisible();
});


test('overlapping card counts, relationship filtering and selection remain distinct from pathway counts', async ({page}) => {
  const cards = [
    {index: 0, card: 'Mixed fixture', pathways: {MATCH: 2, UNKNOWN: 1, NO_MATCH: 1}, mechanics: ['Sacrifice'], branches: [], unsupportedRecords: 1, partialPathways: 1, reason: 'Synthetic sacrifice relationship.'},
    {index: 1, card: 'Unresolved fixture', pathways: {MATCH: 0, UNKNOWN: 1, NO_MATCH: 0}, mechanics: ['Destruction'], branches: [], unsupportedRecords: 0, partialPathways: 1, reason: 'Synthetic unresolved destruction.'},
  ];
  await page.route('**/api/v1/discover', r => r.fulfill({json: {...summary(cards), pathwayCounts: {MATCH: 2, UNKNOWN: 2, NO_MATCH: 1}}}));
  await page.route('**/api/v1/runs/fixture-run/cards/*', r => {
    const card = cards[Number(r.request().url().split('/').pop())];
    return r.fulfill({json: {runId: 'fixture-run', card: card.card, authoredCard: null, results: [], actions: [], facts: [], unsupported: [], structuralSupport: [], eligibility: [], translationAssurance: [], explanations: []}});
  });
  await page.goto('/');
  await expect(page.getByRole('heading', {name: 'Commander Discovery', exact: true})).toBeVisible();
  await page.getByRole('button', {name: 'Run discovery'}).click();
  const categories = page.getByRole('navigation', {name: 'Finding categories'});
  await expect(categories.getByRole('button', {name: /May help/})).toHaveText('1May helpcards');
  await expect(categories.getByRole('button', {name: /Needs investigation/})).toHaveText('2Needs investigationcards');
  await expect(categories.getByRole('button', {name: /Abilities ruled out/})).toHaveText('1Abilities ruled outcards');
  await expect(page.getByText('These views overlap.', {exact: true})).toBeVisible();
  await categories.getByRole('button', {name: /Needs investigation/}).click();
  await page.getByLabel('Mechanical relationship').selectOption('Destruction');
  await expect(page.getByRole('button', {name: 'Mixed fixture', exact: true})).toHaveCount(0);
  const selected = page.getByRole('button', {name: 'Unresolved fixture', exact: true});
  await expect(selected).toHaveAttribute('aria-pressed', 'true');
  await expect(page.getByRole('region', {name: 'Result inspection'}).getByRole('heading', {name: 'Unresolved fixture'})).toBeVisible();
  await expect(page.getByText('Synthetic fixture: image identity is not established.')).toBeVisible();
  await page.getByRole('link', {name: 'Read selected card: Unresolved fixture ↓'}).click();
  await expect(page.getByRole('region', {name: 'Result inspection'})).toBeFocused();
});
