import {expect, test} from '@playwright/test';
import {mkdir} from 'node:fs/promises';

test.skip(!process.env.EXPLORER_URL, 'Requires the running application.');
for (const width of [1365, 390]) {
  test(`Discover explains the historical state and exact branches at ${width}px`, async ({page}) => {
    await page.setViewportSize({width,height:950});
    await page.goto('/');
    await page.getByRole('combobox',{name:'Objective',exact:true}).selectOption('creature-died-this-turn');
    const complete = page.waitForResponse(r=>r.url().endsWith('/api/v1/discover') && r.ok());
    await page.getByRole('button',{name:'Find cards',exact:true}).click();
    const run = await (await complete).json();
    expect(run.objective).toBe('creature-died-this-turn');
    expect(run.cards.find((c:{card:string})=>c.card==='Tragic Slip').pathways.MATCH).toBe(1);
    await page.getByRole('combobox',{name:'Mechanic',exact:true}).selectOption('Depends on death history');
    await page.getByLabel('Card name').fill('Tragic Slip');
    const detail = page.getByRole('region',{name:'Result inspection'});
    const history = detail.getByRole('region',{name:'Death history'});
    await expect(history).toContainText('During resolution');
    await expect(history).toContainText('does not trigger from that death');
    await expect(history).toContainText('-13/-13');
    await expect(history).toContainText('-1/-1');
    expect(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await mkdir('../generated/explorer/state-dependency',{recursive:true});
    await (width===390 ? history : detail).screenshot({path:`../generated/explorer/state-dependency/tragic-slip-${width}.png`});
    expect(run.cards.find((c:{card:string})=>c.card==='Gravelighter').pathways.MATCH).toBe(1);
    await page.getByLabel('Card name').fill('Gravelighter');
    await expect(detail.getByRole('heading',{name:'Gravelighter',exact:true})).toBeVisible();
    await expect(history).toContainText('Incoming trigger:');
    await expect(history).toContainText('During resolution');
    await expect(history).toContainText(/when true: draw a card/i);
    await expect(history).toContainText(/otherwise: each player sacrifices a creature/i);
    expect(await page.evaluate(()=>document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await (width===390 ? history : detail).screenshot({path:`../generated/explorer/state-dependency/gravelighter-${width}.png`});
  });
}
