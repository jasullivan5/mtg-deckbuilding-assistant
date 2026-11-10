#!/usr/bin/env python3
"""Bounded offline research: existing death results + basic selections + union/restriction.
No production admission, Oracle rules parser, strategic score or persistent objective schema.
"""
import argparse
import copy
import gzip
import hashlib
import json
import re
from pathlib import Path

COLORS = dict(WHITE='W', BLUE='U', BLACK='B', RED='R', GREEN='G')
KEPT_BRANCHES = {'DIRECT_DEATH_GENERATOR', 'INTRINSIC_CREATURE_INPUT', 'DEATH_TRIGGERED_OUTCOME'}

def digest(value):
    return hashlib.sha256(json.dumps(value,sort_keys=True,ensure_ascii=False,separators=(',',':')).encode()).hexdigest()

def load(path):
    return json.loads(Path(path).read_text(encoding='utf-8-sig'))

def snapshot_cards(path):
    if str(path).endswith('.gz'):
        with gzip.open(path,'rt',encoding='utf-8') as f:
            return [json.loads(line) for line in f]
    return load(path)

def add_member(selection, card, evidence, native_names=()):
    identity=card['oracle_id']
    if identity in selection and selection[identity]['name']!=card['name']:
        raise ValueError('Conflicting physical identity/name observations; do not silently choose one')
    member=selection.setdefault(identity,dict(oracleId=identity,name=card['name'],layout=card['layout'],
        sourcePrintingId=card.get('id'),nativeAssociations=sorted(native_names),
        assurance='Membership evidence below is not whole-card mechanical verification',memberships={}))
    member['memberships'].setdefault(evidence['selection'],[]).append(evidence)

def compose(left,right,operation,left_context,right_context):
    if left_context!=right_context:
        raise ValueError('Composition requires exactly the same declared population/context')
    if operation not in ('union','restrict'):
        raise ValueError('Only union and restrict are supported')
    identities=set(left)|set(right) if operation=='union' else set(left)&set(right)
    result={}
    for identity in sorted(identities):
        parts=[s[identity] for s in (left,right) if identity in s]
        if len({p['name'] for p in parts})!=1:
            raise ValueError('Conflicting names for one identity')
        item=copy.deepcopy(parts[0])
        for part in parts[1:]:
            item['nativeAssociations']=sorted(set(item['nativeAssociations'])|set(part['nativeAssociations']))
            for selection,evidence in part['memberships'].items():
                # Exact evidence equality only; distinct ability/path references remain distinct.
                target=item['memberships'].setdefault(selection,[])
                for entry in evidence:
                    if entry not in target:target.append(copy.deepcopy(entry))
        result[identity]=item
    return result

def project_event(row, action):
    if row['verdict']!='MATCH':return 'NOT_CURRENT_MATCH'
    if row['branch'] in KEPT_BRANCHES:return 'RETAIN_CURRENT_RELATIONSHIP'
    if row['branch']!='DEATH_ENHANCED_REMOVAL':return 'UNRESOLVED_BRANCH'
    if action is None:return 'UNRESOLVED_ACTION_ASSOCIATION'
    # This is a projection of an ALREADY accepted legacy dependency. It does not
    # walk new effects or infer an event/filter match from Oracle text.
    trigger=(action or {}).get('native',{}).get('trigger')
    if trigger is None:return 'WITHHOLD_HISTORY_OR_NONTRIGGER_DEPENDENCY'
    if trigger.get('type')=='ZoneChangeEvent' and trigger.get('from')=='Battlefield' and trigger.get('to')=='Graveyard':
        if row.get('mechanicalCompatibility')=='MATCH':return 'RETAIN_LEGACY_DEATH_TRIGGER_DEPENDENCY'
    return 'UNRESOLVED_LEGACY_TRIGGER_FORM'

def attach_source_overlay(result, profiles, observations):
    if profiles.get('status')!='SOURCE_EXPECTATION_OVERLAY_NOT_EXTRACTED_ADMISSION':
        raise ValueError('Manual source profiles require an explicit nonmechanical label')
    original=digest(result['selections'])
    by_name={x['card']:x for x in observations['cards']}
    result['sourceExpectationOverlay']={'status':profiles['status'],'basis':profiles['basis'],
        'profiles':[dict(profile=p,currentNativeObservation=by_name.get(p['card']),
            membershipEffect='NONE: source expectations never add accepted relationships') for p in profiles['profiles']]}
    assert digest(result['selections'])==original

