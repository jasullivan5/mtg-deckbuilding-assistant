import {expect, test} from '@playwright/test';
import {mkdir} from 'node:fs/promises';

test.skip(!process.env.EXPLORER_URL, 'Requires the running application.');
for (const width of [1365, 390]) {
  test(`Discover shows neutral death-triggered operations at ${width}px`, async ({page}) => {
    await page.setViewportSize({width, height: 950});
    await page.goto('/');
    const complete = page.waitForResponse(r => r.url().endsWith('/api/v1/discover') && r.ok());
    await page.getByRole('button', {name: 'Find cards', exact: true}).click();
    const run = await (await complete).json();
    expect(run.objective).toBe('own-nontoken-creature-deaths');
    await page.getByRole('combobox', {name: 'Mechanic', exact: true}).selectOption('Responds to deaths');
    const search = page.getByLabel('Card name');
    const detail = page.getByRole('region', {name: 'Result inspection'});
    const response = detail.getByRole('region', {name: 'Death response'});
    await mkdir('../generated/explorer/triggered-operations', {recursive: true});

    await search.fill('Zulaport Cutthroat');
    await expect(detail.getByRole('heading', {name: 'Zulaport Cutthroat', exact: true})).toBeVisible();
    await expect(response).toContainText('each opponent loses 1 life');
    await expect(response).toContainText('you gain 1 life');
    await expect(response.getByText(/^Required event:/)).toHaveCount(1);
    await (width === 390 ? response : detail).screenshot({path: `../generated/explorer/triggered-operations/zulaport-${width}.png`});

    await search.fill('Pyretic Prankster');
    await expect(detail.getByRole('heading', {name: /Pyretic Prankster/})).toBeVisible();
    await expect(response).toContainText(/each opponent sacrifices/i);
    await expect(response).toContainText('This ability belongs to the back face.');
    await expect(response).toContainText(/any resulting creature death happens later/i);
    await (width === 390 ? response : detail).screenshot({path: `../generated/explorer/triggered-operations/prankster-${width}.png`});

    await search.fill('Midnight Reaper');
    await expect(response).toContainText('deals 1 damage to you');
    await expect(response).toContainText('draw 1');
    await search.fill('Deathcurse Ogre');
    await expect(response).toContainText('each player loses 3 life');
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  });
}
