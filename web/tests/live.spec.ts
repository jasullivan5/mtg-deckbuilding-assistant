import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';
import type {Run, Evidence, CommanderView} from '../src/api';

test.skip(!process.env.EXPLORER_URL, 'Set EXPLORER_URL to a running Kotlin application.');
const objective = 'own-nontoken-creature-deaths';

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
  await expect(panel.locator('.contribution')).toHaveText('Sacrificing one of your nontoken creatures can trigger this ability.');
  await expect(panel.getByRole('button', {name: /Discovery/})).toHaveCount(0);
  await panel.getByRole('button', {name: /Rules comparison/}).click();
  const rules = panel.getByRole('region', {name: 'Rules comparison evidence'});
  await expect(rules.locator('.evidence-fields')).toContainText('Scryfall Oracle text');
  await expect(rules.locator('.evidence-fields')).toContainText('AI agent');
  await expect(rules.locator('.evidence-fields')).toContainText('Not separately identified');
  await expect(rules.locator('.evidence-fields')).toContainText('Not individually recorded');
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
      await expect(detail.locator('.unresolved-note')).toContainText('land would also have to be a nontoken creature');
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
        await expect(button).toContainText('No review recorded'); await expect(button).not.toContainText('✓');
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
