import {expect, test} from '@playwright/test';
import {mkdir, writeFile} from 'node:fs/promises';

test.skip(!process.env.EXPLORER_URL, 'Requires the running application.');
test('Discover separates death production responses and history removal with ability evidence', async ({page}) => {
  await page.goto('/');
  await page.getByText('About objective', {exact:true}).click();
  await expect(page.locator('.objective-help')).toContainText('Benefits from deaths');
  const completed=page.waitForResponse(r=>r.url().endsWith('/api/v1/discover') && r.ok());
  await page.getByRole('button',{name:'Find cards',exact:true}).click();
  const run=await (await completed).json();
  const detail=page.getByRole('region',{name:'Result inspection'});
  const mechanic=page.getByRole('combobox',{name:'Mechanic',exact:true});
  await mkdir('../generated/explorer/event-outcomes',{recursive:true});
  await mechanic.selectOption('Benefits from deaths');
  await page.getByLabel('Card name').fill('Grim Haruspex');
  await expect(detail.getByRole('heading',{name:'Grim Haruspex',exact:true})).toBeVisible();
  await expect(detail.getByRole('region',{name:'Death response'})).toContainText('another');
  await expect(detail.getByRole('region',{name:'Death response'})).toContainText('draw 1');
  await expect(detail).toContainText('does not cause the initial death');
  await page.screenshot({path:'../generated/explorer/event-outcomes/discover-relationships.png',fullPage:true});
  for (const [name,outcome] of [['Pitiless Plunderer','Treasure'],['Doomed Dissenter','Zombie']]) {
    await page.getByLabel('Card name').fill(name);
    await expect(detail.getByRole('heading',{name,exact:true})).toBeVisible();
    await expect(detail.getByRole('region',{name:'Death response'})).toContainText(outcome);
    await detail.screenshot({path:`../generated/explorer/event-outcomes/${name.replaceAll(' ','-')}.png`});
  }
  await mechanic.selectOption('Produces deaths');
  await page.getByLabel('Card name').fill('Viscera Seer');
  await expect(detail.getByRole('heading',{name:'Viscera Seer',exact:true})).toBeVisible();
  await expect(detail.getByRole('region',{name:'Death response'})).toHaveCount(0);
  await mechanic.selectOption('Death-enhanced removal');
  await page.getByLabel('Card name').fill('Tragic Slip');
  await expect(detail.getByRole('heading',{name:'Tragic Slip',exact:true})).toBeVisible();
  await expect(detail.getByRole('region',{name:'Death response'})).toHaveCount(0);
  await mechanic.selectOption('Benefits from deaths');
  await page.getByLabel('Card name').fill('Grim Haruspex');
  await page.setViewportSize({width:390,height:844});
  await expect(detail.getByRole('region',{name:'Death response'})).toContainText('draw 1');
  expect(await page.evaluate(()=>document.documentElement.scrollWidth)).toBe(390);
  await detail.screenshot({path:'../generated/explorer/event-outcomes/haruspex-mobile.png'});
  await writeFile('../generated/explorer/event-outcomes/run.json',JSON.stringify(run,null,2));
});
