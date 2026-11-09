import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';
import type {Run, Evidence, CommanderView} from '../src/api';

test.skip(!process.env.EXPLORER_URL, 'Set EXPLORER_URL to a running Kotlin application.');
const objective = 'own-nontoken-creature-deaths';
const reviewedCoverageEngine = '03869c08665bf476a8b40dacbfbcd501c54a33bd';
const unsearchedCoverageCards = ['Grim Haruspex', 'Doomed Dissenter', 'Pitiless Plunderer', 'Reassembling Skeleton'];
const historicalGapLabel = '4 Outside current objective coverage';
const unexplainedLabel = '5 Insufficient evidence';
const historicalDiagnosis = 'leaves land and creature overlap unresolved';
const differentEngineDiagnosis = 'Inspect the current decision records. The retained diagnosis used a different engine revision.';

test('Mechanical Verification Lab connects real discovery, cost payment and target rejection', async ({page}) => {
  await page.goto('/');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('stock-own-graveyard');
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const name = page.getByRole('textbox', {name: 'Card name'});
  await name.fill('Mogg Fanatic');
  const inspector = page.getByRole('region', {name: 'Result inspection'});
  await expect(inspector.getByRole('heading', {name: 'Mogg Fanatic'})).toBeVisible();
  const investigate = inspector.getByRole('button', {name: 'Investigate execution'});
  await inspector.getByText('Archived execution investigation', {exact:true}).click();
  await investigate.focus();
  await page.keyboard.press('Enter');
  const lab = page.getByRole('region', {name: 'Mechanical Verification Lab', exact: true});
  await expect(lab.getByText('From discovery', {exact: false})).toBeVisible();
  const executed = page.waitForResponse(r => r.url().endsWith('/api/v1/lab/execute') && r.ok());
  await lab.getByRole('button', {name: 'Run scenario'}).click();
  const result = await (await executed).json();
  expect(result.scenarioId).toBe('mogg-pay');
  expect(result.execution).toBe('FRESH_NATIVE');
  await expect(lab.getByText('Mogg Fanatic was sacrificed.', {exact: true})).toBeVisible();
  await expect(lab.getByText('Source graveyard · Creature', {exact: false}).first()).toContainText('Opponent 20 life');
  await expect(lab.getByText('Mogg Fanatic dealt 1 damage to the opponent.')).toBeVisible();
  await expect(lab.getByText('Observed', {exact: true})).toHaveCount(2);
  await lab.getByText('Exact revisions, object references, native actions, events and state').click();
  await expect(lab.locator('.lab-technical')).toContainText(result.argentumCommit);
  await expect(lab.locator('.lab-technical')).toContainText('ZoneChangeEvent');
  await lab.getByText('Exact revisions, object references, native actions, events and state').click();
  await mkdir('../generated/explorer/lab', {recursive: true});
  await page.screenshot({path: '../generated/explorer/lab/desktop-mogg.png', fullPage: true});
  await lab.getByRole('button', {name: 'Omit the required target'}).click();
  await expect(lab.getByRole('heading', {name: 'What happened'})).toHaveCount(0);
  await lab.getByRole('button', {name: 'Run scenario'}).click();
  await expect(lab.getByText('Rejected by Argentum.', {exact: false})).toBeVisible();
  await expect(lab.getByText('Did not occur', {exact: true})).toHaveCount(2);

  await page.getByRole('button', {name: 'Back to discovery results'}).click();
  await expect(inspector.getByRole('heading', {name: 'Mogg Fanatic'})).toBeVisible();
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption(objective);
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await name.fill('Spark Elemental');
  await expect(inspector.getByRole('heading', {name: 'Spark Elemental'})).toBeVisible();
  await inspector.getByText('Archived execution investigation', {exact:true}).click();
  await inspector.getByRole('button', {name: 'Investigate execution'}).click();
  await expect(lab.getByRole('button', {name: 'Spark Elemental', exact: true})).toHaveAttribute('aria-pressed', 'true');
  await expect(lab.getByRole('heading', {name: 'What happened'})).toHaveCount(0);
});

test('Mechanical Verification Lab shows the deceptive land and both Spark outcomes at 390 pixels', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  await page.getByRole('button', {name: 'Mechanical Verification Lab', exact: true}).click();
  const lab = page.getByRole('region', {name: 'Mechanical Verification Lab', exact: true});
  await lab.getByRole('button', {name: 'Evolving Wilds', exact: true}).focus();
  await page.keyboard.press('Enter');
  const run = lab.getByRole('button', {name: 'Run scenario'});
  await run.focus();
  await page.keyboard.press('Enter');
  await expect(lab.getByText('Observed outcome agrees with the rules expectation')).toBeVisible();
  await expect(lab.getByText('Observed', {exact: true})).toHaveCount(1);
  await expect(lab.getByText('Did not occur', {exact: true})).toHaveCount(1);
  await expect(lab.getByText('graveyard · Land', {exact: true})).toBeVisible();
  await expect(lab.getByText('the basic land search still on the stack', {exact: false})).toBeVisible();
  await mkdir('../generated/explorer/lab', {recursive: true});
  await page.screenshot({path: '../generated/explorer/lab/mobile-wilds-390.png', fullPage: true});
  await lab.getByRole('button', {name: 'Spark Elemental', exact: true}).click();
  await run.click();
  await expect(lab.getByText('Spark Elemental was sacrificed.', {exact: true})).toBeVisible();
  await lab.getByRole('button', {name: 'Blink in response to the trigger'}).click();
  await run.click();
  await expect(lab.getByText('The permanent that returned is a new game object.', {exact: false})).toBeVisible();
  await expect(lab.getByText('battlefield · Creature — Elemental', {exact: true})).toHaveCount(2);
  await expect(lab.getByText('Spark Elemental was sacrificed.', {exact: true})).toHaveCount(0);
  await lab.getByText('Exact revisions, object references, native actions, events and state').click();
  await expect(lab.locator('.lab-technical')).toContainText('"generation"');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
});

