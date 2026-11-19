import collections,hashlib,json,pathlib,subprocess,xml.etree.ElementTree as ET,zipfile
root=pathlib.Path(__file__).resolve().parent.parent
app=root/'mtg-native-consolidation';engine=root/'argentum-native-consolidation'
out=app/'generated/jdk25-integration/receipts';out.mkdir(exist_ok=True)
APP='529a78ea38b12b13598fd3c06f2e59ec9a4e9abe';ENGINE='3969d0f6dca34fe79eb52d8ba703366ea4004121'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def suite(name,path):
    files=sorted(path.glob('TEST-*.xml'));assert files,(name,path)
    roots=[ET.parse(p).getroot() for p in files]
    stamps=sorted(e.get('timestamp','') for e in roots)
    manifest='\n'.join(p.name+' '+sha(p) for p in files)
    assert stamps[0]>='2026-11-18T15:20:',(name,stamps[0])
    row={'module':name,'suites':len(files),**{k:sum(int(e.get(k,0)) for e in roots) for k in ['tests','failures','errors','skipped']},'firstSuiteTimestamp':stamps[0],'lastSuiteTimestamp':stamps[-1],'sortedXmlManifestSha256':hashlib.sha256(manifest.encode()).hexdigest(),'skippedCases':[{'class':e.get('name'),'test':t.get('name')} for e in roots for t in e.findall('testcase') if t.find('skipped') is not None]}
    assert row['failures']==row['errors']==0,row
    return row,files
paths=[('application',app/'build/test-results/test'),('sdk',engine/'mtg-sdk/build/test-results/test'),('rules',engine/'rules-engine/build/test-results/test')]
eras=['1993-1999','2000-2002','2003-2007','2008-2016','2017-2022','2023','2024','2025','2026']
paths += [(era,engine/f'mtg-sets/{era}/tests/build/test-results/test') for era in eras]
paths += [('native-catalog',engine/'mtg-sets/build/test-results/test')]
rows=[];controls=[]
sourceFiles={repo:subprocess.check_output(['git','-C',str(folder),'ls-files','*.kt'],text=True).splitlines() for repo,folder in [('application',app),('engine',engine)]}
with zipfile.ZipFile(out/'junit.zip','w',compression=zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for name,path in paths:
        row,files=suite(name,path);rows.append(row)
        for p in files:
            z.write(p,name+'/'+p.name)
            if any(x in p.name for x in ['MandatoryTapPaymentTest','CollectionCounterPlacementTest','SparkElementalScenarioTest','SourceInstanceSacrificeTest','SourceZoneIdentityTest','LinkedExileSourceInstanceTest','SunforgerScenarioTest','PlanewideCelebrationScenarioTest','TyriteSanctumScenarioTest','TranslationMutationTest','NativeDefinitionAdoptionTest','NativeIdentityProjectionTest','HistoricalDiscoverySnapshotTest','ReportDiagnosticsTest','ResearchProgressTest']):
                e=ET.parse(p).getroot();row={'module':name,'class':e.get('name'),'tests':int(e.get('tests',0)),'failures':int(e.get('failures',0)),'skipped':int(e.get('skipped',0)),'xmlSha256':sha(p),'cases':[t.get('name') for t in e.findall('testcase')]}
                repo='application' if name=='application' else 'engine';folder=app if repo=='application' else engine
                sourceName=e.get('name').rsplit('.',1)[-1]+'.kt';matches=[s for s in sourceFiles[repo] if s.endswith('/'+sourceName)]
                assert len(matches)==1,(sourceName,matches)
                source=matches[0];base='a150cdd' if repo=='application' else 'cf6d6bdf'
                currentBlob=subprocess.check_output(['git','-C',str(folder),'rev-parse','HEAD:'+source],text=True).strip()
                prior=subprocess.run(['git','-C',str(folder),'rev-parse',base+':'+source],capture_output=True,text=True)
                row.update(source=source,sourceSha256=sha(folder/source),sourceGitBlob=currentBlob,modifiedSinceMergedBase=prior.returncode!=0 or prior.stdout.strip()!=currentBlob)
                controls.append(row)
assert rows[0]['tests']==766,rows[0]
assert rows[-1]['tests']==493,rows[-1]
summary={'application':APP,'engine':ENGINE,'modules':rows,'controls':controls,'nativeTotalIncludingSdkAndCatalog':sum(r['tests'] for r in rows[1:]),'junitZipSha256':sha(out/'junit.zip'),'commands':'JDK25; exact candidate/origin/clean-source guards enabled. Engine394 full native rerun uses its wrapper9.8.2: SDK, rules, all nine eras and strict catalog. Final application529 full rerun uses wrapper9.6.1 via just application/dev plus installDist; normal Windows launcher uses the same distribution. Native engine sources did not change between native verification and final application execution. No copied-resource normalization hook.'}
(out/'test-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
logs=[{'path':p.name,'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(root.glob('jdk25-*.log'))]
(out/'log-manifest.json').write_text(json.dumps(logs,indent=2)+'\n',encoding='utf8')
resources=subprocess.check_output(['git','-C',str(app),'diff','--name-status','a150cdd',APP,'--','src/main/resources','src/test/resources'],text=True).splitlines()
assert all(x.startswith('A\t') for x in resources),resources
preservation={'baseApplication':'a150cdd66bf227f4c88a406b04620e56a7aed275','application':APP,'modifiedOrDeletedExistingSourceResources':[],'addedResources':[x.split('\t',1)[1] for x in resources],'qualification':'No source, golden, original correction/binding, old baseline or historical execution resource was changed. New versioned resources have separate provenance.'}
(out/'resource-preservation.json').write_text(json.dumps(preservation,indent=2)+'\n',encoding='utf8')
print(json.dumps({'modules':[{k:r[k] for k in ['module','tests','failures','errors','skipped']} for r in rows],'nativeTotal':summary['nativeTotalIncludingSdkAndCatalog'],'zipBytes':(out/'junit.zip').stat().st_size}))
