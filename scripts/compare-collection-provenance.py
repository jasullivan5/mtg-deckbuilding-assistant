"""Require unchanged Discover evidence apart from three additive action-local records."""
import argparse
from collections import Counter
import json
from pathlib import Path
from runpy import run_path

shared = run_path(str(Path(__file__).with_name('compare-counter-placement.py')))
OBJECTIVES = (*shared['OBJECTIVES'], 'place-plus-one-counters')
ADDITIONS = {'unionTargets', 'collectionStages', 'collectionUses'}


def compare(before, after):
    label = after['commander'], after['objective']
    assert before.keys() == after.keys(), label
    assert all(before[k] == after[k] for k in before if k != 'actions'), label
    facts = {f['id']: f for f in after['facts']}
    totals = Counter()
    for old, new in zip(before['actions'], after['actions'], strict=True):
        assert old == {k:v for k,v in new.items() if k not in ADDITIONS}, (label,new['id'])
        stages = {s['id']:s for s in new.get('collectionStages', [])}
        for stage in stages.values():
            assert stage['actionId'] == new['id']
            assert stage['inputId'] is None or stage['inputId'] in stages
            assert stage['minimum'] == 0
        for use in new.get('collectionUses', []):
            assert use['actionId'] == new['id']
            assert use['producerId'] is None or use['producerId'] in stages
        for record in (*stages.values(), *new.get('collectionUses', []), *new.get('unionTargets', [])):
            for key in record.get('movementFactIds', []):
                assert key in facts and facts[key]['actionId'] == new['id'], (label, key)
        totals.update({key:len(new.get(key, [])) for key in ADDITIONS})
    return dict(objective=after['objective'],population=after['counts']['commander_eligible_cards'],
                populationSha256=shared['population_hash'](after),unchangedResults=shared['counts'](after['results']), additions=dict(totals))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for key in ('baseline','candidate','output'): parser.add_argument(key, type=Path)
    args = parser.parse_args()
    result = {}
    for commander in ('judith','krenko','kenrith'):
        result[commander] = []
        for objective in OBJECTIVES:
            name=f'{commander}-{objective}.json'
            result[commander].append(compare(shared['read'](args.baseline/name),shared['read'](args.candidate/name)))
            print(commander,objective,'unchanged existing evidence')
    args.output.write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