test('independent graveyard discovery finds mill discard and sacrifice through Kotlin', async ({page}) => {
  const searches: string[] = [];
  page.on('request', request => {
    if (request.url().endsWith('/api/v1/discover')) searches.push(request.postDataJSON().objective);
  });
  await page.goto('/');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('stock-own-graveyard');
  const completed = page.waitForResponse(response => response.url().endsWith('/api/v1/discover') && response.ok());
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const run: Run = await (await completed).json();
  expect(searches).toEqual(['stock-own-graveyard']);
  for (const route of ['sacrifice', 'mill', 'discard']) {
    expect(run.counts[`accepted_state_${route}_pathways`]).toBeGreaterThan(0);
  }
  expect(run.cards.some(card => card.card === 'Tome Scour')).toBe(false);
  const results = page.getByRole('region', {name: 'Discovery results', exact: true});
  const detail = page.getByRole('region', {name: 'Result inspection'});
  const mechanic = results.getByRole('combobox', {name: 'Mechanic', exact: true});
  await mechanic.selectOption('Mill');
  const first = results.locator('button.card').first();
  await first.click();
  await expect(detail.locator('.objective-notes')).toContainText(/mill .* from your library/i);

  await page.getByLabel('Card name').fill("Stitcher's Supplier");
  await results.getByRole('button', {name: "Stitcher's Supplier", exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('When this permanent enters, mill 3 cards');
  await expect(detail.locator('.objective-notes')).toContainText('starts outside the battlefield');
  await detail.getByRole('combobox', {name: 'Ability to inspect', exact: true}).selectOption({index: 1});
  await expect(detail.locator('.objective-notes')).toContainText('When this permanent dies, mill 3 cards');
  await detail.getByRole('button', {name: /Discovery/}).click();
  await expect(detail.getByRole('region', {name: 'Discovery evidence'})).toContainText('Event comparison');
  await detail.screenshot({path: '../generated/explorer/state-discovery-desktop.png'});

  await page.getByLabel('Card name').fill('Faithless Looting');
  await mechanic.selectOption('Discard');
  await results.getByRole('button', {name: 'Faithless Looting', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('Discard 2 cards from your hand');
  await expect(detail.locator('.objective-notes')).toContainText('eligible creature card');
  await page.setViewportSize({width: 390, height: 844});
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await detail.screenshot({path: '../generated/explorer/state-discovery-mobile.png'});

  await page.setViewportSize({width: 1365, height: 950});
  await page.getByLabel('Card name').fill('Twinshot Sniper');
  await results.getByRole('button', {name: 'Twinshot Sniper', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('Discard this creature card from your hand to activate this ability');
  await expect(detail.locator('.objective-notes')).toContainText('any target requirements');
  await expect(detail.locator('.objective-notes')).toContainText('starts outside the battlefield');
  await detail.getByRole('button', {name: /Discovery/}).click();
  await expect(detail.getByRole('region', {name: 'Discovery evidence'})).toContainText('Event comparison');
  await detail.screenshot({path: '../generated/explorer/source-discard-desktop.png'});
  expect(run.cards.find(card => card.card === 'Sokenzan, Crucible of Defiance')?.pathways.MATCH ?? 0).toBe(0);

  await page.getByLabel('Card name').fill('Viscera Seer');
  await mechanic.selectOption('Sacrifice');
  await results.getByRole('button', {name: 'Viscera Seer', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('This same sacrifice can cause a death');
  await page.getByLabel('Card name').fill('Mogg Fanatic');
  await results.getByRole('button', {name: 'Mogg Fanatic', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('Sacrifice this permanent to activate this ability');
  await expect(detail.locator('.objective-notes')).toContainText('You must own the physical card');
  await expect(detail.locator('.objective-notes')).toContainText('Replacement effects can redirect the move');
  await detail.getByRole('button', {name: /Discovery/}).click();
  await expect(detail.getByRole('region', {name: 'Discovery evidence'})).toContainText('This same sacrifice can cause a death');
  await detail.screenshot({path: '../generated/explorer/source-sacrifice-desktop.png'});
  await page.setViewportSize({width: 390, height: 844});
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await detail.screenshot({path: '../generated/explorer/source-sacrifice-mobile.png'});
  await page.setViewportSize({width: 1365, height: 950});

  await page.getByLabel('Card name').fill('Spark Elemental');
  await results.getByRole('button', {name: 'Spark Elemental', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('Sacrifice this permanent when its trigger resolves');

  await page.getByRole('button', {name: /Needs interpretation/}).click();
  await page.getByLabel('Card name').fill('Pit Raptor');
  await results.getByRole('button', {name: 'Pit Raptor', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('leaves the creature-card arrival unresolved');

  await page.getByRole('button', {name: /All findings/}).click();
  await page.getByLabel('Card name').fill('Seal of Fire');
  await results.getByRole('button', {name: 'Seal of Fire', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('it is a noncreature card there');
  await mechanic.selectOption('Discard');
  await page.getByLabel('Card name').fill('Sokenzan, Crucible of Defiance');
  await results.getByRole('button', {name: 'Sokenzan, Crucible of Defiance', exact: true}).click();
  await expect(detail.locator('.objective-notes')).toContainText('which is a noncreature card');
  await detail.screenshot({path: '../generated/explorer/source-discard-nonmatch.png'});
});

test('discover conditional graveyard contributions from native Judith results', async ({page}) => {
  await page.goto('/');
  const completed = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const run: Run = await (await completed).json();
  expect(run.objective).toBe(objective);
  const found = run.cards.filter(card => card.conditionalGraveyardContribution);
  expect(found.length).toBeGreaterThan(3);
  expect(found.every(card => card.pathways.MATCH > 0)).toBe(true);
  const view = page.getByRole('button', {name: /Potential graveyard contributions/});
  await expect(view).toContainText(`${found.length} cards`);
  await view.click();
  const results = page.getByRole('region', {name: 'Discovery results', exact: true});
  const detail = page.getByRole('region', {name: 'Result inspection'});
  const relationship = detail.getByRole('region', {name: 'Graveyard contribution'});
  const firstCard = results.locator('button.card').first();
  await expect(firstCard).toHaveAccessibleName(found[0].card);
  await firstCard.click();
  await expect(relationship).toContainText('I have at least one creature card in my graveyard');
  await expect(relationship).toContainText('through the checkpoint');
  await expect(relationship).toContainText('finality counter');
  await expect(relationship).toContainText('animated land');
  await relationship.getByText('Native evidence', {exact: true}).click();
  await expect(relationship.getByRole('link', {name: 'Open authored native definition'}))
    .toHaveAttribute('href', new RegExp(run.argentumCommit));
  await relationship.getByText('Separate engine validation', {exact: true}).click();
  await expect(relationship).toContainText('previously false');
  await detail.screenshot({path: '../generated/explorer/relationship-desktop.png'});

  await page.getByLabel('Card name').fill('Viscera Seer');
  await results.getByRole('button', {name: 'Viscera Seer', exact: true}).click();
  await expect(relationship).toContainText('to activate the ability');
  await expect(relationship).toContainText('does not create that material');
  await expect(relationship).toContainText('source may be chosen');
  await page.setViewportSize({width: 390, height: 844});
  await expect.poll(() => page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await relationship.screenshot({path: '../generated/explorer/relationship-mobile.png'});

  await page.getByRole('button', {name: /Needs interpretation/}).click();
  await page.getByLabel('Card name').fill('Agent of Shauku');
  await results.getByRole('button', {name: 'Agent of Shauku', exact: true}).click();
  await expect(relationship).toContainText('Unresolved.');
  await page.getByRole('button', {name: /All findings/}).click();
  await page.getByLabel('Card name').fill('Murder');
  await results.getByRole('button', {name: 'Murder', exact: true}).click();
  await expect(detail.getByRole('heading', {name: 'Murder', exact: true})).toBeVisible();
  await expect(relationship).toHaveCount(0);
  expect(found.map(card => card.card)).not.toContain('Reassembling Skeleton');
});

test('all existing objectives changed commanders and blocked selections still execute genuine Kotlin discovery', async ({page}) => {
  const runs: Run[] = []; const errors: string[] = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('response', async r => {if (r.url().endsWith('/api/v1/discover') && r.ok()) runs.push(await r.json());});
  await page.goto('/');
  for (const id of ['create-creature-tokens', 'reanimate-own-creatures']) {
    await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption(id);
    await page.getByRole('button', {name: 'Find cards', exact: true}).click();
    await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  }
  await expect.poll(() => runs.length).toBe(2); expect(runs[1].runId).not.toBe(runs[0].runId);
  await page.getByRole('combobox', {name: 'Commander', exact: true}).fill('Oviya Pashiri, Sage Lifecrafter');
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('create-creature-tokens');
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect.poll(() => runs.length, {timeout: 130_000}).toBe(3);
  expect(runs[2].allowedIdentity).toEqual(['GREEN']);
  await page.getByRole('combobox', {name: 'Commander', exact: true}).fill('Murder');
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect(page.getByRole('region', {name: 'Selected commander'})).toContainText('Selection blocked');
  expect(runs).toHaveLength(3); expect(errors).toEqual([]);
});

test('commander and objective disclosures work before discovery with precise review fields at 390 pixels', async ({page}) => {
  let searches = 0; page.on('request', r => {if (r.url().endsWith('/discover')) searches++;});
  await page.setViewportSize({width: 390, height: 844}); await page.goto('/');
  for (const destination of ['Discover', 'Progress', 'Primer']) {
    await page.keyboard.press('Tab');
    await expect(page.getByRole('navigation', {name: 'Primary navigation'}).getByRole('button', {name: destination, exact: true})).toBeFocused();
  }
  await page.keyboard.press('Tab'); await expect(page.getByRole('combobox', {name: 'Commander', exact: true})).toBeFocused();
  await page.keyboard.press('Tab'); await expect(page.getByRole('combobox', {name: 'Objective', exact: true})).toBeFocused();
  await page.keyboard.press('Tab'); await expect(page.getByRole('button', {name: 'Find cards', exact: true})).toBeFocused();
  await page.keyboard.press('Tab'); await expect(page.getByText('About commander', {exact: true})).toBeFocused();
  await page.keyboard.press('Enter');
  const panel = page.getByRole('region', {name: 'Selected commander'});
  await expect(panel.locator('.card-properties')).toContainText('Black, Red');
  const art = panel.getByRole('img', {name: /^Judith/}); await expect(art).toBeVisible({timeout: 30_000});
  await expect.poll(() => art.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0), {timeout: 30_000}).toBe(true);
  await expect(panel.getByRole('img', {name: 'Relevant to this objective', exact: true})).toHaveCount(1);
  await expect(panel.getByText('Whenever a nontoken creature you control dies, Judith deals 1 damage to any target.', {exact: true})).toHaveCount(1);
  await expect(panel.locator('.contribution')).toHaveCount(0);
  await expect(panel.getByRole('button', {name: /Discovery/})).toHaveCount(0);
  await panel.getByRole('button', {name: /Rules comparison/}).click();
  const rules = panel.getByRole('region', {name: 'Rules comparison evidence'});
  await expect(rules.locator('.evidence-fields')).toContainText('Scryfall Oracle text');
  await expect(rules.locator('.evidence-fields')).toContainText('AI agent');
  await expect(rules.locator('.evidence-fields')).toContainText('Not captured separately');
  await expect(rules.locator('.evidence-fields')).toContainText('no ability-by-ability links');
  await expect(rules.getByRole('link', {name: /Scryfall Oracle text/})).toHaveAttribute('href', /src\/test\/resources\/cards\/judith-the-scourge-diva-source\.json/);
  await panel.getByRole('button', {name: /Engine tests/}).click();
  const engine = panel.getByRole('region', {name: 'Engine tests evidence'});
  await expect(engine).toContainText('Different from the current version');
  await engine.getByText('Technical evidence and recorded interactions', {exact: true}).click();
  await expect(engine).toContainText('4133241856227d1d36269e92aaba633bbaf93f5c');
  await engine.getByText('Technical evidence and recorded interactions', {exact: true}).click();
  expect(searches).toBe(0);
  await page.getByText('About objective', {exact: true}).click();
  await expect(page.locator('.objective-help li')).toHaveCount(3);
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/hierarchy-mobile-commander.png', fullPage: true});
});

test('five representative cards retain concise conditions honest evidence and investigable concerns', async ({page}) => {
  const errors: string[] = []; page.on('pageerror', e => errors.push(e.message));
  await page.goto('/'); const completed = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button', {name: 'Find cards', exact: true}).click(); const run: Run = await (await completed).json();
  expect(run.objective).toBe(objective);
  const nav = page.getByRole('navigation', {name: 'Finding categories', exact: true});
  await expect(nav.getByRole('button', {name: /Identified mechanics/}).locator('.category-count')).toHaveText(run.cards.filter(c => c.pathways.MATCH > 0).length.toLocaleString());
  const detail = page.getByRole('region', {name: 'Result inspection'});
  const audit: Record<string, {defaultText: string; evidence: Record<string, string>}> = {};
  const snapshots: Record<string, Evidence> = {};
  for (const [name, category, slug] of [
    ['Accursed Centaur', 'Identified mechanics', 'centaur'], ['Attrition', 'Identified mechanics', 'attrition'],
    ['Acolyte of Aclazotz', 'Identified mechanics', 'acolyte'], ['Agent of Shauku', 'Needs interpretation', 'agent'], ['Tatterkite', 'All findings', 'concern'],
  ]) {
    await nav.getByRole('button', {name: new RegExp(category)}).click(); await page.getByLabel('Card name').fill(name);
    const index = run.cards.find(c => c.card === name)!.index;
    const data: Evidence = await (await page.request.get(`/api/v1/runs/${run.runId}/cards/${index}`)).json(); snapshots[name] = data;
    await expect(detail.getByRole('heading', {name, exact: true})).toBeVisible();
    await expect(detail.locator('.rules-text')).toContainText(data.authoredCard!.text.split('\n')[0]);
    for (const line of data.rules) await expect(detail.getByText(line.text, {exact: true})).toHaveCount(1);
    const art = detail.getByRole('img', {name: new RegExp(`^${name}`)}); await expect(art).toBeVisible({timeout: 30_000});
    await expect.poll(() => art.evaluate((img: HTMLImageElement) => img.complete && img.naturalWidth > 0), {timeout: 30_000}).toBe(true);
    if (name === 'Accursed Centaur') {
      await expect(detail.getByRole('img', {name: 'Relevant to this objective', exact: true})).toHaveCount(1);
      await expect(detail.locator('.objective-notes')).toContainText('mandatory when the ability resolves');
      expect((await detail.innerText()).match(/enters the battlefield/g)).toHaveLength(1);
    }
    if (name === 'Attrition') {
      await expect(detail.locator('.objective-notes')).toContainText('legal target before activation');
      await expect(detail.locator('.unresolved-note')).toContainText('nonblack creature');
      expect(data.results.map(r => r.verdict)).toEqual(['MATCH', 'NO_MATCH']);
      await expect(detail.getByRole('button', {name: /Discovery/})).toContainText('Creature sacrifice cost identified');
    }
    if (name === 'Acolyte of Aclazotz') await expect(detail.locator('.objective-notes')).toContainText('noncreature artifact or token');
    if (name === 'Agent of Shauku') {
      await expect(detail.getByRole('img', {name: 'Relevance unresolved', exact: true})).toHaveCount(1);
      await expect(detail.locator('.unresolved-note')).toContainText('land must also be a nontoken creature');
      await expect(detail.getByRole('button', {name: /Discovery/})).toContainText('Affected creature type unresolved');
    }
    if (name === 'Tatterkite') {
      await expect(detail.locator('.recorded-problem')).toContainText('no counters may be placed on Tatterkite');
      await expect(detail.locator('.recorded-problem')).toContainText('Open historical problem');
    }
    audit[name] = {defaultText: await detail.innerText(), evidence: {}};
    await detail.screenshot({path: `../generated/explorer/hierarchy-${slug}.png`});
    for (const label of ['Discovery', 'Rules comparison', 'Engine tests', 'Independent verification']) {
      const button = detail.getByRole('button', {name: new RegExp(label)}); if (!await button.count()) continue;
      await button.click(); audit[name].evidence[label] = await detail.locator('.evidence-panel').innerText();
      if (name === 'Attrition' && label === 'Rules comparison') {
        await expect(button).toContainText('No formal comparison recorded'); await expect(button).not.toContainText('✓');
      }
      await button.click();
    }
  }
  const view: CommanderView = await (await page.request.post('/api/v1/commander-view', {data: {commander: run.commander, objective: run.objective}})).json();
  await mkdir('../generated/explorer', {recursive: true});
  await writeFile('../generated/explorer/hierarchy-reading-audit.json', JSON.stringify({observedAt: new Date().toISOString(), applicationRevision: run.applicationRevision, engineRevision: run.argentumCommit, runId: run.runId, cards: audit, commander: view, snapshots, errors}, null, 2));
  await writeFile('../generated/explorer/hierarchy-report.json', await (await page.request.get(`/api/v1/runs/${run.runId}/report`)).body());
  expect(errors).toEqual([]);
});

test('keyboard card selection focuses the compact inspector without a jump link at 390 pixels', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844}); await page.goto('/'); await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  await page.getByLabel('Card name').fill('Attrition');
  const button = page.getByRole('button', {name: 'Attrition', exact: true}); await button.focus(); await page.keyboard.press('Enter');
  const detail = page.getByRole('region', {name: 'Result inspection'}); await expect(detail).toBeFocused();
  await expect(detail.locator('.objective-notes')).toContainText('legal target before activation');
  await expect(page.getByRole('link', {name: /Read selected card/})).toHaveCount(0);
  const review = detail.getByRole('button', {name: /Rules comparison/}); await review.focus(); await page.keyboard.press('Enter');
  await expect(review).toHaveAttribute('aria-expanded', 'true');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await page.screenshot({path: '../generated/explorer/hierarchy-mobile-card.png', fullPage: true});
});


test('source investigation distinguishes death sources removal policy and omitted non-removal payoffs', async ({page}) => {
  await page.goto('/'); await page.getByText('About objective', {exact: true}).click();
  await expect(page.locator('.objective-help')).not.toContainText('not searched as independent payoffs');
  await expect(page.locator('.objective-help li')).toHaveCount(3);
  const completed=page.waitForResponse(r=>r.url().endsWith('/api/v1/discover')&&r.ok());
  await page.getByRole('button',{name:'Find cards',exact:true}).click();const run:Run=await(await completed).json();
  const detail=page.getByRole('region',{name:'Result inspection'});const nav=page.getByRole('navigation',{name:'Finding categories',exact:true});
  const cases:Record<string,unknown>={};
  for(const [name,expected] of [['Accursed Centaur','sacrificed and die'],['Judith, the Scourge Diva','does not cause that first death'],['Tragic Slip','enable or improve this removal'],['Murder','choosing your own creature'],['Midnight Reaper','damage affects a player'],['Blood Artist','No discovery relationship was evaluated']]) {
    await nav.getByRole('button',{name:/All findings/}).click();await page.getByLabel('Card name').fill(name);
    await expect(detail.getByRole('heading',{name,exact:true})).toBeVisible();
    if(name==='Blood Artist'){await expect(detail).toContainText(expected);continue;}
    const button=detail.getByRole('button',{name:/Discovery/});await button.click();
    const panel=detail.getByRole('region',{name:'Discovery evidence'});await expect(panel).toContainText(expected);
    cases[name]={defaultText:await detail.innerText(),fields:await panel.innerText()};
    if(name==='Accursed Centaur') {await expect(panel).toContainText('conditional on reaching the graveyard');await expect(panel).toContainText('Source enters the battlefield; sacrifice required');}
    if(name==='Murder') {
      await expect(button).toContainText('Excluded by objective policy');
      await expect(button).not.toContainText('✓');
    }
  }
  expect(run.cards.find(c=>c.card==='Pitiless Plunderer')).toBeUndefined();
  const draw=run.cards.find(c=>c.card==='Grim Haruspex');expect(draw?.pathways.MATCH??0).toBe(0);
  await writeFile('../generated/explorer/coverage-observation.json',JSON.stringify({applicationRevision:run.applicationRevision,engineRevision:run.argentumCommit,runId:run.runId,cases,omissions:['Pitiless Plunderer has no finding','Grim Haruspex has no accepted payoff']},null,2));
  await writeFile('../generated/explorer/coverage-report.json',await(await page.request.get(`/api/v1/runs/${run.runId}/report`)).body());
  await page.getByRole('combobox',{name:'Objective',exact:true}).selectOption('create-creature-tokens');await page.getByRole('button',{name:'Find cards',exact:true}).click();
  await expect(page.getByRole('region',{name:'Completed discovery'})).toBeVisible({timeout:130_000});await page.getByLabel('Card name').fill('Doomed Dissenter');
  await expect(page.getByRole('button',{name:'Doomed Dissenter',exact:true})).toBeVisible();
});

test('sacrifice type boundaries retain real restrictions and renewable creatures stay within existing coverage', async ({page}) => {
  await page.goto('/');
  const completed = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const run: Run = await (await completed).json();
  const detail = page.getByRole('region', {name: 'Result inspection'});
  const nav = page.getByRole('navigation', {name: 'Finding categories', exact: true});
  const observations: Record<string, string> = {};
  for (const [name, expected] of [
    ['Viscera Seer', 'Optional activation; repeat only when the source is available'],
    ['Akki Scrapchomper', 'Artifact or land sacrifice as a cost'],
    ['Atog', 'Artifact sacrifice as a cost'],
    ['Claws of Gix', 'Permanent sacrifice as a cost'],
    ['Agent of Shauku', 'Land sacrifice as a cost'],
  ]) {
    await nav.getByRole('button', {name: /All findings/}).click();
    await page.getByLabel('Card name').fill(name);
    await expect(detail.getByRole('heading', {name, exact: true})).toBeVisible();
    await detail.getByRole('button', {name: /Discovery/}).click();
    await expect(detail.getByRole('region', {name: 'Discovery evidence'})).toContainText(expected);
    if (name === 'Akki Scrapchomper') {
      await expect(detail.locator('.objective-notes')).toContainText('without animation');
      await expect(detail.locator('.objective-notes')).toContainText('Sacrificing a noncreature permanent');
      await expect(detail.getByRole('region', {name: 'Discovery evidence'})).toContainText('Artifact-creature choice; land/creature overlap remains unresolved');
      await expect(detail.getByRole('button', {name: /Discovery/})).toContainText('Artifact or land sacrifice cost identified');
    }
    if (name === 'Agent of Shauku') await expect(detail.locator('.unresolved-note')).toContainText('inherently or through a type-changing effect');
    observations[name] = await detail.innerText();
    await detail.screenshot({path: `../generated/explorer/boundary-${name.toLowerCase().replaceAll(' ', '-')}.png`});
  }
  await page.getByText('Search scope, counts, exclusions and source revisions', {exact: true}).click();
  await expect(page.locator('.provenance')).toContainText('not searched as independent payoffs');
  await writeFile('../generated/explorer/boundary-report.json', await (await page.request.get(`/api/v1/runs/${run.runId}/report`)).body());
  await page.getByRole('combobox', {name: 'Objective', exact: true}).selectOption('reanimate-own-creatures');
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  for (const name of ['Reassembling Skeleton', 'Bloodghast']) {
    await page.getByLabel('Card name').fill(name);
    await expect(detail.getByRole('heading', {name, exact: true})).toBeVisible();
    observations[name] = await detail.innerText();
  }
  await writeFile('../generated/explorer/boundary-reading.json', JSON.stringify({applicationRevision: run.applicationRevision, engineRevision: run.argentumCommit, observations}, null, 2));
});

test('Discovery Coverage counts and filters the fixed cohort and inspects recognized, omitted and unresolved cards', async ({page}) => {
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  const completed = page.waitForResponse(response => response.url().endsWith('/api/v1/discover') && response.ok());
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  const run: Run = await (await completed).json();
  const reviewed = run.argentumCommit === reviewedCoverageEngine;
  const omissionLabel = reviewed ? historicalGapLabel : unexplainedLabel;
  await page.getByRole('button', {name: 'Discovery Coverage', exact: true}).click({timeout: 130_000});
  const nav = page.getByRole('navigation', {name: 'Coverage categories'});
  const list = page.getByRole('region', {name: 'Cohort cards'});
  const detail = page.getByRole('region', {name: 'Coverage card inspection'});
  const groups = [
    ['4 Recognized contribution', ['Accursed Centaur', 'Viscera Seer', 'Attrition', 'Akki Scrapchomper']],
    ['1 Unresolved relationship', ['Agent of Shauku']],
    ['1 Excluded by objective policy', ['Murder']],
    ['1 Excluded by Commander eligibility', ['Dryad Arbor']],
    [omissionLabel, reviewed ? unsearchedCoverageCards : [...unsearchedCoverageCards, 'Walking Corpse']],
    ...(reviewed ? [['1 Ordinary creature body', ['Walking Corpse']] as const] : []),
  ] as const;
  await expect(nav.getByRole('button', {name: '12 All cards'})).toBeVisible();
  for (const [label, names] of groups) {
    await nav.getByRole('button', {name: label, exact: true}).click();
    await expect(list.getByRole('button')).toHaveCount(names.length);
    for (const name of names) await expect(list.getByRole('button', {name, exact: true})).toBeVisible();
  }
  if (!reviewed) {
    await expect(nav.getByRole('button', {name: '0 Outside current objective coverage', exact: true})).toBeDisabled();
    await expect(nav.getByRole('button', {name: '0 Ordinary creature body', exact: true})).toBeDisabled();
  }
  await nav.getByRole('button', {name: '4 Recognized contribution', exact: true}).click();
  await list.getByRole('button', {name: 'Viscera Seer'}).click();
  await expect(detail.getByRole('button', {name: /Engine tests/})).toBeVisible();
  await expect(detail.getByRole('button', {name: /Discovery.*Creature sacrifice cost identified/})).toBeVisible();
  await expect(detail).toContainText(reviewed ? 'activation cost' : differentEngineDiagnosis);
  await nav.getByRole('button', {name: omissionLabel, exact: true}).click();
  await list.getByRole('button', {name: 'Reassembling Skeleton'}).click();
  await expect(detail).toContainText('In the ordinary findings index');
  await expect(detail).toContainText('No pathway check for the reviewed relationship');
  await list.getByRole('button', {name: 'Pitiless Plunderer'}).click();
  await expect(detail).toContainText('Absent from the ordinary findings index');
  await expect(detail).toContainText('No pathway check for the reviewed relationship');
  if (!reviewed) await expect(detail).toContainText('The available records do not establish the outcome for this relationship.');
  await expect(detail.getByRole('button', {name: /Rules comparison/})).toBeVisible();
  await detail.getByText('Retained Oracle text and identity').click();
  await expect(detail.getByRole('link', {name: 'Scryfall printing record'})).toBeVisible();
  await nav.getByRole('button', {name: '1 Unresolved relationship', exact: true}).click();
  await expect(detail).toContainText(reviewed ? historicalDiagnosis : differentEngineDiagnosis);
  await page.getByRole('button', {name: 'Back to discovery results'}).click();
  await expect(page.getByRole('navigation', {name: 'Finding categories', exact: true})).toBeVisible();
});

test('Integration Change Case Study compares historical records and preserves live discovery', async ({page}) => {
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  const entry = page.getByRole('button', {name: 'Open Integration Change Case Study'});
  await entry.focus();
  await page.keyboard.press('Enter');
  const study = page.getByRole('region', {name: 'When a return trigger works from the graveyard'});
  await expect(study).toContainText('HISTORICAL DISCOVERY COMPARISON');
  await expect(study).toContainText('6,111 eligible physical card inputs');
  await expect(study).toContainText('95 → 104');
  await expect(study).toContainText('97 → 106');
  await expect(study).toContainText('93 → 93');
  await expect(study).toContainText('20 shared movement facts changed');
  const controls = study.getByRole('navigation', {name: 'Representative cards'});
  const selected = (name: string) => study.getByRole('region', {name, exact: true});
  await expect(selected('Bloodghast')).toContainText('Excluded → Recognized');
  await expect(selected('Bloodghast')).toContainText('No independently qualified requirement');
  await expect(selected('Bloodghast')).toContainText('historical runs executed discovery');
  const skeleton = controls.getByRole('button', {name: 'Reassembling Skeleton'});
  await skeleton.focus();
  await page.keyboard.press('Space');
  await expect(selected('Reassembling Skeleton')).toBeFocused();
  await expect(selected('Reassembling Skeleton')).toContainText('Recognized → Recognized');
  await controls.getByRole('button', {name: 'Blood Speaker'}).click();
  await expect(selected('Blood Speaker')).toContainText('Excluded → Excluded');
  await expect(selected('Blood Speaker')).toContainText('movement fact changed');
  await controls.getByRole('button', {name: 'Wolfbat'}).click();
  await expect(selected('Wolfbat')).toContainText('draw your second card');
  await expect(selected('Wolfbat')).toContainText('Excluded → Recognized');
  await expect(selected('Wolfbat')).toContainText('finality counter');
  await study.getByText('Browse all measured changes and controls').click();
  await study.getByRole('button', {name: "All Hallow's Eve", exact: true}).click();
  await selected("All Hallow's Eve").getByText('Source provenance, native action and recorded before/after evidence').click();
  await expect(selected("All Hallow's Eve")).toContainText('Unresolved → Unresolved');
  await study.getByText('Comparison conditions and receipt hashes').click();
  await expect(study).toContainText('03869c08665bf476a8b40dacbfbcd501c54a33bd');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
  await page.setViewportSize({width: 1365, height: 950});
  await expect(study).toBeVisible();
  await page.getByRole('button', {name: 'Close Integration Change Case Study'}).click();
  await page.getByRole('button', {name: 'Find cards', exact: true}).click();
  await expect(page.getByRole('region', {name: 'Completed discovery'})).toBeVisible({timeout: 130_000});
  await expect(page.getByRole('button', {name: 'Discovery Coverage', exact: true})).toBeVisible();
});

for (const width of [1440, 390]) test(`Primer separates current discovery, intended layering and future experiments at ${width}px`, async ({page}) => {
  let searches = 0;
  page.on('request', r => {if(r.url().endsWith('/api/v1/discover')) searches++;});
  await page.setViewportSize({width, height: width === 390 ? 844 : 1000});
  await page.goto('/#primer');
  const primer = page.getByRole('article', {name: 'Primer', exact: true});
  await expect(primer.getByRole('heading', {name: 'How Card Integration Works'})).toBeVisible();
  const map = primer.getByRole('list', {name: 'From card rules to deck choices'});
  await expect(map.getByRole('button')).toHaveCount(5);
  await expect(map.getByRole('button', {name: /Strategic relationships/})).toHaveCount(0);
  await primer.getByRole('button', {name: /Native definitions/}).click();
  await expect(primer).toContainText('input to gameplay and to mechanical extraction');
  const layering = map.getByRole('button', {name: /Objective discovery, subroles and composition/});
  await layering.focus(); await page.keyboard.press('Enter');
  await expect(layering).toHaveAttribute('aria-pressed', 'true');
  await expect(primer).toContainText('Four queries implemented; role filtering and composition intended');
  await expect(primer).toContainText('not implemented');
  await map.getByRole('button', {name: /Experimental deck evaluation/}).click();
  await expect(primer).toContainText('Future research hypothesis');
  await expect(primer).toContainText('No deck evaluator, objective-aware pilot or deck simulation is implemented');
  await expect(primer).toContainText('not currently a prerequisite');
  await primer.getByText('Our working six-role vocabulary', {exact:true}).click();
  const roles = primer.locator('section', {has: primer.getByRole('heading', {name: 'Take the roles your objective needs'})});
  for (const role of ['Provider', 'Event responder', 'Event facilitator', 'State facilitator', 'State responder', 'Modifier']) {
    await expect(roles.locator('dt').filter({hasText: new RegExp(`^${role}$`)})).toBeVisible();
  }
  await expect(roles).toContainText('not a filter available today');
  await primer.getByText('Why mechanical contributions need context', {exact:true}).click();
  await primer.getByRole('button', {name: /Provider.*Reassembling Skeleton/}).click();
  await expect(primer).toContainText('renewable creature fuel is not yet recognized by the death query');
  await primer.getByRole('button', {name: /Event responder.*Grim Haruspex/}).click();
  await expect(primer).toContainText('not a current death-payoff discovery result');
  await expect(primer).toContainText('inherited catalog fidelity is not measured');
  await expect(primer).toContainText('separate human must explicitly approve');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(width);
  await mkdir('../generated/explorer/primer', {recursive:true});
  await primer.getByText('Why mechanical contributions need context', {exact:true}).click();
  await primer.getByText('Our working six-role vocabulary', {exact:true}).click();
  await page.evaluate(() => window.scrollTo(0,0));
  await page.screenshot({path:`../generated/explorer/primer/${width}-overview.png`, fullPage:true});
  await primer.getByText('Our working six-role vocabulary', {exact:true}).click();
  await roles.screenshot({path:`../generated/explorer/primer/${width}-roles.png`});
  await page.getByRole('button', {name: 'Discover', exact:true}).click();
  await expect(page.getByRole('button', {name:'Find cards',exact:true})).toBeVisible();
  expect(searches).toBe(0);
});

test('contribution study compares roles against the same mechanics without a new discovery run', async ({page}) => {
  let discoveryRequests = 0;
  page.on('request', request => {if (request.url().endsWith('/api/v1/discover')) discoveryRequests++;});
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  const open = page.getByRole('button', {name: 'Open Contribution Model Study'});
  await open.focus();
  await page.keyboard.press('Enter');
  const study = page.getByRole('region', {name: 'How do cards contribute to a strategy?'});
  const card = study.getByRole('combobox', {name: 'Reviewed card'});
  const five = study.getByRole('region', {name: 'Model A'});
  const four = study.getByRole('region', {name: 'Model B'});
  await expect(study).toContainText('Renewable nontoken creature material');
  await card.selectOption({label: 'Pitiless Plunderer'});
  await expect(four).toContainText('Respond to Event + Provide');
  await expect(study).toContainText('relationship is unqueried');
  await card.selectOption({label: 'Murder'});
  await expect(study).toContainText('deliberately excluded by policy');

  await study.getByRole('button', {name: 'Events versus states'}).click();
  await expect(study.getByRole('heading', {name: 'Goblin Chieftain'})).toBeVisible();
  await expect(four).toContainText('bonus per recipient is fixed');
  await card.selectOption({label: 'Cranial Plating'});
  await expect(five).toContainText('Modify + Leverage State');
  await expect(four).toContainText('Continuous quantity dependency');
  await card.selectOption({label: 'Scute Swarm'});
  await expect(four).toContainText('Resolution threshold selects output identity');
  await expect(study).toContainText('No live query runs for this objective');
  await study.getByRole('button', {name: 'Held-out controls'}).click();
  await expect(study).toContainText('Possible fidelity concern');
  await card.selectOption({label: 'Tragic Slip'});
  await expect(four).toContainText('Earlier-event history');
  await expect(four).not.toContainText('Respond to Event');
  await expect(study.getByText('Provisional recommendation.')).toBeHidden();
  await study.getByText('What does the sample suggest?').click();
  await expect(study.getByText('Provisional recommendation.')).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
  expect(discoveryRequests).toBe(0);
  await page.setViewportSize({width: 1365, height: 950});
  await expect(study).toBeVisible();
});

test('contribution follow-up distinguishes state access from scaling and keeps mechanics stable across lenses', async ({page}) => {
  let discoveryRequests = 0;
  page.on('request', request => {if (request.url().endsWith('/api/v1/discover')) discoveryRequests++;});
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  await page.getByRole('button', {name: 'Open Contribution Model Study'}).click();
  const study = page.getByRole('region', {name: 'How do cards contribute to a strategy?'});
  await study.getByRole('button', {name: 'Events versus states'}).click();
  const card = study.getByRole('combobox', {name: 'Reviewed card'});
  const five = study.getByRole('region', {name: 'Model A'});
  const four = study.getByRole('region', {name: 'Model B'});
  await card.selectOption({label: 'Flamewake Phoenix'});
  await expect(five).toContainText('Leverage State');
  await expect(four).toContainText('Binary access at two checkpoints');
  await expect(study).toContainText('power 4 or power 8');
  await expect(study).toContainText('boost that starts only after attacking arrives too late');
  await expect(study).toContainText('option and mana to pay {R}');
  await expect(study).toContainText('did not establish the Chieftain connection');
  await card.selectOption({label: 'Coat of Arms'});
  await expect(four).toContainText('across controllers');
  await card.selectOption({label: 'Zulaport Cutthroat'});
  const capability = study.getByRole('listitem').filter({hasText: 'Capability'});
  const mechanic = await capability.innerText();
  await expect(study).toContainText('Get a life-total payoff from deaths you cause.');
  const lens = study.getByRole('button', {name: 'Life-gain lens'});
  await lens.focus();
  await page.keyboard.press('Space');
  await expect(lens).toHaveAttribute('aria-pressed', 'true');
  await expect(study).toContainText('Get repeated life-gain events.');
  await expect(capability).toHaveText(mechanic, {useInnerText: true});
  await expect(five).not.toContainText('Respond to Event + Leverage State');
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
  expect(discoveryRequests).toBe(0);
});

test('Primary navigation preserves live discovery at desktop and 390 px', async ({page}) => {
  await page.goto('/');
  await page.getByRole('button', {name:'Find cards',exact:true}).click();
  const inspection = page.getByRole('region', {name:'Result inspection'});
  await page.getByLabel('Card name').fill('Viscera Seer');
  await expect(inspection.getByRole('heading',{name:'Viscera Seer',exact:true})).toBeVisible();
  for (const width of [1440,390]) {
    await page.setViewportSize({width,height:950});
    await page.getByRole('button',{name:'Primer',exact:true}).click();
    await expect(page.getByRole('article',{name:'Primer',exact:true})).toBeVisible();
    await page.getByRole('button',{name:'Discover',exact:true}).click();
    await expect(page.getByLabel('Card name')).toHaveValue('Viscera Seer');
    await expect(inspection.getByRole('heading',{name:'Viscera Seer',exact:true})).toBeVisible();
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(width);
  }
});

test('event-to-state research distinguishes observed death and checkpoint state', async ({page}) => {
  const searches: string[] = [];
  page.on('request', request => {
    if (request.url().endsWith('/api/v1/discover')) searches.push(request.url());
  });
  await page.setViewportSize({width: 390, height: 844});
  await page.goto('/');
  await page.getByText('Developer research index', {exact:true}).click();
  await page.getByRole('button', {name: 'Open Contribution Model Study'}).click();
  await page.getByRole('button', {name: 'Open event-to-state experiment'}).click();
  const study = page.getByRole('region', {name: 'Can a death establish a graveyard state?'});
  const observation = study.getByRole('region', {name: 'Selected observation'});
  const event = observation.getByRole('region', {name: 'Event outcome'}).locator('p');
  const state = observation.getByRole('region', {name: 'State outcome'}).locator('p');
  await expect(study).toContainText('RETAINED EXECUTION RECORD');
  await expect(event).toHaveText('Satisfied');
  await expect(state).toHaveText('Satisfied');
  await expect(observation).toContainText('still supplies the state');

  const borrowed = study.getByRole('button', {name: 'Opponent-owned creature'});
  await borrowed.focus();
  await page.keyboard.press('Space');
  await expect(borrowed).toHaveAttribute('aria-pressed', 'true');
  await expect(event).toHaveText('Satisfied');
  await expect(state).toHaveText('Not satisfied');
  await expect(observation).toContainText('Battlefield → opponent’s graveyard');
  await expect(observation).toContainText('The graveyard state was unsatisfied');

  await study.getByRole('button', {name: 'Creature token'}).click();
  await expect(event).toHaveText('Not satisfied');
  await expect(state).toHaveText('Not satisfied');
  await expect(observation).toContainText('The tracked token is gone');

  await study.getByRole('button', {name: 'Finality counter'}).click();
  await expect(event).toHaveText('Not satisfied');
  await expect(state).toHaveText('Not satisfied');
  await expect(observation).toContainText('Battlefield → exile');
  await observation.getByText('Rules-derived expectation and recorded inputs').click();
  await expect(observation.getByText(/Finality counters at departure: 1/)).toBeVisible();

  await study.getByRole('button', {name: 'Mill a creature card'}).click();
  await expect(event).toHaveText('Not satisfied');
  await expect(state).toHaveText('Satisfied');
  await expect(observation).toContainText('This move did not meet the death criterion');
  await observation.getByText('Rules-derived expectation and recorded inputs').click();
  await expect(observation.getByText(/Retained PR #30 verdicts/)).toBeVisible();

  await study.getByRole('button', {name: 'Animated land'}).click();
  await expect(event).toHaveText('Satisfied');
  await expect(state).toHaveText('Not satisfied');
  await expect(observation).toContainText('in graveyard with card types land');

  await study.getByRole('button', {name: 'Prefilled graveyard'}).click();
  await expect(event).toHaveText('Satisfied');
  await expect(state).toHaveText('Satisfied');
  await expect(observation).toContainText('Graveyard state before the move. Satisfied.');
  await expect(observation).toContainText('The state was satisfied independently of this arrival');
  await study.getByText('Evidence and limits of this comparison').click();
  await expect(study.getByText(/Scenario application/)).toBeVisible();
  expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(390);
  await page.getByRole('button', {name: 'Back to role comparison'}).click();
  await expect(page.getByText('Choose a research comparison')).toBeVisible();
  expect(searches).toEqual([]);
});
