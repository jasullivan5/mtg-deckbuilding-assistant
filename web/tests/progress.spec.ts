import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';

test.skip(!process.env.EXPLORER_URL, 'Requires the running Kotlin Explorer.');

test('Progress opens without discovery and shows scoped Kotlin coverage trust gains and obstacles', async ({page}) => {
  test.setTimeout(240_000);
  let discovery = 0;
  page.on('request', r => {if (r.url().endsWith('/api/v1/discover')) discovery++;});
  const response = page.waitForResponse(r => r.url().endsWith('/api/v1/progress') && r.ok(), {timeout:180_000});
  await page.goto('/#progress');
  const data = await (await response).json();
  const dashboard = page.getByRole('article', {name:'Research Progress Dashboard'});
  await expect(dashboard).toBeVisible();
  await expect(dashboard.locator('#fidelity-pilot')).toContainText('reviewed ability scopes', {timeout:150_000});
  expect(data.mechanics.denominator).toBe(Object.values(data.mechanics.counts).reduce((sum: number,n) => sum + Number(n),0));
  expect(data.population.length).toBe(Object.values(data.definitionOrigins).reduce((sum: number,n) => sum + Number(n),0));
  await expect(dashboard.locator('#node-coverage')).toHaveAttribute('value', String(data.mechanics.counts.SUPPORTED));
  await expect(dashboard.locator('#node-coverage')).toHaveAttribute('max', String(data.mechanics.denominator));
  await expect(dashboard).toContainText('Not measured at catalog scale');
  await expect(dashboard).toContainText('not NO_MATCH');
  await expect(dashboard.getByRole('link', {name:'Read investigation'}).first()).toHaveAttribute('href', new RegExp(`/blob/${data.applicationRevision.replace(/\+dirty$/, '')}/`));
  expect(data.assurance.qualification.QUALIFIED).toBe(0);
  expect(data.assurance.notMeasured).toBeGreaterThan(0);
  if (data.external.state === 'MEASURED') {
    await expect(dashboard.locator('#external')).toContainText('confidently associated external physical identities');
    expect(data.external.counts.eligible_oracle_cards).toBeGreaterThan(data.external.counts.confident_native_identity_associations);
  } else await expect(dashboard.locator('#external')).toContainText('Not measured');
  await expect(dashboard.locator('#history')).toContainText('resolved 223 unknown pathways');
  await dashboard.getByText('Inspect gains, negative cases, regressions and comparison provenance',{exact:true}).click();
  await dashboard.getByLabel('Filter changed card').fill('Seal of Fire');
  await expect(dashboard.locator('#history')).toContainText('Seal of Fire · UNKNOWN → NO_MATCH');
  await dashboard.getByLabel('Reusable correction').selectOption('0');
  await dashboard.getByLabel('Filter changed card').fill('Twinshot Sniper');
  await expect(dashboard.locator('#history')).toContainText('Twinshot Sniper · NO_PRIOR_PATHWAY → MATCH');
  await dashboard.getByText('Inspect gains, negative cases, regressions and comparison provenance',{exact:true}).click();
  await dashboard.getByText('Inspect mechanical groups and representative cards',{exact:true}).click();
  await dashboard.getByLabel('Native type or representative card').fill('Pit Raptor');
  await expect(dashboard.locator('.progress-record').filter({hasText:'nodes /'}).first()).toBeVisible({timeout:90_000});
  await dashboard.getByText('Inspect mechanical groups and representative cards',{exact:true}).click();
  await dashboard.locator('#obstacles').getByText('Why it matters and a bounded investigation',{exact:true}).first().click();
  await expect(dashboard.locator('#obstacles')).toContainText('Tatterkite');
  await dashboard.locator('#obstacles').getByText('Why it matters and a bounded investigation',{exact:true}).first().click();
  await expect(dashboard.locator('#capabilities')).toContainText('Substitutions and displaced deck slots');
  await mkdir('../generated/explorer/progress',{recursive:true});
  await writeFile('../generated/explorer/progress/measured-report.json',JSON.stringify(data,null,2));
  for (const width of [1440,390]) {
    await page.setViewportSize({width,height:width===390?844:1000});
    await page.evaluate(() => window.scrollTo(0,0));
    expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBe(width);
    await page.screenshot({path:`../generated/explorer/progress/${width}-dashboard.png`,fullPage:true});
    await page.screenshot({path:`../generated/explorer/progress/${width}-first-screen.png`});
    const primary = page.getByRole('navigation',{name:'Primary navigation'});
    await expect(primary.getByRole('button')).toHaveCount(3);
    await primary.getByRole('button',{name:'Primer',exact:true}).focus();
    await page.keyboard.press('Enter');
    await expect(page.getByRole('article',{name:'Primer',exact:true})).toBeVisible();
    await primary.getByRole('button',{name:'Progress',exact:true}).click();
    await expect(dashboard).toBeVisible();
  }
  await page.getByRole('button',{name:'Discover',exact:true}).click();
  await expect(page.getByRole('button',{name:'Find cards',exact:true})).toBeVisible();
  expect(discovery).toBe(0);
});

test('Unavailable historical baseline stays separate from current measurements', async ({page}) => {
  await page.route('**/api/v1/progress/history', route => route.fulfill({status:404,contentType:'application/json',body:JSON.stringify({message:'No baseline fixture'})}));
  await page.goto('/#progress');
  await expect(page.getByText(/Historical baseline unavailable/)).toBeVisible({timeout:180_000});
  await expect(page.getByRole('heading',{name:'What can we represent?'})).toBeVisible();
  await expect(page.getByText('Source-card discard gained nine pathways',{exact:false})).toHaveCount(0);
});
