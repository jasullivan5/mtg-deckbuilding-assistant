import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';
import {createHash} from 'node:crypto';
import {readFileSync, writeFileSync} from 'node:fs';
import {isDeepStrictEqual as equal} from 'node:util';

const revisions = {
  before: '45782ae4e537cdaab0a2e13de22308466409ecb1',
  after: '877f9ee90fea7120f85096de583db7d898e6fd56',
  engine: '03869c08665bf476a8b40dacbfbcd501c54a33bd',
};
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const read = path => JSON.parse(readFileSync(path, 'utf8'));
const cards = 'src/test/resources/cards';
const receipts = 'generated/explorer';
const diff = read(receipts + '/boundary-diff.json');
const commander = 'Judith, the Scourge Diva';
const resultKey = ({identifier, face, abilityPath, branch}) => JSON.stringify([identifier, face, abilityPath, branch]);
function paired(before, after, key) {
  const index = Map.groupBy(after, key);
  assert.equal(before.length, after.length);
  return before.map(record => {
    const candidates = index.get(key(record));
    assert(candidates?.length, key(record));
    const exact = candidates.findIndex(candidate => equal(candidate, record));
    // Duplicate identities are safe only when exact records disambiguate them.
    assert(exact >= 0 || candidates.length === 1, key(record));
    return {before: record, after: candidates.splice(Math.max(0, exact), 1)[0]};
  });
}
const changed = pairs => pairs.filter(pair => !equal(pair.before, pair.after));
const objectives = {};
let population, eligibility, reanimation, movements, changedResults;
for (const [label, objective] of Object.entries({
  reanimation: 'reanimate-own-creatures',
  deaths: 'own-nontoken-creature-deaths',
  tokens: 'create-creature-tokens',
})) {
  const hashes = {};
  const [before, after] = ['before', 'after'].map(side => {
    const bytes = readFileSync(receipts + '/boundary-' + side + '-' + label + '.json');
    hashes[side] = sha(bytes);
    assert.equal(hashes[side], diff[label][side + 'Sha256']);
    const report = JSON.parse(bytes);
    assert.equal(report.argentumCommit, revisions.engine);
    assert.equal(report.objective, objective);
    assert.equal(report.commander, commander);
    assert.equal(report.conventionalQuery, null);
    population ??= report.mechanicalInputCards;
    eligibility ??= report.eligibility;
    assert.deepEqual(report.mechanicalInputCards, population);
    assert.deepEqual(report.eligibility, eligibility);
    assert.deepEqual(report.allowedIdentity, ['BLACK', 'RED']);
    assert.equal(new Set(population).size, population.length);
    assert.equal(report.counts.mechanical_input_cards_after_conventional_search, population.length);
    return report;
  });
  const {facts: beforeFacts, results: beforeResults, counts: beforeCounts, ...beforeOther} = before;
  const {facts: afterFacts, results: afterResults, counts: afterCounts, ...afterOther} = after;
  assert.deepEqual(beforeOther, afterOther);
  const factChanges = changed(paired(beforeFacts, afterFacts, record => record.id));
  const resultPairs = paired(beforeResults, afterResults, resultKey);
  const resultChanges = changed(resultPairs);
  if (label === 'reanimation') {
    reanimation = {before, after, resultPairs};
    movements = factChanges;
    changedResults = resultChanges;
  } else {
    assert.deepEqual(factChanges, movements);
    assert.deepEqual(beforeCounts, afterCounts);
    assert.equal(resultChanges.length, 0);
  }
  objectives[label] = {objective, hashes, results: resultPairs.length, factChanges: factChanges.length,
    resultChanges: resultChanges.length, verdictChanges: resultChanges.filter(pair => pair.before.verdict !== pair.after.verdict).length};
}

