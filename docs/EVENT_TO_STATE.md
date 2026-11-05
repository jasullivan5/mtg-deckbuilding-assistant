# Atomic objective relationship experiment

## Frozen expectations

Every case begins with no creature cards in the observing player's graveyard. Read the state after the stack and pending decisions settle, with no intervening new action. The actual engine move determines the event. Count actual creature cards in the observing player's graveyard at that checkpoint.

| Case | Setup and native action | Expected transition of the tracked object | Event A | State B | A establishes B through the tracked move |
| --- | --- | --- | --- | --- | --- |
| Owned | Activate Viscera Seer by sacrificing an owned and controlled Wolfbat | Battlefield → own graveyard | Yes | Yes | Yes |
| Borrowed | Same cost with an opponent-owned Wolfbat under our control | Battlefield → owner's graveyard | Yes | No | No |
| Token | Same cost with a controlled creature token | Battlefield → own graveyard, then token ceases to exist | No | No | No |
| Finality | Same cost with an owned and controlled Wolfbat bearing a finality counter | Battlefield → exile instead of graveyard | No | No | No |
| Mill | Target ourselves with Tome Scour with Wolfbat on top of the library | Library → own graveyard | No | Yes | No |

A token can die without satisfying the nontoken criterion. Controller and owner are independent. Finality applies to the move even if the sacrifice cost was paid. Sources include the retained Seer and Wolfbat printings, [Wizards' rules glossary](https://magic.wizards.com/en/keyword-glossary), the [finality release notes](https://magic.wizards.com/en/news/feature/duskmourn-house-of-horror-release-notes) and the [Comprehensive Rules](https://magic.wizards.com/en/rules) (dies, zones, tokens and state-based actions).
