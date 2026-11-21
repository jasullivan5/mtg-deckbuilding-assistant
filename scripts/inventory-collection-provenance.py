"""Census exact native declarations and production-walker bindings; no name-based joins.

Native input is LegacyExileInventory.java JSONL (native-only, face-container aware).
Interpretation input is CollectionProvenanceInventory.java JSONL. Bulk rows stay generated.
"""
import argparse
from collections import Counter, defaultdict
import hashlib
import json
import re
from pathlib import Path
from runpy import run_path

nodes = run_path(str(Path(__file__).with_name('inventory-legacy-exile.py')))['nodes']
KINDS = {'GatherCards': 'producers', 'SelectFromCollection': 'selections',
         'FilterCollection': 'filters', 'MoveCollection': 'movements',
         'ConditionalOnCollection': 'conditions', 'PipelineTarget': 'indexedConsumers'}


def declaration(path):
    root = re.match(r'script\.(?:triggeredAbilities|activatedAbilities|stateTriggeredAbilities|sagaChapters)\[\d+\]', path)
    if root: return root.group()
    if path.startswith(('script.spellEffect', 'script.targetRequirements')): return 'script.spell'
    return path.split('.effect', 1)[0]


def census(native_path, evidence_path):
    cards, faces, declarations = set(), set(), defaultdict(list)
    populations = defaultdict(set)
    cohort = json.loads(Path('src/test/resources/regression/collection-movement-sources.json').read_text(encoding='utf-8'))
    frozen = {c['name'] for c in cohort['cases']}
    cohort_declarations = []
    for line in native_path.open(encoding='utf-8-sig'):
        if not line.startswith('{'): continue
        row = json.loads(line)
        card, face = row['physical'], row['face']
        cards.add(card); faces.add((card, face))
        populations['applicationExtensions' if (row.get('nativePath') or '').startswith('src/main/kotlin/extensions/') else 'engineDefinitions'].add(card)
        for path, node in nodes(row['script']):
            kind = node.get('type')
            categories = []
            if kind in KINDS: categories.append(KINDS[kind])
            if kind == 'TargetObject' and node.get('filter', {}).get('alternatives'):
                categories.append('unionTargets')
                def zones(f):
                    return {f.get('zone','BATTLEFIELD').upper()} | set().union(*(zones(a) for a in f.get('alternatives', [])))
                if len(zones(node['filter'])) > 1: categories.append('unionZoneTargets')
            if kind == 'MoveCollection' and node.get('storeMovedAs'): categories.append('movedResultProducers')
            if kind == 'FromVariable' and path.endswith('.source'): categories.append('variableConsumers')
            if kind == 'IterationSpace.Collection' and path.endswith('.space'): categories.append('iterationConsumers')
            for category in categories:
                declarations[category].append((card, face, path))
                if card in frozen:
                    cohort_declarations.append(dict(card=card, face=face, path=path, category=category, native=node))
    interpreted = defaultdict(list)
    moved_continuations = []
    unknown = Counter()
    for line in evidence_path.open(encoding='utf-8-sig'):
        if not line.startswith('{'): continue
        action = json.loads(line)
        stages = {s['id']:s for s in action.get('collectionStages', [])}
        for key in ('unionTargets', 'collectionStages', 'collectionUses'):
            for record in action.get(key, []):
                interpreted[key].append((action['card'], action['face'], action['id'], record))
                if record['support'] == 'UNKNOWN': unknown.update(set(record['unresolved']))
                if key == 'collectionUses' and record['support'] == 'MATCH' and stages.get(record['producerId'], {}).get('membership') == 'NATIVE_MOVED_RESULT':
                    moved_continuations.append((action['card'], action['face'], action['id'], record))
    def units(rows):
        return dict(physicalCards=len({r[0] for r in rows}), faceContainers=len({r[:2] for r in rows}), declarations=len(rows))
    cross_zone = [r for r in interpreted['unionTargets'] if len({c['zone'] for c in r[3]['clauses']}) > 1]
    return dict(engineRevision=cohort['engineRevision'],
                inputs={str(p): hashlib.sha256(p.read_bytes()).hexdigest() for p in (native_path, evidence_path)},
                units='Distinct physical roots and script face containers; declarations and action-local relationships are separate, overlapping units. No source fidelity inferred.',
                catalog=dict(physicalCards=len(cards), faceContainers=len(faces),populations={key:len(value) for key,value in populations.items()}),
                native={key: dict(units(rows),actionOrAbilityContainers=len({(r[0],r[1],declaration(r[2])) for r in rows})) for key, rows in sorted(declarations.items())},
                interpreted={key: dict(units(rows), actions=len({r[2] for r in rows}),
                    support=dict(Counter(r[3]['support'] for r in rows)),
                    kinds=dict(Counter(r[3].get('membership', r[3].get('consumer', 'UNION')) for r in rows)))
                    for key, rows in interpreted.items()},
                supportedMovedResultContinuations=dict(units(moved_continuations),actions=len({r[2] for r in moved_continuations}),
                    consumerKinds=dict(Counter(r[3]['consumer'] for r in moved_continuations))),
                interpretedUnionZones=dict(units(cross_zone),support=dict(Counter(r[3]['support'] for r in cross_zone))),
                unknownReasons=dict(unknown), cohortDeclarations=cohort_declarations)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('native', 'evidence', 'output'): parser.add_argument(name, type=Path)
    args = parser.parse_args()
    result = census(args.native, args.evidence)
    args.output.write_text(json.dumps(result, indent=2, ensure_ascii=False)+'\n', encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k not in ('cohortDeclarations','inputs')}, indent=2))