const stable = read(cards + '/sacrifice-boundary-sources.json').sources.find(source => source.name === 'Reassembling Skeleton');
const manifest = read(cards + '/trigger-zone-sources.json').sources;
const bound = new Set();
const relationships = [...manifest, {...stable, abilityPath: 'script.activatedAbilities[0].effect', beforeVerdict: 'MATCH', afterVerdict: 'MATCH'}].map(source => {
  const {name, abilityPath} = source;
  const pair = reanimation.resultPairs.filter(({before}) => before.card === name && before.abilityPath === abilityPath);
  assert.equal(pair.length, 1, name);
  const outcome = pair[0];
  assert.equal(outcome.before.verdict, source.beforeVerdict, name);
  assert.equal(outcome.after.verdict, source.afterVerdict, name);
  const isStable = source.name === stable.name;
  assert.equal(!equal(outcome.before, outcome.after), !isStable, name);
  const factPairs = movements.filter(({after}) => after.path === abilityPath && after.actionId === outcome.after.actionId);
  assert.equal(factPairs.length, isStable ? 0 : 1, name);
  const fact = factPairs[0] ?? {
    before: reanimation.before.facts.find(record => record.path === abilityPath && record.actionId === outcome.before.actionId),
    after: reanimation.after.facts.find(record => record.path === abilityPath && record.actionId === outcome.after.actionId),
  };
  if (isStable) assert.deepEqual(fact.before, fact.after);
  bound.add(resultKey(outcome.after));
  const bytes = readFileSync(cards + '/' + source.file);
  assert.equal(sha(bytes), source.sha256, name);
  const snapshot = JSON.parse(bytes);
  assert.deepEqual([snapshot.name, snapshot.id, snapshot.oracle_id], [name, source.printingId, source.oracleId]);
  assert.equal(outcome.after.identifier, [snapshot.set.toUpperCase(), snapshot.collector_number, name].join(':'));
  assert.equal(outcome.after.face, 'front');
  const nativeSource = execFileSync('git', ['-C', '../argentum-engine', 'show', revisions.engine + ':' + source.nativeSource]);
  assert(nativeSource.toString().includes('"' + name + '"'), name);
  const action = reanimation.after.actions.find(record => record.id === outcome.after.actionId);
  assert(action);
  assert(population.includes(name));
  const eligibilityRecord = eligibility.find(record => record.card === name);
  assert.equal(eligibilityRecord.deck, 'MATCH');
  const assurance = reanimation.after.translationAssurance.filter(record => record.card === name || record.oracleId === source.oracleId);
  assert.equal(assurance.length, 0, name);
  return {
    source: {...source, nativeSha256: sha(nativeSource), oracleText: snapshot.oracle_text,
      cardUrl: snapshot.scryfall_uri, set: snapshot.set, collectorNumber: snapshot.collector_number,
      image: snapshot.image_uris?.normal ?? null, artist: snapshot.artist},
    outcome, fact, action, eligibility: eligibilityRecord, assurance,
    otherOutcomes: reanimation.resultPairs.filter(pair => pair.after.card === name && resultKey(pair.after) !== resultKey(outcome.after)),
    unsupported: reanimation.after.unsupported.filter(record => record.card === name),
  };
});
assert.equal(changedResults.length, manifest.length);
assert.equal(movements.length, manifest.length);
assert(changedResults.every(pair => bound.has(resultKey(pair.after))));
const totals = {};
for (const side of ['before', 'after']) {
  const report = reanimation[side];
  const accepted = report.results.filter(row => row.verdict === 'MATCH');
  const cards = new Set(accepted.map(row => row.card)).size;
  const pathways = accepted.length;
  const unresolved = report.results.filter(row => row.verdict === 'UNKNOWN').length;
  const rejected = report.results.filter(row => row.verdict === 'NO_MATCH').length;
  assert.deepEqual([cards, pathways, unresolved, rejected], [
    report.counts.accepted_reanimation_cards, report.counts.accepted_reanimation_pathways,
    report.counts.unknown_query_pathways, report.counts.rejected_movement_pathways,
  ]);
  totals[side] = {cards, pathways, unresolved, rejected};
}
const gains = relationships.filter(row => row.outcome.before.verdict === 'NO_MATCH' && row.outcome.after.verdict === 'MATCH');
assert.equal(gains.length, totals.after.pathways - totals.before.pathways);
assert.equal(new Set(gains.map(row => row.source.name)).size, totals.after.cards - totals.before.cards);
const path = 'web/src/integration-change-evidence.json';
const output = {revisions, commander, population: population.length, objectives, totals, relationships};
if (process.argv.includes('--check')) assert.deepEqual(read(path), output);
else writeFileSync(path, JSON.stringify(output) + '\n');
console.log('Validated six receipt hashes, three objective comparisons and ' + relationships.length + ' source-bound relationships.');

