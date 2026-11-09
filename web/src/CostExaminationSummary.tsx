import {useState} from 'react';

type Step={label:string;validationMessage:string|null;outcome:string;rejection:string|null;message:string|null;before:unknown;action:unknown;nativeResult:unknown;after:unknown};
export type ExaminationData={applicationRevision:string;engine:string;reviewStatus:string;sourceState:string;nativeState:string;adjudicationState:string;
  phaseFiles:Record<string,string>;source:unknown;native:unknown;adjudication:unknown;executionState:string;counts:Record<string,number>;potentialTapReach:Record<string,number>;
  additionalChallenge:{defect:string;result:string;comparison:unknown};limits:string[];
  execution:{applicationRevision:string;executedAt:string;engine:string;scenarios:{id:string;card:string;scopeId:string;sourceExpectation:string;result:string;diagnostic:string;checks:Record<string,boolean>;steps:Step[];concern:string|null}[]}|null};
const human=(s:string)=>s.replaceAll('_',' ').toLowerCase();const n=(v:number)=>v.toLocaleString();
export default function CostExaminationSummary({data,population}:{data:ExaminationData;population:number}) {
  const [selected,setSelected]=useState('tap-boundary:mind-stone:0:TAPPED_EXPLICIT');
  const c=data.counts;const scenario=data.execution?.scenarios.find(s=>s.id===selected)??data.execution?.scenarios[0];
  return <section id="cost-examination" className="cost-summary" aria-labelledby="cost-examination-title">
    <h4 id="cost-examination-title">Examined payment boundaries</h4>
    <p>{c.examined_source_scopes} source scopes were frozen before native inspection; {c.examined_native_scopes} native scopes were frozen before prior verdicts and execution were exposed. A no-history examiner used the same provider and coordinator, with instruction-only isolation. <strong>Independent qualification remains pending.</strong></p>
    <p>Source: {human(data.sourceState)}. Native: {human(data.nativeState)}. Adjudication: {human(data.adjudicationState)}. This examination adds evidence depth, not newly reviewed physical cards or whole-card certification.</p>
    <p className="progress-callout"><strong>{data.executionState==='RECORDED_CURRENT_BINDINGS'?`${c.source_expectations_contradicted} current source-payment contradictions.`:'Current payment outcomes not established.'}</strong> Recorded execution: {human(data.executionState)} · {c.current_scenarios} current scenarios / {c.current_native_actions} native actions. {data.executionState==='RECORDED_CURRENT_BINDINGS'?'Already-tapped Mind Stone and Evolving Wilds completed under Explicit despite the required tap payment; AutoPay refused the corresponding attempts.':'A missing or stale receipt does not establish a zero failure count; retained observations keep their original provenance.'} The engine remains unchanged.</p>
    <p>Viscera Seer's prefilled noncreature or foreign-controller choice was refused during execution with rollback. A missing choice uses a supported native decision workflow; an invalid response is rejected before a valid response completes payment. These dispatch outcomes are separate from payment legality. Original incomplete observations remain recorded.</p>
    <details><summary>Inspect payment outcomes and examination phases</summary>
      {scenario&&<><label>Payment-boundary scenario<select value={scenario.id} onChange={e=>setSelected(e.target.value)}>{data.execution!.scenarios.map(s=><option key={s.id} value={s.id}>{s.card} · {s.id}</option>)}</select></label>
        <p><strong>{human(scenario.result)}</strong> · {human(scenario.diagnostic)}</p><p>{scenario.sourceExpectation}</p>{scenario.concern&&<p>{scenario.concern}</p>}
        <ul>{Object.entries(scenario.checks).map(([key,value])=><li key={key}>{human(key)}: {String(value)}</li>)}</ul>
        {scenario.steps.map((step,i)=><details key={i}><summary>{step.label} · {step.outcome}{step.rejection?` / ${step.rejection}`:''}</summary><p>Initial native validation: {step.validationMessage??'passed'}. {step.message}</p><pre>{JSON.stringify(step,null,2)}</pre></details>)}
        <p>Executed {data.execution!.executedAt} · application <code>{data.execution!.applicationRevision}</code> · engine <code>{data.execution!.engine}</code>. One scenario may contain multiple actions; rereading an older receipt is not new execution.</p></>}
      <p>Additional structural counterexample: {data.additionalChallenge.defect} · {data.additionalChallenge.result}. Original missing-mana, unsupported alternative and wrong-target/effect findings remain in the cost evidence above.</p>
      <details><summary>Source-first derivation, native assessment and adjudication</summary><p>Raw freeze bytes and UTF-8 LF transport hashes are separately preserved. Line-ending equivalence does not change requirements or mechanical verdicts.</p><pre>{JSON.stringify({source:data.source,native:data.native,adjudication:data.adjudication,phaseHashes:data.phaseFiles},null,2)}</pre></details>
      <p>Candidate investigation reach: {n(data.potentialTapReach.primary_cost_trees_with_source_tap)} projectable primary-definition source-tap cost trees on {n(data.potentialTapReach.distinct_native_physical_cards)} cards among {n(population)} confidently represented legal physical cards. These are not confirmed affected definitions. Back/flip faces, granted abilities, lowered keywords and unprojectable costs are excluded.</p>
      <ul>{data.limits.map(l=><li key={l}>{l}</li>)}</ul><a href="/api/v1/progress/cost-examination" target="_blank" rel="noreferrer">Complete phased examination and native boundary receipt</a>
    </details>
  </section>;
}
