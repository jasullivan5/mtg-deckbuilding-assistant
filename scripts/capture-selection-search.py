from pathlib import Path
from urllib.request import Request,urlopen
from urllib.parse import urlencode
from urllib.error import HTTPError
import json,time,hashlib,datetime,argparse
parser=argparse.ArgumentParser(description='Research-only public Scryfall capture; not production integration. Existing receipts are never overwritten.')
parser.add_argument('--output',type=Path,required=True)
out=parser.parse_args().output
if out.exists() and any(out.iterdir()):raise SystemExit('Choose an empty output directory to preserve existing receipts')
out.mkdir(parents=True,exist_ok=True)
queries={'lands':'legal:commander id<=wubrg t:land','power4':'legal:commander id<=wubrg t:creature pow>=4','boardwipe':'legal:commander id<=wubrg otag:boardwipe','removal':'legal:commander id<=wubrg otag:removal',
 'deathwipes':'legal:commander id<=wubrg otag:boardwipe otag:removal-creature otag:removal-destroy'}
manifest={'capturedAtUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'note':'Public API research capture, not production integration or mechanical verification. Complete paginated sets with unique=cards; community functional tags are not a rules interpreter.','queries':{}}
for slug,q in queries.items():
 url='https://api.scryfall.com/cards/search?'+urlencode({'q':q,'unique':'cards','order':'name'})
 members=[];pages=[];record={'query':q,'initialUrl':url,'pages':pages}
 try:
  while url:
   time.sleep(.15)
   with urlopen(Request(url,headers={'User-Agent':'MTG-Deckbuilding-Research/0.1','Accept':'application/json'}),timeout=45) as response:raw=response.read()
   page=json.loads(raw);path=out/f'{slug}-{len(pages)+1}.json';path.write_bytes(raw)
   pages.append({'url':url,'file':path.name,'sha256':hashlib.sha256(raw).hexdigest(),'warnings':page.get('warnings',[]),'totalCards':page.get('total_cards')})
   members.extend(page['data']);url=page.get('next_page') if page.get('has_more') else None
  record.update(status='COMPLETE',count=len(members),members=[{'oracleId':c.get('oracle_id'),'printingId':c['id'],'name':c['name'],'typeLine':c.get('type_line'),'power':c.get('power'),'layout':c.get('layout'),'colorIdentity':c.get('color_identity'),'commanderLegality':c.get('legalities',{}).get('commander')} for c in members])
 except Exception as ex:
  record.update(status='FAILED',error=str(ex),partialMembers=len(members))
  if isinstance(ex,HTTPError):
   body=ex.read();(out/f'{slug}-error.json').write_bytes(body);record['errorBody']=body.decode(errors='replace')[:1000]
 manifest['queries'][slug]=record
 print(slug,record['status'],record.get('count'),record.get('error'),flush=True)
 (out/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
