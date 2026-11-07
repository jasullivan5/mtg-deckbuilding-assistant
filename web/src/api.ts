export type RecordData = Record<string, unknown>;
export type Inspection = {
  requestedName: string; selectable: boolean; colorIdentity: string[]; availability: string;
  blockers: {code: string; reason: string; nextAction: string}[]; knownLimitations: string[];
  mechanicalCaveat: string; constructionScope: string; argentumCommit: string;
};
export type Objective = {id: string; label: string; explanation: string; capabilities: string[]; coverageNote?: string | null};
export type Finding = {
  index: number; card: string; pathways: Record<string, number>; branches: string[];
  unsupportedRecords: number; partialPathways: number; reason: string | null; mechanics: string[];
  browsingState: string; recordedConcern: boolean;
  conditionalGraveyardContribution?: boolean;
};
export type Run = {
  schemaVersion: number; execution: string; runId: string; startedAt: string; durationMs: number;
  applicationRevision: string; argentumCommit: string; engineMode: string; commander: string;
  objective: string; allowedIdentity: string[]; counts: Record<string, number>;
  pathwayCounts: Record<string, number>; limitations: string[]; cards: Finding[];
};
export type Row = {
  card: string; face: string; actionId: string; abilityPath: string; branch: string;
  operation: string; verdict: string; mechanicalCompatibility?: string | null; policyAcceptance?: string | null; interpretation: string; prerequisites: string[];
  decisions: {stage: string; verdict: string; reason: string}[];
  sourceReferences: string[]; circumstances: RecordData | null;
  stateEntry?: {factId: string} | null;
};
export type Explanation = {summary: string; requirements: string[]; unknowns: string[]; mechanic: string; outcome: string};
export type ArrivalInspection = {
  relationship: {actionId: string; path: string; factId: string | null; decision: {verdict: string; reason: string}};
  stateCriterion: string; mechanism: string | null; prerequisites: string[]; sources: string[];
};
export type Evidence = {
  labScenarioIds?: string[];
  runId: string; card: string; authoredCard: {manaCost: string; typeLine: string; text: string} | null;
  results: Row[]; actions: RecordData[]; facts: RecordData[]; unsupported: {reason: string; actionId: string; path: string; nativeType: string}[];
  structuralSupport: RecordData[]; eligibility: RecordData[]; translationAssurance: RecordData[];
  explanations: Explanation[]; abilities: AbilityInspection[]; evidenceSummary: EvidenceIndicator[]; concerns: ResearchConcern[];
  rules: RulesLine[];
  graveyardContributions?: ArrivalInspection[];
};
export type EvidenceState = 'ESTABLISHED' | 'NOT_RECORDED' | 'INCOMPLETE' | 'KNOWN_PROBLEM' | 'OUTDATED';
export type EvidenceIndicator = {
  id: string; label: string; state: EvidenceState; status: string; scope: string; explanation: string;
  established: string[]; limits: string[]; nextSteps: string[];
  references: {label: string; path: string; sha256: string | null}[];
  recordedEngine: string | null; currentEngine: string; historical: boolean;
  reading: string;
  fields: {label: string; value: string; reference?: {label: string; path: string; sha256: string | null} | null}[];
};
export type ResearchConcern = {title: string; detail: string; nextStep: string; state: EvidenceState; scope: string; reading?: string};
export type AbilityInspection = {
  id: string; title: string; face: string | null; verdict: string; contribution: string;
  costs: string[]; effects: string[]; conditions: string[]; tradeoffs: string[]; uncertainties: string[];
  evidence: EvidenceIndicator[]; questions: ResearchConcern[]; resultIndexes: number[]; actionId: string;
  objectiveNotes: string[]; unresolvedNotes: string[];
};
export type RulesLine = {text: string; actionId: string | null; relevance: string | null};
export type CommanderView = {
  inspection: Inspection; authoredCard: Evidence['authoredCard']; headline: string;
  evidenceSummary: EvidenceIndicator[]; concerns: ResearchConcern[];
  objectiveConnection: string | null; image: CardImage | null;
  rules: RulesLine[];
};
export type CardImage = {
  index: number; card: string; status: string; reason: string; printingId: string | null;
  oracleId: string | null; set: string | null; collectorNumber: string | null;
  sourceUrl: string | null; fetchedAt: string | null; metadataSha256: string | null; algorithm: string;
  faces: {name: string; url: string; artist: string | null}[];
};
export type Health = {applicationRevision: string; argentumCommit: string; engineMode: string};

export async function api<T>(path: string, body?: unknown, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`/api/v1/${path}`, {
    ...(body === undefined ? {} : {
      method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body),
    }), signal: signal ?? AbortSignal.timeout(135_000),
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.message ?? `Request failed (${response.status})`);
  return data;
}
