"""Read-only health, ordinary Kenrith API run and heap/provenance receipt."""
import json,pathlib,time,urllib.request
root=pathlib.Path(__file__).resolve().parent.parent/'mtg-native-consolidation'
out=root/'generated/jdk25-integration/receipts'
base='http://127.0.0.1:8876/api/v1/'
def get(path,body=None):
    payload=None if body is None else json.dumps(body).encode()
    request=urllib.request.Request(base+path,data=payload,headers={'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=180) as response:return json.load(response)
health=get('health')
assert health['status']=='ready',health
assert health['applicationRevision']=='529a78ea38b12b13598fd3c06f2e59ec9a4e9abe',health
assert health['argentumCommit']=='3969d0f6dca34fe79eb52d8ba703366ea4004121',health
(out/'explorer-health.json').write_text(json.dumps(health,indent=2)+'\n',encoding='utf8')
started=time.monotonic();run=get('discover',{'commander':'Kenrith, the Returned King','objective':'own-nontoken-creature-deaths'})
assert run['argentumCommit']==health['argentumCommit'],run.keys()
assert run['counts']['mechanical_input_cards_after_conventional_search']==20143,run['counts']
receipt={'health':health,'heap':'normal scripts/explorer.ps1 -Xmx1g -XX:ActiveProcessorCount=4','durationSeconds':round(time.monotonic()-started,3),'runId':run['runId'],'commander':run['commander'],'objective':run['objective'],'counts':run['counts'],'cardRows':len(run['cards']),'applicationRevision':run['applicationRevision'],'argentumCommit':run['argentumCommit']}
(out/'kenrith-runtime-1g.json').write_text(json.dumps(receipt,indent=2)+'\n',encoding='utf8')
print(json.dumps({'health':health,'durationSeconds':receipt['durationSeconds'],'counts':run['counts']}))
