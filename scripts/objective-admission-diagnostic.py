#!/usr/bin/env python3
"""Read-only branch-withdrawal experiment, never production admission.

This evaluates only the specified counterfactual: withhold currently MATCHed
DEATH_ENHANCED_REMOVAL pathways from primary results. It does not identify
interaction, decide intent, promote mechanical compatibility, or settle B/C.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path


def digest(value):
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=False).encode()).hexdigest()


def compare(report):
    if report['objective'] != 'own-nontoken-creature-deaths':
        raise ValueError('This bounded counterfactual accepts only the existing death objective')
    original_digest = digest(report)
    rows = report['results']
    accepted = [(i, r) for i, r in enumerate(rows) if r['verdict'] == 'MATCH']
    withheld = [(i, r) for i, r in accepted if r['branch'] == 'DEATH_ENHANCED_REMOVAL']
    retained = [(i, r) for i, r in accepted if r['branch'] != 'DEATH_ENHANCED_REMOVAL']
    before_cards = {r['card'] for _, r in accepted}
    after_cards = {r['card'] for _, r in retained}
    # Index and row digest preserve duplicate pathway bases and every original
    # decision/prerequisite. Never collapse rows into a card/action-key dictionary.
    references = lambda entries: [dict(index=i, card=r['card'], actionId=r['actionId'],
        path=r['abilityPath'], branch=r['branch'], rowSha256=digest(r)) for i, r in entries]
    output = {
        'scope': 'Read-only exact enhanced-removal branch withdrawal; not an interaction classifier, complete candidate-policy evaluation or recommendation to change production',
        'commander': report['commander'], 'objective': report['objective'],
        'engine': report['argentumCommit'],
        'reportCanonicalSha256': original_digest,
        'eligibleInputCards': len(report['mechanicalInputCards']),
        'inputListSha256': digest(report['mechanicalInputCards']),
        'before': dict(cards=len(before_cards), pathways=len(accepted)),
        'scenario': dict(cards=len(after_cards), pathways=len(retained)),
        'withheld': dict(cards=len({r['card'] for _, r in withheld}), pathways=len(withheld)),
        'cardsLosingAllCurrentMatches': sorted(before_cards-after_cards),
        'cardsRetainingOtherMatches': sorted({r['card'] for _, r in withheld} & after_cards),
        'unchangedUnknownPathways': sum(r['verdict'] == 'UNKNOWN' for r in rows),
        'unchangedNoMatchPathways': sum(r['verdict'] == 'NO_MATCH' for r in rows),
        'unchangedVerdictCounts': dict(collections.Counter(r['verdict'] for r in rows)),
        'retainedMatchedRows': references(retained),
        'withheldRows': [dict(index=i, original=r, primaryAdmissionInScenario='WITHHELD_BY_PROPOSED_POLICY', rowSha256=digest(r)) for i, r in withheld],
        'preservedEvidenceSha256': {k: digest(report.get(k)) for k in ('eligibility','actions','facts','unsupported','structuralSupport','eventOutcomes','selfReturns')},
        'limitations': ['Only this branch-withdrawal scenario is fully evaluated. Candidate B/C treatment of same-operation multi-purpose producers, intended targets and conceptual objectives remains source-grounded manual research.',
            'Original mechanical verdicts, UNKNOWN and existing exclusions are unchanged. Withheld does not mean mechanically impossible.',
            'Cards and pathways differ; alternative accepted contributions can preserve card membership. No precision/recall estimate or whole-card certification.']
    }
    assert digest(report) == original_digest
    return output


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    raw = args.report.read_bytes()
    result = compare(json.loads(raw))
    result['reportFileSha256'] = hashlib.sha256(raw).hexdigest()
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
    assert args.report.read_bytes() == raw
    print(json.dumps({k: result[k] for k in ('commander','eligibleInputCards','before','scenario','withheld','cardsRetainingOtherMatches')}, ensure_ascii=False))
