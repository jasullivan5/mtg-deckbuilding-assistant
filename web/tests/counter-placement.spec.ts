import {expect, test} from '@playwright/test';
import {mkdir} from 'node:fs/promises';

test.skip(!process.env.EXPLORER_URL, 'Requires the running application.');

for (const width of [1365, 390]) {
  test(`Discover distinguishes placement and modification at ${width}px`, async ({page}) => {
    await page.setViewportSize({width, height: 950});
    await page.goto('/');
    await page.getByRole('combobox', {name: 'Commander', exact: true}).fill('Kenrith, the Returned King');
    const objective = page.getByRole('combobox', {name: 'Objective', exact: true});
    await expect(objective.locator('option[value="place-plus-one-counters"]')).toHaveCount(1);
    await objective.selectOption('place-plus-one-counters');
    const complete = page.waitForResponse(response => response.url().endsWith('/api/v1/discover') && response.ok());
    await page.getByRole('button', {name: 'Find cards', exact: true}).click();
    const run = await (await complete).json();
    expect(run.objective).toBe('place-plus-one-counters');
    expect(run.commander).toBe('Kenrith, the Returned King');

    const mechanic = page.getByRole('combobox', {name: 'Mechanic', exact: true});
    const search = page.getByLabel('Card name');
    const detail = page.getByRole('region', {name: 'Result inspection'});
    const placement = detail.getByRole('region', {name: 'Counter placement'});
    const output = '../generated/explorer/counter-placement';
    await mkdir(output, {recursive: true});

    await mechanic.selectOption('Places +1/+1 counters');
    await search.fill('Travel Preparations');
    await expect(detail.getByRole('heading', {name: 'Travel Preparations', exact: true})).toBeVisible();
    await expect(placement).toContainText('Places +1/+1 counters');
    await expect(placement).toContainText(/target/i);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await (width === 390 ? placement : detail).screenshot({path: `${output}/travel-preparations-${width}.png`});

    await search.clear();
    await mechanic.selectOption('Modifies +1/+1 counter placement');
    await search.fill('Hardened Scales');
    await expect(detail.getByRole('heading', {name: 'Hardened Scales', exact: true})).toBeVisible();
    await expect(placement).toContainText('Modifies +1/+1 counter placement');
    await expect(placement).toContainText(/another operation must initiate the placement/i);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
    await (width === 390 ? placement : detail).screenshot({path: `${output}/hardened-scales-${width}.png`});
  });
}
