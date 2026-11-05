import type {Objective} from './api';

const scope: Record<string, {goal: string; searched: string; limits: string}> = {
  'own-nontoken-creature-deaths': {
    goal: 'A nontoken creature you control moves from the battlefield to a graveyard.',
    searched: 'Recognized sacrifices and other supported ways a creature can cause its own death, plus removal that a death triggers, enables or improves.',
    limits: 'General death rewards, such as Treasure, and renewable sacrifice material are outside this search. Ordinary removal aimed at your own creature is excluded by policy.',
  },
  'reanimate-own-creatures': {
    goal: 'Return creature cards from your graveyard to the battlefield under your control.',
    searched: 'Supported movements of the original creature card from your graveyard to the battlefield under your control.',
    limits: 'A return to hand or a token copy does not meet this check. Some conditions remain unresolved.',
  },
  'create-creature-tokens': {
    goal: 'Create one or more creature tokens under your control.',
    searched: 'Supported token-creation effects, including recognized variable amounts that can produce at least one creature token.',
    limits: 'Noncreature tokens are outside this goal. Unknown token types, quantities or conditions can leave a check unresolved.',
  },
};

export function ObjectiveScope({objective, afterSearch = false}: {objective: Objective; afterSearch?: boolean}) {
  const description = scope[objective.id];
  if (!description) return null;
  return <section className="objective-scope" aria-label={afterSearch ? 'Scope of this search' : 'Objective and search scope'}>
    <dl>
      <div><dt>The objective</dt><dd>{description.goal}</dd></div>
      <div><dt>Currently searched</dt><dd>{description.searched}</dd></div>
      <div><dt>Important limits</dt><dd>{description.limits}</dd></div>
    </dl>
  </section>;
}
