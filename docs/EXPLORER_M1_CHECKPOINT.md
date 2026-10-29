# Explorer M1 checkpoint — ready for Prototype Lab, acceptance pending

Delivery PR: [#13](https://github.com/jasullivan5/mtg-deckbuilding-assistant/pull/13), open and **not merged**. Continue on `feature/explorer-m1`; original foundation **a35b9b5** is preserved and **aa0f497** reconciles main's sibling app-stable integration. No restart or restoration of the retired nested/pin workflow is needed.

Tested clean application revision: **9e187e074f52346ec5b6ca48f61a59d4437254c9**. Later delivery changes are documentation/receipt only. Normal engine remains clean on sibling `../argentum-engine`, **app-stable**, revision **e46fab1475ddc6f355e1c8fdc770b12bdc697b0f**. No engine changes were made for M1.

Fresh final checks: **465 application tests / 59 suites**, no failures/errors/skips; five HTTP cases include full fresh CLI evidence parity for all objectives. Locked frontend build and **four fixture browser tests** passed. **Two genuine live Chrome tests** passed: Judith tokens → Bitterblossom evidence → reanimation → Oviya tokens → rejected Murder; mobile keyboard/layout at 390px. Actual screenshots inspected. Windows launch, occupied-port/missing-asset diagnostics and Ctrl+C port closure checked. No Explorer server remains running.

Warm final token query: 5,087 ms native / 5,285 ms HTTP, 808,720-byte index, 12,099-byte Bitterblossom detail, 4,579 finding cards. Full exports remain 40–49 MB. Only latest run is retained. No imagery, independent translation certification or strategic recommendations are claimed.

Read [launch, guided exercises, API and limitations](EXPLORER_M1.md) and [compact verification receipt](../reference/explorer-m1/verification.json). Launch: `.\scripts\explorer.ps1`; wait for Explorer ready at http://127.0.0.1:8787; Ctrl+C stops it. Avoid installDist while live processes read packaged jars. Bulk generated reports, screenshots, logs and node_modules stay ignored.

**Next authorized work:** finish any PR checks/review corrections, then the user tries M1 in Prototype Lab. Evaluate whether Bitterblossom's conditional explanation, prerequisites, unsupported record and provisional assurance are understandable. Do not merge without user authorization. M1 is not accepted and M2+ have not begun. Preserve this checkpoint if usage interrupts review.
