import copy
import importlib.util
from pathlib import Path
import unittest

spec=importlib.util.spec_from_file_location('admission',Path(__file__).with_name('objective-admission-diagnostic.py'))
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def row(card, branch, verdict='MATCH', mechanical='MATCH'):
    return dict(card=card,branch=branch,verdict=verdict,actionId='same-action',abilityPath='same-path',mechanicalCompatibility=mechanical,decisions=[{'basis':'MECHANICAL','verdict':mechanical}],prerequisites=['actual graveyard arrival'])

def report(rows):
    return dict(objective='own-nontoken-creature-deaths',commander='synthetic',argentumCommit='test',mechanicalInputCards=['a','b'],results=rows)

class DiagnosticTest(unittest.TestCase):
    def test_overlap_keeps_card_and_does_not_change_original_evidence(self):
        r=report([row('a','DEATH_ENHANCED_REMOVAL'),row('a','DIRECT_DEATH_GENERATOR'),row('b','DEATH_ENHANCED_REMOVAL')])
        before=copy.deepcopy(r);result=module.compare(r)
        self.assertEqual(r,before)
        self.assertEqual(result['cardsLosingAllCurrentMatches'],['b'])
        self.assertEqual(result['cardsRetainingOtherMatches'],['a'])
        self.assertEqual(result['withheldRows'][0]['original']['mechanicalCompatibility'],'MATCH')
    def test_unknown_and_policy_exclusion_are_not_promoted_or_reinterpreted(self):
        r=report([row('a','DEATH_ENHANCED_REMOVAL','UNKNOWN','UNKNOWN'),row('b','DIRECT_DEATH_GENERATOR','NO_MATCH')])
        result=module.compare(r)
        self.assertEqual(result['scenario'],dict(cards=0,pathways=0))
        self.assertEqual(result['unchangedUnknownPathways'],1)
        self.assertEqual(result['unchangedNoMatchPathways'],1)
        self.assertEqual(result['withheldRows'],[])
    def test_duplicate_pathway_identity_preserves_distinct_occurrences(self):
        r=report([row('a','DEATH_ENHANCED_REMOVAL'),row('a','DEATH_ENHANCED_REMOVAL')])
        result=module.compare(r)
        self.assertEqual(result['withheld'],dict(cards=1,pathways=2))
        self.assertEqual([x['index'] for x in result['withheldRows']],[0,1])
    def test_other_objective_rejected(self):
        r=report([]);r['objective']='create-creature-tokens'
        with self.assertRaises(ValueError): module.compare(r)

if __name__=='__main__': unittest.main()