def experiment(report, records, identities, tags, report_hash, snapshot_hash):
    if report['objective']!='own-nontoken-creature-deaths':raise ValueError('Expected the unchanged death report')
    if identities['context']['argentumCommit']!=report['argentumCommit']:raise ValueError('Engine identity evidence mismatch')
    if identities['context']['snapshotSha256']!=snapshot_hash:raise ValueError('Identity/snapshot mismatch')
    before=digest(report)
    colors={COLORS[c] for c in report['allowedIdentity']}
    pool={};out_of_scope=[]
    for card in records:
        if card.get('legalities',{}).get('commander')!='legal':continue
        if card.get('color_identity') is None:
            out_of_scope.append(dict(card=card.get('name'),reason='UNKNOWN_EXTERNAL_IDENTITY'));continue
        if not set(card['color_identity'])<=colors:continue
        if card.get('layout')!='normal':
            out_of_scope.append(dict(card=card.get('name'),reason='NON_NORMAL_LAYOUT'));continue
        identity=card.get('oracle_id')
        if not identity:
            out_of_scope.append(dict(card=card.get('name'),reason='NO_ORACLE_ID'));continue
        if identity in pool:raise ValueError('Snapshot must contain one original physical observation per Oracle identity')
        pool[identity]=card
    by_name={};native_names={}
    for a in identities['assignments']:
        if a['confidence']!='CONFIDENT' or not a.get('oracleId'):continue
        by_name.setdefault(a['native']['name'],set()).add(a['oracleId'])
        native_names.setdefault(a['oracleId'],set()).add(a['native']['name'])
    context=dict(commander=report['commander'],allowedIdentity=sorted(colors),engine=report['argentumCommit'],
        snapshotSha256=snapshot_hash,externalEligibleNormalPoolSha256=digest(sorted(pool)),reportFileSha256=report_hash)
    selections={name:{} for name in ['current-focused','projected-event','basic-lands','basic-creatures','basic-power4','basic-boardwipe','basic-removal','basic-destruction-wipes']}
    def put(name,card,basis,**fields):
        add_member(selections[name],card,dict(selection=name,basis=basis,**fields),native_names.get(card['oracle_id'],()))
    variable_power=[]
    for card in pool.values():
        types=card.get('type_line','').split(' — ',1)[0].split()
        evidence=dict(sourcePrinting=card.get('id'),source='PINNED_EXTERNAL_STRUCTURED_METADATA',nativeMechanicalClaim=False)
        if 'Land' in types:put('basic-lands',card,'METADATA_TYPE',**evidence)
        if 'Creature' in types:
            put('basic-creatures',card,'METADATA_TYPE',**evidence)
            power=str(card.get('power',''))
            if re.fullmatch(r'-?\d+',power):
                if int(power)>=4:put('basic-power4',card,'NUMERIC_PRINTED_POWER',printedPower=power,**evidence)
            else:variable_power.append(card['oracle_id'])
    unmapped=[];projection=[];actions={a['id']:a for a in report['actions']}
    for index,row in enumerate(report['results']):
        if row['verdict']!='MATCH':continue
        mapping=by_name.get(row['card'],set())
        if len(mapping)!=1 or next(iter(mapping)) not in pool:
            unmapped.append(dict(index=index,card=row['card'],reason='UNRESOLVED_IDENTITY_OR_OUTSIDE_EXTERNAL_SCOPE',oracleCandidates=sorted(mapping)))
            continue
        identity=next(iter(mapping))
        if re.fullmatch(r'[0-9a-f-]{36}',row['identifier']) and row['identifier']!=identity:
            unmapped.append(dict(index=index,card=row['card'],reason='CONFLICTING_NATIVE_ORACLE_ID'));continue
        ref=dict(reportFileSha256=report_hash,rowIndex=index,rowSha256=digest(row),nativeCard=row['card'],
            actionId=row['actionId'],abilityPath=row['abilityPath'],branch=row['branch'],
            mechanicalCompatibility=row.get('mechanicalCompatibility'),policyAcceptance=row.get('policyAcceptance'),
            interpretation=row.get('interpretation'),localSupport=row.get('localSupport'),
            dependency=row.get('dependency'),prerequisites=row.get('prerequisites',[]),
            note='Existing structural MATCH, not successful gameplay or whole-card certification')
        put('current-focused',pool[identity],'UNCHANGED_PRODUCTION_RESULT',**ref)
        status=project_event(row,actions.get(row['actionId']))
        projection.append(dict(index=index,card=row['card'],oracleId=identity,status=status,branch=row['branch'],actionId=row['actionId'],path=row['abilityPath']))
        if status.startswith('RETAIN_'):put('projected-event',pool[identity],'RESEARCH_PROJECTION_OF_EXISTING_MATCH',projection=status,**ref)
    tag_scope={}
    for tag,selection in [('boardwipe','basic-boardwipe'),('removal','basic-removal'),('deathwipes','basic-destruction-wipes')]:
        query=tags['queries'][tag]
        if query['status']!='COMPLETE':raise ValueError('Functional search incomplete; no empty-set substitution')
        excluded=[];drift=[];seen=set()
        for member in query['members']:
            identity=member.get('oracleId')
            if identity in seen:raise ValueError('Functional capture is not unique by physical Oracle identity')
            seen.add(identity)
            if identity not in pool:excluded.append(identity);continue
            card=pool[identity]
            changed={k:dict(snapshot=card.get(k),live=member.get(field)) for k,field in [('color_identity','colorIdentity'),('layout','layout'),('type_line','typeLine'),('power','power')] if card.get(k)!=member.get(field)}
            if changed:
                drift.append(dict(oracleId=identity,changes=changed));continue
            put(selection,card,'PINNED_SOURCE_DERIVED_FUNCTIONAL_QUERY',query=query['query'],
                capturedAtUtc=query.get('capturedAtUtc',tags.get('capturedAtUtc')),nativeMechanicalClaim=False,
                note='Observed functional membership; not exact target, destruction, controller, cost or causal semantics')
        tag_scope[tag]=dict(query=query['query'],capturedMembers=len(query['members']),outsidePopulation=excluded,metadataDrift=drift,
            warnings=[w for page in query.get('pages',[]) for w in page.get('warnings',[])])
    selections['add-boardwipes']=compose(selections['projected-event'],selections['basic-boardwipe'],'union',context,context)
    selections['add-destruction-wipes']=compose(selections['projected-event'],selections['basic-destruction-wipes'],'union',context,context)
    selections['restrict-creatures']=compose(selections['projected-event'],selections['basic-creatures'],'restrict',context,context)
    assert digest(report)==before
    return dict(scope='RESEARCH_ONLY_NOT_PRODUCTION_POLICY',context=context,
        population=dict(externalEligibleNormalCards=len(pool),nativeEligibleInputCards=len(report['mechanicalInputCards']),
            omittedNormalLayoutOrIdentity=out_of_scope,variablePrintedPowerUnresolved=variable_power),
        counts={k:dict(cards=len(v),membershipEvidence=sum(len(es) for c in v.values() for es in c['memberships'].values())) for k,v in selections.items()},
        selections={k:[v[x] for x in sorted(v)] for k,v in selections.items()},
        diagnostics=dict(unmappedAcceptedNativeRows=unmapped,eventProjection=projection,functionalScope=tag_scope,
            originalUnknownPathways=sum(r['verdict']=='UNKNOWN' for r in report['results']),originalNoMatchPathways=sum(r['verdict']=='NO_MATCH' for r in report['results'])),
        limits=['Basic selection does not certify native representation or mechanics.',
            'The event projection reuses current specialized death policies and accepted legacy dependency evidence; it is not the revised universal admission system.',
            'No new event/state/modification query, ordinary-capability baseline classifier, multi-card relationship or deck value is inferred by composition.'])

if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__)
    for name in ['report','snapshot','identities','tags','output']:p.add_argument('--'+name,type=Path,required=True)
    p.add_argument('--profiles',type=Path)
    p.add_argument('--observations',type=Path)
    a=p.parse_args()
    result=experiment(load(a.report),snapshot_cards(a.snapshot),load(a.identities),load(a.tags),
        hashlib.sha256(a.report.read_bytes()).hexdigest(),hashlib.sha256(a.snapshot.read_bytes()).hexdigest())
    if bool(a.profiles)!=bool(a.observations):p.error('--profiles and --observations must be supplied together')
    if a.profiles:attach_source_overlay(result,load(a.profiles),load(a.observations))
    a.output.parent.mkdir(parents=True,exist_ok=True)
    a.output.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(dict(context=result['context'],counts=result['counts']),ensure_ascii=False))
