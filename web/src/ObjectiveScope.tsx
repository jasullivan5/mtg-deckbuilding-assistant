import type {Objective} from './api';

const scope: Record<string, {goal: string; searched: string; limits: string}> = {
  'own-nontoken-creature-deaths': {
    goal: 'A nontoken creature you control moves from the battlefield to a graveyard.',
    searched: 'Produces deaths through supported sacrifice and own-source operations; supplies creature inputs through supported original-creature self-return; benefits from deaths through qualifying triggered draw and creature or noncreature token creation, including Treasure; separately identifies death-enhanced removal.',
    limits: 'Other resource providers, life-change/mana payoffs and general replacements are outside this search. A self-return is not a death or a proven loop. Event restrictions and same-ability branches must qualify; unresolved conditions stay uncertain. Ordinary removal aimed at your own creature is excluded by policy.',
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
  'creature-died-this-turn': {
    goal: 'At least one creature has died during the current turn.',
    searched: 'Abilities with supported checks that use this history to enable or strengthen an outcome.',
    limits: 'Some abilities restrict whose death counts. This search does not look for ways to cause deaths or treat a direct dies trigger as a history check.',
  },
  'stock-own-graveyard': {
    goal: 'Have at least one creature card in your graveyard at a checkpoint.',
    searched: 'Supported sacrifices, mills and discards that could put a creature card into your graveyard.',
    limits: 'Results depend on the actual cards moved, replacement effects and what is still in your graveyard at the checkpoint.',
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
