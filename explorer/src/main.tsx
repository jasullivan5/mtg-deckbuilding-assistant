import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import './style.css';

type RecordData = Record<string, unknown>;
type Inspection = {requestedName:string;selectable:boolean;colorIdentity:string[];availability:string;blockers:{code:string;reason:string;nextAction:string}[];knownLimitations:string[];mechanicalCaveat:string;constructionScope:string};
type Objective = {id:string;label:string;explanation:string};
type Row = {card:string;face:string;actionId:string;abilityPath:string;branch:string;operation:string;verdict:string;interpretation:string;prerequisites:string[];decisions:{stage:string;verdict:string;reason:string}[];sourceReferences:string[];circumstances:RecordData|null};
type Report = {commander:string;objective:string;argentumCommit:string;allowedIdentity:string[];counts:Record<string,number>;results:Row[];limitations:string[];unsupported:(RecordData & {card:string;reason:string})[];translationAssurance:(RecordData & {card:string})[];eligibility:(RecordData & {card:string})[];actions:(RecordData & {card:string})[];facts:(RecordData & {card:string})[];structuralSupport:(RecordData & {card:string})[]};
type Run = {startedAt:string;durationMs:number;applicationRevision:string;report:Report};
async function api<T>(path:string, body?:unknown):Promise<T>{
 const response=await fetch(`/api/v1/${path}`,body===undefined?{}:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)});
 const data=await response.json();
 if(!response.ok)throw new Error(data.message??`Request failed (${response.status})`);
 return data;
}
const verdictLabel=(v:string)=>v==='MATCH'?'Conditional potential':v==='UNKNOWN'?'Unresolved':'Pathway does not match';
const branchLabel:Record<string,string>={DIRECT_DEATH_GENERATOR:'Direct death generation',DEATH_ENHANCED_REMOVAL:'Death-enhanced removal',REANIMATE_OWN_CREATURE:'Return our creature card',CREATE_CREATURE_TOKEN:'Create a creature token'};
function Json({value}:{value:unknown}){return <pre>{JSON.stringify(value,null,2)}</pre>}
export function App(){
 const [objectives,setObjectives]=useState<Objective[]>([]),[commander,setCommander]=useState('Judith, the Scourge Diva'),[objective,setObjective]=useState('own-nontoken-creature-deaths');
 const [inspection,setInspection]=useState<Inspection|null>(null),[run,setRun]=useState<Run|null>(null),[selected,setSelected]=useState<string|null>(null),[busy,setBusy]=useState(''),[error,setError]=useState('');
 useEffect(()=>{api<Objective[]>('objectives').then(setObjectives).catch(e=>setError(e.message))},[]);
 const changed=()=>{setInspection(null);setRun(null);setSelected(null);setError('')};
 async function research(discovery:boolean){
  setBusy(discovery?'Running discovery…':'Inspecting commander…');setError('');setRun(null);setSelected(null);setInspection(null);
  try{
   const inspected=await api<Inspection>('commander',{commander});setInspection(inspected);
   if(discovery&&inspected.selectable){const fresh=await api<Run>('discover',{commander,objective});setRun(fresh);setSelected(fresh.report.results[0]?.card??fresh.report.unsupported[0]?.card??null)}
  }catch(e){setError(e instanceof Error?e.message:String(e))}finally{setBusy('')}
 }
 const report=run?.report;
 const cards=report?[...new Set([...report.results.map(r=>r.card),...report.unsupported.map(r=>r.card)])].sort():[];
 const rows=report?.results.filter(r=>r.card===selected)??[];
 return <><header><a href="/">MTG / RESEARCH LAB</a><span>Explorer · M1</span></header><main>
 <div className="eyebrow">COMMANDER DISCOVERY</div><h1>Find a possibility.<br/><em>Understand its conditions.</em></h1>
 <p className="intro">Explore native card mechanics in your Commander’s colors. Every finding comes with its evidence and boundaries.</p>
 <section className="controls" aria-label="Research controls"><form onSubmit={e=>{e.preventDefault();void research(true)}}>
 <label>Commander’s exact name<input list="commanders" value={commander} onChange={e=>{changed();setCommander(e.target.value)}} disabled={!!busy} required maxLength={200}/></label>
 <datalist id="commanders"><option>Judith, the Scourge Diva</option><option>Oviya Pashiri, Sage Lifecrafter</option><option>Krenko, Mob Boss</option></datalist>
 <label>Research objective<select value={objective} onChange={e=>{changed();setObjective(e.target.value)}} disabled={!!busy||!objectives.length}>{objectives.map(o=><option value={o.id} key={o.id}>{o.label}</option>)}</select></label>
 <p className="hint">{objectives.find(o=>o.id===objective)?.explanation}</p>
 <div className="actions"><button type="button" onClick={()=>void research(false)} disabled={!!busy||!commander.trim()}>Inspect commander</button><button className="primary" disabled={!!busy||!objectives.length||!commander.trim()}>Run discovery →</button></div>
 </form><aside aria-live="polite"><h2>Your research question</h2>{busy?<p role="status">{busy} The first query also loads source references. Please wait.</p>:!inspection?<p>Choose a commander and an objective, then run a fresh query. Exact spelling and case matter.</p>:<><p><b>{inspection.requestedName}</b></p><p>{inspection.selectable?'Available for single-commander discovery':'Selection blocked'} · {inspection.colorIdentity.join(' / ')||'No resolved colors'}</p><p>{inspection.availability.replaceAll('_',' ')}</p>{inspection.blockers.map((b,i)=><p className="warning" key={i}><b>{b.code.replaceAll('_',' ')}</b><br/>{b.reason}<br/>{b.nextAction}</p>)}{inspection.knownLimitations.map((l,i)=><p key={i}>{l}</p>)}<details><summary>Commander eligibility and review</summary><p>{inspection.constructionScope}</p><p>{inspection.mechanicalCaveat}</p><Json value={inspection}/></details></>}{error&&<p role="alert" className="warning">{error}</p>}</aside></section>
 <p className="notice">A structural match is a conditional possibility. It does not prove successful resolution, certify the card’s translation, or recommend a deck inclusion. No emitted pathway is not a proven nonmatch.</p>
 {report&&run&&<><section className="run-summary" aria-label="Completed discovery"><div className="eyebrow">FRESH KOTLIN DISCOVERY · COMPLETED</div><h2>{report.commander}</h2><p>{objectives.find(o=>o.id===report.objective)?.label} · {run.durationMs} ms · {new Date(run.startedAt).toLocaleString()}</p><p>{report.results.filter(r=>r.verdict==='MATCH').length} conditional-match pathways · {report.results.filter(r=>r.verdict==='UNKNOWN').length} unresolved pathways · {report.results.filter(r=>r.verdict==='NO_MATCH').length} nonmatching pathways · {report.unsupported.length} unsupported structure records</p><p>Search scope: {report.counts.mechanical_input_cards_after_conventional_search} eligible physical cards from the pinned authored catalog, with application extensions. This is not the entire Commander card pool. The list has {cards.length} cards with emitted pathways or unsupported records; categories can overlap.</p></section>
 <div className="workspace"><section className="results" aria-label="Discovery results"><h2>Explore findings</h2>{!cards.length&&<p>No pathways or unsupported records emitted. This does not establish that these mechanics are absent.</p>}{cards.map(card=>{const rs=report.results.filter(r=>r.card===card);return <button key={card} className={`card ${selected===card?'selected':''}`} onClick={()=>setSelected(card)} aria-pressed={selected===card}><strong>{card}</strong><span>{rs.length?`${rs.length} pathway${rs.length===1?'':'s'} · ${[...new Set(rs.map(r=>verdictLabel(r.verdict)))].join(' / ')}`:'Unsupported structures; no emitted objective pathway'}</span><small>{[...new Set(rs.map(r=>branchLabel[r.branch]??r.branch))].join(' · ')}</small></button>})}</section>
 <section className="detail" aria-label="Result inspection" tabIndex={0}><div className="eyebrow">INSPECT THE EVIDENCE</div><h2>{selected??'Select a finding'}</h2>{selected&&<><p className="hint">Imagery is deferred: no verified printing and face association has been loaded.</p>{!rows.length&&<p>No objective pathway emitted for this card. Inspect its unsupported structures below.</p>}{rows.map((r,i)=><article key={i} className="pathway"><span className={`badge ${r.verdict}`}>{verdictLabel(r.verdict)}</span><h3>{branchLabel[r.branch]??r.branch}</h3><p>Native action: <b>{r.operation}</b> · {r.face}</p><p>{r.interpretation.replaceAll('_',' ').toLowerCase()}</p><h4>Why this pathway appeared</h4>{r.decisions.map((d,j)=><p key={j}><b>{verdictLabel(d.verdict)}</b> — {d.reason}</p>)}<h4>What must be true</h4><ul>{r.prerequisites.map((p,j)=><li key={j}>{p}</li>)}</ul>{r.circumstances&&<details><summary>Timing, resources and circumstances</summary><Json value={r.circumstances}/></details>}<details><summary>Exact pathway and source evidence</summary><p>Path: <code>{r.abilityPath}</code></p>{r.sourceReferences.map((source,j)=>source.startsWith('https://github.com/')?<p key={j}><a href={source} target="_blank" rel="noreferrer">{source}</a></p>:<p key={j}>{source}</p>)}<Json value={r}/></details></article>)}
 <h3>Unsupported structures and limitations</h3>{report.unsupported.filter(u=>u.card===selected).map((u,i)=><p className="warning" key={i}>{u.reason}</p>)}{!report.unsupported.some(u=>u.card===selected)&&<p>No unsupported record emitted for this card; this is not whole-card certification.</p>}
 <h3>Translation assurance</h3>{report.translationAssurance.some(a=>a.card===selected)?<Json value={report.translationAssurance.filter(a=>a.card===selected)}/>:<p>No application translation-assurance record supplied. Inherited native definitions are not certified by discovery.</p>}
 <details><summary>Related native actions, facts, eligibility and support</summary><Json value={{actions:report.actions.filter(a=>a.card===selected),facts:report.facts.filter(a=>a.card===selected),eligibility:report.eligibility.filter(a=>a.card===selected),support:report.structuralSupport.filter(a=>a.card===selected),unsupported:report.unsupported.filter(a=>a.card===selected)}}/></details></>}</section></div>
 <details className="provenance"><summary>Run scope, revisions, exclusions and limitations</summary><p>Application: <code>{run.applicationRevision}</code><br/>Engine: <code>{report.argentumCommit}</code><br/>Objective: <code>{report.objective}</code><br/>Allowed identity: {report.allowedIdentity.join(', ')}</p><ul>{report.limitations.map((l,i)=><li key={i}>{l}</li>)}</ul><Json value={report.counts}/><details><summary>All eligibility records, including exclusions and unknowns</summary><Json value={report.eligibility}/></details><a download="live-discovery.json" href={URL.createObjectURL(new Blob([JSON.stringify(run)],{type:'application/json'}))}>Download full live evidence</a></details></>}
 <footer>Local mechanical research · Magic: The Gathering © Wizards of the Coast · Unofficial project</footer>
 </main></>;
}
createRoot(document.getElementById('root')!).render(<App/>);
