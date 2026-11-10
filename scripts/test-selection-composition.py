import copy
import importlib.util
import json
from pathlib import Path
import unittest

ROOT=Path(__file__).resolve().parent.parent
spec=importlib.util.spec_from_file_location('selection',ROOT/'scripts/selection-composition.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def card(identity='one',name='same name',kind='Creature',power='4'):
    return dict(oracle_id=identity,id='printing-'+identity,name=name,type_line=kind,power=power,
        color_identity=[],legalities={'commander':'legal'},layout='normal')

def row(name='same name',branch='DIRECT_DEATH_GENERATOR',verdict='MATCH'):
    return dict(card=name,identifier='SET:1:card',branch=branch,verdict=verdict,actionId='action',abilityPath='path',mechanicalCompatibility='MATCH',policyAcceptance='MATCH')

def inputs(records=None,rows=None):
    records=records or [card()]
    report=dict(objective='own-nontoken-creature-deaths',commander='test',argentumCommit='engine',allowedIdentity=['BLACK'],
        mechanicalInputCards=[x['name'] for x in records],actions=[],results=rows if rows is not None else [row()])
    identities=dict(context=dict(argentumCommit='engine',snapshotSha256='snapshot'),assignments=[dict(confidence='CONFIDENT',oracleId=x['oracle_id'],native={'name':x['name']}) for x in records])
    tags=dict(capturedAtUtc='test',queries={n:dict(status='COMPLETE',query=n,members=[],pages=[]) for n in ['boardwipe','removal','deathwipes']})
    return report,records,identities,tags

def run(values):return m.experiment(*values,'report-hash','snapshot')

class SelectionTest(unittest.TestCase):
    def test_union_keeps_two_paths_and_filter_reason_for_one_physical_card(self):
        left={};right={};c=card()
        m.add_member(left,c,dict(selection='focused',path='cost'))
        m.add_member(left,c,dict(selection='focused',path='return'))
        m.add_member(right,c,dict(selection='basic',type='Creature'))
        result=m.compose(left,right,'union',{},{});self.assertEqual(len(result),1)
        self.assertEqual(len(result['one']['memberships']['focused']),2)
        self.assertIn('basic',result['one']['memberships'])
    def test_intersection_does_not_promote_ordinary_material(self):
        left={};right={}
        m.add_member(left,card('a','a'),dict(selection='focused'))
        for identity in ['a','b']:m.add_member(right,card(identity,identity),dict(selection='basic'))
        self.assertEqual(set(m.compose(left,right,'restrict',{},{})),{'a'})
    def test_same_name_different_physical_id_not_collapsed(self):
        left={};right={}
        m.add_member(left,card('a'),dict(selection='focused'))
        m.add_member(right,card('b'),dict(selection='basic'))
        self.assertEqual(len(m.compose(left,right,'union',{},{})),2)
    def test_context_mismatch_rejected(self):
        with self.assertRaises(ValueError):m.compose({}, {},'union',{'population':'a'},{'population':'b'})
    def test_conflicting_identity_observations_rejected(self):
        left={};right={}
        m.add_member(left,card(name='one'),dict(selection='focused'))
        m.add_member(right,card(name='other'),dict(selection='basic'))
        with self.assertRaises(ValueError):m.compose(left,right,'union',{}, {})
    def test_external_only_basic_card_is_valid_without_native_claim(self):
        values=inputs(rows=[]);values[2]['assignments']=[]
        result=run(values)['selections']['basic-creatures'][0]
        self.assertEqual(result['nativeAssociations'],[])
        self.assertFalse(result['memberships']['basic-creatures'][0]['nativeMechanicalClaim'])
    def test_unknown_or_competing_identity_not_name_joined(self):
        values=inputs();values[2]['assignments'][0]['confidence']='UNKNOWN'
        result=run(values)
        self.assertEqual(result['counts']['projected-event']['cards'],0)
        self.assertEqual(len(result['diagnostics']['unmappedAcceptedNativeRows']),1)
    def test_variable_printed_power_is_not_guessed(self):
        result=run(inputs([card(power='*')]))
        self.assertEqual(result['counts']['basic-power4']['cards'],0)
        self.assertEqual(result['population']['variablePrintedPowerUnresolved'],['one'])
    def test_token_layout_not_ordinary_physical_creature_material(self):
        values=inputs();values[1][0]['layout']='token'
        result=run(values)
        self.assertEqual(result['counts']['basic-creatures']['cards'],0)
    def test_engine_and_snapshot_mismatches_rejected(self):
        values=inputs();values[2]['context']['argentumCommit']='other'
        with self.assertRaises(ValueError):run(values)
        values=inputs();values[2]['context']['snapshotSha256']='other'
        with self.assertRaises(ValueError):run(values)
    def test_failed_functional_query_is_not_an_empty_mechanical_negative(self):
        values=inputs();values[3]['queries']['deathwipes']['status']='FAILED'
        with self.assertRaises(ValueError):run(values)
    def test_history_and_direct_trigger_projection_are_distinct(self):
        r=row(branch='DEATH_ENHANCED_REMOVAL')
        self.assertEqual(m.project_event(r,{'native':{'spellEffect':{}}}),'WITHHOLD_HISTORY_OR_NONTRIGGER_DEPENDENCY')
        action={'native':{'trigger':dict(type='ZoneChangeEvent',**{'from':'Battlefield'},to='Graveyard')}}
        self.assertEqual(m.project_event(r,action),'RETAIN_LEGACY_DEATH_TRIGGER_DEPENDENCY')
        action['native']['trigger']['type']='AnyOf'
        self.assertEqual(m.project_event(r,action),'UNRESOLVED_LEGACY_TRIGGER_FORM')
    def test_missing_action_does_not_borrow_another_ability(self):
        self.assertEqual(m.project_event(row(branch='DEATH_ENHANCED_REMOVAL'),None),'UNRESOLVED_ACTION_ASSOCIATION')
    def test_report_is_not_mutated_and_unknown_stays_unknown(self):
        values=inputs(rows=[row(verdict='UNKNOWN'),row(verdict='NO_MATCH')]);before=copy.deepcopy(values[0])
        result=run(values)
        self.assertEqual(values[0],before)
        self.assertEqual(result['diagnostics']['originalUnknownPathways'],1)
        self.assertEqual(result['diagnostics']['originalNoMatchPathways'],1)
    def test_live_metadata_drift_is_exposed_without_overwriting_snapshot(self):
        values=inputs();values[3]['queries']['boardwipe']['members']=[dict(oracleId='one',layout='normal',typeLine='Creature',power='5',colorIdentity=[])]
        result=run(values)
        self.assertEqual(result['counts']['basic-boardwipe']['cards'],0)
        self.assertEqual(len(result['diagnostics']['functionalScope']['boardwipe']['metadataDrift']),1)
    def test_pinned_real_memberships_preserve_focused_basic_and_narrow_tag_reasons(self):
        sample=m.load(ROOT/'src/test/resources/regression/selection-composition-sample.json')
        result=m.experiment(sample['report'],sample['cards'],sample['identities'],sample['tags'],'sample',sample['sourceContext']['snapshotSha256'])
        names=lambda selection:{c['name'] for c in result['selections'][selection]}
        self.assertIn('Wrath of God',names('basic-destruction-wipes'))
        self.assertNotIn('Cyclonic Rift',names('basic-destruction-wipes'))
        self.assertIn('Cyclonic Rift',names('basic-boardwipe'))
        self.assertIn('Murder',names('basic-removal'))
        self.assertNotIn('Murder',names('projected-event'))
        self.assertNotIn('Wrath of God',names('projected-event'))
        self.assertNotIn('Tragic Slip',names('projected-event'))
        self.assertIn('Tragic Slip',names('current-focused'))
        self.assertIn('Attrition',names('projected-event'))
        self.assertIn('Walking Corpse',names('basic-creatures'))
        self.assertIn('Llanowar Elves',names('basic-creatures'))
        self.assertNotIn('Walking Corpse',names('restrict-creatures'))
        self.assertIn('Forest',names('basic-lands'))
        self.assertIn('Grave Pact',names('basic-removal'))
        wrath=next(c for c in result['selections']['add-destruction-wipes'] if c['name']=='Wrath of God')
        self.assertEqual(set(wrath['memberships']),{'basic-destruction-wipes'})

    def test_same_name_real_token_does_not_replace_original_control_identity(self):
        sample=m.load(ROOT/'src/test/resources/regression/selection-composition-sample.json')
        candidates=[c for c in sample['cards'] if c['name']=='Llanowar Elves']
        self.assertEqual(len(candidates),2)
        original=next(c for c in candidates if c['layout']=='normal')
        token=next(c for c in candidates if c['layout']=='token')
        self.assertEqual(sample['controlOracleIds']['Llanowar Elves'],original['oracle_id'])
        self.assertNotEqual(original['oracle_id'],token['oracle_id'])
        addendum=m.load(ROOT/'src/test/resources/regression/selection-source-identity-addendum.json')
        self.assertEqual(addendum['correctOriginalCardSource']['oracle_id'],original['oracle_id'])
        self.assertEqual(addendum['existingConfidentNativeAssignment']['oracleId'],original['oracle_id'])
        self.assertEqual(addendum['originalFrozenCaptureOracleId'],token['oracle_id'])

    def test_manual_expectation_overlay_cannot_add_grave_pact_to_focused_results(self):
        result=run(inputs());before=copy.deepcopy(result['selections'])
        overlay=dict(status='SOURCE_EXPECTATION_OVERLAY_NOT_EXTRACTED_ADMISSION',basis='source contract',profiles=[dict(card='Grave Pact',proposed='INCLUDE')])
        m.attach_source_overlay(result,overlay,dict(cards=[dict(card='Grave Pact',available=False)]))
        self.assertEqual(result['selections'],before)
        self.assertFalse(result['sourceExpectationOverlay']['profiles'][0]['currentNativeObservation']['available'])

if __name__=='__main__':unittest.main()
