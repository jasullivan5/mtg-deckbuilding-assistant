import {expect,test} from '@playwright/test';
import {mkdir,writeFile} from 'node:fs/promises';
test.skip(!process.env.EXPLORER_URL,'Requires running Kotlin Explorer with an explicit compatible snapshot.');

test('Cards Commanders Objectives and reusable fidelity expose actual separate populations',async({page})=>{
  test.setTimeout(300_000);
  await page.goto('/#progress');
  const dashboard=page.getByRole('article',{name:'Research Progress Dashboard'});
  await expect(dashboard.locator('#fidelity-pilot')).toContainText('reviewed ability scopes',{timeout:220_000});
  await expect(page.getByRole('status')).toHaveCount(0);
  const report=await(await page.request.get('/api/v1/progress')).json();
  const commanders=await(await page.request.get('/api/v1/progress/commanders')).json();
  const objectives=await(await page.request.get('/api/v1/progress/objectives')).json();
  const pilot=await(await page.request.get('/api/v1/progress/pilot')).json();
  const baselineResponse=await page.request.get('/api/v1/progress/global-baseline');expect(baselineResponse.ok()).toBe(true);
  const baseline=await baselineResponse.json();
  expect(baseline.records.filter((r:{poolState:string})=>r.poolState==='ELIGIBLE').length).toBe(report.global.counts.eligible_oracle_cards);
  expect(report.global.counts.eligible_oracle_cards).toBeGreaterThan(report.external.counts.eligible_oracle_cards);
  await expect(dashboard.locator('#global-cards')).toContainText(report.global.counts.eligible_oracle_cards.toLocaleString());
  expect(commanders.counts.selection_gate_passed).toBeGreaterThan(0);
  expect(commanders.counts.selection_gate_passed).toBeLessThan(commanders.counts.identified_single_candidates);
  await expect(dashboard.locator('#commander-coverage')).toContainText('not successful execution for every candidate');
  await dashboard.getByText('Inspect commander candidates and blockers',{exact:true}).click();
  await dashboard.getByLabel('Commander candidate name').fill('Daretti');
  await expect(dashboard.locator('#commander-coverage')).toContainText('native missing');
  await dashboard.getByText('Inspect commander candidates and blockers',{exact:true}).click();
  expect(objectives.objectives).toHaveLength(4);
  expect(new Set(objectives.objectives.map((o:{counts:Record<string,number>})=>o.counts.eligible_physical_cards_examined)).size).toBe(1);
  expect(objectives.population.length).toBe(6111);
  await expect(dashboard.locator('#objectives')).toContainText('Not measured at catalog scale');
  expect(pilot.counts.reviewed_ability_scopes).toBe(6);
  expect(pilot.counts.held_out_native_checks_supported).toBe(2);
  expect(pilot.executionState).toBe(pilot.recordedExecution.engine===pilot.engine?'RECORDED_CURRENT_BINDINGS':'STALE');
  expect(pilot.recordedExecution.scenarios.every((s:{state:string})=>s.state==='AGREES')).toBe(true);
  await dashboard.getByText('Inspect source reviewed cohort and held out results',{exact:true}).click();
  await dashboard.getByText('Reckless Barbarian · held-out variant',{exact:true}).click();
  await expect(dashboard.locator('#fidelity-pilot')).toContainText('arriving card characteristics');
  await dashboard.getByText('Reckless Barbarian · held-out variant',{exact:true}).click();
  await dashboard.getByText('Inspect source reviewed cohort and held out results',{exact:true}).click();
  await expect(dashboard.locator('#fidelity-pilot')).toContainText('does not independently qualify');
  await mkdir('../generated/global-coverage/screenshots',{recursive:true});
  await writeFile('../generated/global-coverage/browser-measurements.json',JSON.stringify({report,commanders:commanders.counts,objectives:objectives.objectives.map((o:unknown)=>o),pilot},null,2));
  for(const width of [1440,390]){
    await page.setViewportSize({width,height:width===390?844:1000});await page.evaluate(()=>window.scrollTo(0,0));
    expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBe(width);
    await page.screenshot({path:`../generated/global-coverage/screenshots/${width}-overview.png`});
    for(const id of ['global-cards','commander-coverage','objectives','fidelity-pilot'])await dashboard.locator('#'+id).screenshot({path:`../generated/global-coverage/screenshots/${width}-${id}.png`});
    await page.getByRole('button',{name:'Discover',exact:true}).click();
    await expect(page.getByRole('button',{name:'Find cards',exact:true})).toBeVisible();
    await page.getByRole('button',{name:'Progress',exact:true}).click();
    await expect(dashboard.locator('#fidelity-pilot')).toContainText('reviewed ability scopes',{timeout:120_000});
  }
});

test('Absent snapshot displays unmeasured global and commander coverage without manufactured zeros',async({page,request})=>{
  const response=await request.get('/api/v1/progress');expect(response.ok()).toBe(true);const report=await response.json();
  await page.route('**/api/v1/progress',route=>route.fulfill({contentType:'application/json',body:JSON.stringify({...report,global:{state:'NOT_MEASURED',reason:'No explicit snapshot selected.',counts:{},exclusions:{}}})}));
  await page.route('**/api/v1/progress/commanders',route=>route.fulfill({contentType:'application/json',body:JSON.stringify({state:'NOT_MEASURED'})}));
  await page.goto('/#progress');
  await expect(page.locator('#global-cards')).toContainText('No explicit snapshot selected');
  await expect(page.locator('#commander-coverage')).toContainText('not measured without a compatible explicit snapshot');
  await expect(page.locator('#global-cards progress')).toHaveCount(0);
  await expect(page.locator('#fidelity-pilot')).toContainText('reviewed ability scopes',{timeout:120_000});
});
