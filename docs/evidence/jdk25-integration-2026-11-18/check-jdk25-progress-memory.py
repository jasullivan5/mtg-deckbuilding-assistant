"""Exercise cold full-source Progress followed by ordinary Discover at 1 GiB."""
import hashlib,json,pathlib,time,urllib.request
root=pathlib.Path(__file__).resolve().parent.parent/'mtg-native-consolidation'
out=root/'generated/jdk25-integration/receipts'
base='http://127.0.0.1:8876/api/v1/'
expected='529a78ea38b12b13598fd3c06f2e59ec9a4e9abe'
def call(path,body=None):
    global response_sha
    req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body).encode(),headers={'Content-Type':'application/json'})
    start=time.monotonic()
    with urllib.request.urlopen(req,timeout=180) as response: raw=response.read()
    response_sha=hashlib.sha256(raw).hexdigest()
    value=json.loads(raw)
    return value,round(time.monotonic()-start,3)
health,_=call('health')
assert health['applicationRevision']==expected,health
steps=[]
for path in ['progress','progress/commanders','progress/objectives','progress/pilot','progress/verification','progress/activation-costs','progress/cost-examination','progress/global-baseline','progress/mechanics']:
    value,duration=call(path)
    row={'path':path,'durationSeconds':duration}
    if path=='progress':
        assert value['global']['state']=='MEASURED',value['global']
        assert value['global']['counts']['eligible_oracle_cards']==32109,value['global']['counts']
        row['global']=value['global']
        row['native']=value.get('native')
    elif path=='progress/mechanics':
        assert value['encounteredTypedNodes']==256022 and value['inputCards']==21699
        assert response_sha=='3ce4a387333dae27e3ba8202ce1e40b86415c2c5422656e14e28b1094890e248'
        row.update(sha256=response_sha,typedNodes=value['encounteredTypedNodes'],groups=len(value['groups']))
    elif path=='progress/global-baseline':
        row['records']=len(value['records'])
        assert sum(r['poolState']=='ELIGIBLE' for r in value['records'])==32109
    steps.append(row)
    print(json.dumps(row),flush=True)
    del value
for commander,objective,population in [('Kenrith, the Returned King','place-plus-one-counters',20143),('Kenrith, the Returned King','own-nontoken-creature-deaths',20143)]:
    value,duration=call('discover',{'commander':commander,'objective':objective})
    assert value['applicationRevision']==expected,value['applicationRevision']
    assert value['counts']['mechanical_input_cards_after_conventional_search']==population,value['counts']
    row={'path':'discover','commander':commander,'objective':objective,'durationSeconds':duration,'counts':value['counts'],'rows':len(value['cards'])}
    steps.append(row)
    print(json.dumps(row),flush=True)
    del value
value,duration=call('progress/mechanics')
assert response_sha=='3ce4a387333dae27e3ba8202ce1e40b86415c2c5422656e14e28b1094890e248'
steps.append({'path':'progress/mechanics-after-discover','durationSeconds':duration,'sha256':response_sha,'typedNodes':value['encounteredTypedNodes'],'groups':len(value['groups'])})
del value
value,duration=call('progress')
assert value['global']['state']=='MEASURED'
steps.append({'path':'progress-after-discover','durationSeconds':duration,'global':value['global']})
receipt={'applicationRevision':expected,'engine':health['argentumCommit'],'heap':'normal Explorer -Xmx1g -XX:ActiveProcessorCount=4','health':health,'steps':steps}
(out/'progress-discover-memory.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
print('PASS: full-source Progress, Discover counter placement, Kenrith deaths, and Progress revisit at 1 GiB',flush=True)
