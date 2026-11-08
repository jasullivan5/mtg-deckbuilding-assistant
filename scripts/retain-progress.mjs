// Explicit retention of the existing controlled historical reports; never runs during a build.
// Usage: node scripts/retain-progress.mjs <initial.json> <discard.json> <sacrifice.json>
import {readFileSync, writeFileSync} from 'node:fs';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
const paths = process.argv.slice(2);
assert.equal(paths.length, 3);
const expected = ['e40d2babed6c89ea5e9093e80d34e92334e76302c62c855870378f4657c1e9c0', 'e04c6412ab122314245d7b4b4ffe89b7cf4c821444e337b143bdb79c1aedcdce', '5b2dd46830f863a59ce11266c0035be8e4e83f9919a0ffe4f571d83d4074f301'];
const digest = b => createHash('sha256').update(b).digest('hex');
const reports = paths.map((path, i) => {const bytes = readFileSync(path); assert.equal(digest(bytes), expected[i]); return JSON.parse(bytes);});
const engine = '03869c08665bf476a8b40dacbfbcd501c54a33bd';
const population = reports[0].mechanicalInputCards;
assert.equal(population.length, 6111);
for (const r of reports) {
  assert.equal(r.argentumCommit, engine);
  assert.equal(r.commander, 'Judith, the Scourge Diva');
  assert.equal(r.objective, 'stock-own-graveyard');
  assert.equal(r.conventionalQuery, null);
  assert.deepEqual(r.mechanicalInputCards, population);
  assert.deepEqual(r.eligibility, reports[0].eligibility);
}
const stages = reports.map((r, i) => ({
  label: ['Initial state discovery', 'Source-card discard correction', 'Source-bound sacrifice correction'][i],
  applicationRevision: ['b9c970df8a1e99ec32c505121d19019c54e5e98d', '8dc1f01df9f62da14f2413a1d89807ca841bcc8f', 'fa9ba4030356f2eef214d92f4c41b903c196fc7e'][i],
  sourceSha256: expected[i], counts: r.counts,
  matches: r.results.filter(x => x.verdict === 'MATCH').length,
  matchingCards: new Set(r.results.filter(x => x.verdict === 'MATCH').map(x => x.card)).size,
  unknowns: r.results.filter(x => x.verdict === 'UNKNOWN').length,
  nonmatches: r.results.filter(x => x.verdict === 'NO_MATCH').length,
  unsupported: r.unsupported.length,
}));
const key = r => r.stateEntry.factId;
const comparisons = reports.slice(1).map((after, i) => {
  const before = reports[i]; const old = new Map(before.results.map(r => [key(r), r]));
  const now = new Map(after.results.map(r => [key(r), r]));
  assert.equal(old.size, before.results.length); assert.equal(now.size, after.results.length);
  const changes = after.results.filter(r => old.get(key(r))?.verdict !== r.verdict).map(r => ({
    card: r.card, face: r.face, path: r.abilityPath, factId: key(r),
    before: old.get(key(r))?.verdict ?? 'NO_PRIOR_PATHWAY', after: r.verdict,
    prerequisites: r.prerequisites, decisions: r.decisions, sourceReferences: r.sourceReferences,
  }));
  const oldMatchCards = new Set(before.results.filter(r => r.verdict === 'MATCH').map(r => r.card));
  return {beforeStage: i, afterStage: i + 1, comparable: true,
    baselineApplicationRevision: i === 0 ? stages[0].applicationRevision : '6d46214715713d0a34c296e4419682bc47dfd2fa',
    populationChanged: false, objectiveChanged: false,
    newMatchingPathways: changes.filter(r => r.after === 'MATCH').length,
    newMatchingCards: [...new Set(changes.filter(r => r.after === 'MATCH' && !oldMatchCards.has(r.card)).map(r => r.card))].sort(),
    resolvedNonmatches: changes.filter(r => r.before === 'UNKNOWN' && r.after === 'NO_MATCH').length,
    lostMatches: changes.filter(r => r.before === 'MATCH' && r.after !== 'MATCH'),
    newUnknowns: changes.filter(r => r.after === 'UNKNOWN').length,
    removedPathways: before.results.filter(r => !now.has(key(r))).map(r => ({card: r.card, factId: key(r), verdict: r.verdict})), changes};
});
assert.deepEqual(stages.map(s => [s.matches, s.matchingCards]), [[433,412],[442,420],[535,508]]);
assert.deepEqual(comparisons.map(c => [c.newMatchingPathways,c.newMatchingCards.length,c.resolvedNonmatches,c.lostMatches.length,c.removedPathways.length]), [[9,8,0,0,0],[93,88,223,0,0]]);
writeFileSync(new URL('../src/main/resources/research-history.json', import.meta.url), JSON.stringify({
  schemaVersion: 1, observation: 'HISTORICAL_RECORDED_NOT_CURRENT_EXECUTION', engine,
  commander: reports[0].commander, objective: reports[0].objective, conventionalQuery: null,
  population, exclusions: Object.fromEntries(Object.entries(reports[0].counts).filter(([k]) => /eligib|legality|identity|physical|registered/.test(k))),
  comparisonMethod: 'Exact engine, objective, filter, ordered mechanical input and complete eligibility equality; factId-keyed verdict changes. Identical intermediate report SHA256 connects the two controlled comparisons. Historical documents identify the application commits; the sacrifice baseline is bootstrap-only and reproduces the intermediate report bytes.',
  sources: ['docs/SOURCE_DISCARD_DISCOVERY.md','docs/SOURCE_SACRIFICE_DISCOVERY.md'], stages, comparisons,
}, null, 2) + '\n');
