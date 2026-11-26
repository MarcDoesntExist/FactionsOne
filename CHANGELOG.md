# Changelog

All notable changes to this fork are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the project uses semantic versioning.

## [Unreleased]

### Fixed

- A double chest spanning two claimed chunks now counts its contents once,
  from the left half, instead of once per chunk.

## [1.6.1] - 2026-11-26

### Fixed

- Claim values refresh within a second when hoppers move items into or out of
  claimed containers, throttled to one scan per second per chunk.

## [1.6.0] - 2026-11-26

### Fixed

- `/f top value` refreshes immediately after a claim value scan instead of
  waiting for the ranking cache, and reports when the metric is disabled.
- Waterlogged TNT no longer bypasses the target faction's TNT flag, offline
  protection or shield when it breaks the six high-blast-resistance blocks.
- Bucket empty/fill now checks the block actually changed, not only the block
  clicked, so placing liquid across a claim boundary is rejected.
- Sponge absorption is cancelled when it would change water in a different
  faction's territory, preventing cross-claim drainage.
- Hostile monsters can no longer walk from wilderness into a claim whose
  `monsters` flag is disabled; movement inside the same territory is left
  untouched.

### Added

- `/f top value` ranks factions by the value of spawners and container
  contents inside their claims. Prices are configurable with
  `claimValueSpawners`, `claimValueContainers` and `claimValueItems`; the scan
  runs in small slices (`claimValueChunksPerTick`), refreshes dirty chunks after
  claims, block changes and container closes, and `claimValueEnabled` turns the
  metric off.
- Betrayal rules for alliances: declaring war on an ally now requires setting
  the relation to neutral first, and attacking a former ally within
  `betrayalWindowSeconds` costs `betrayalPowerPenalty` power. Both values are
  configurable and `betrayalRulesEnabled` turns the rules off.
- Separate `/fa` namespace for server administration commands (`bypass`/`admin`,
  `reload`, `saveall`, `config`, `powerboost`, `disband`, `kick`, `join`), with
  its own help and tab completion; `/f` remains unchanged.
- Optional BlueMap claim overlay (`mapEnabled`, default `false`): claimed
  chunks are merged into rectangles and refreshed after claim, unclaim and
  relation changes. Colors follow the relation to `mapReferenceFaction` when
  configured, otherwise a stable per-faction color. BlueMap is loaded through
  reflection and a missing or failed API leaves the plugin running.

## [1.5.0] - 2026-11-25

### Added

- Menu items support independent `view` and `click` requirements; hidden entries
  are re-evaluated every second and clicks are rejected when the faction or
  FPerm changed after opening the menu.
- `/f power` now shows the faction power breakdown: member sum, claim count and
  the combined bonus/malus.
- `/f show` now displays a public status line with shield/grace, raidability and
  whether the faction is at war with the viewer.
- TNT bank `fill` and `siphon` commands that move only the amount the
  dispenser inventories actually accepted or released, with FINE verification
  logs.
- Faction tax supports a percent-of-bank mode in addition to the flat
  per-member/per-chunk formula; the existing five-argument settings stay valid.
  Members in `tax-exempt-roles` are excluded from the per-member component.
- Symmetric shield rules: protected factions cannot attack or overclaim, and
  protected territory cannot be overclaimed.
- Warp use can charge a configurable cost before warmup, with automatic refund
  when the warmup is cancelled or the teleport fails.
- Public API accessors for factions, players, claims, bank balances and warps,
  plus cancellable bank, warp, upgrade, shield, role and mission events.
- Shop offers can be perks: temporary faction power, faction bank money,
  faction points and temporary flags. Points are charged only after the perk
  validates, and are refunded if its application fails; temporary perks expire.
- Missions support bank deposits, player/mob kills, mining and player deaths,
  with optional money and item rewards, daily/weekly reset periods and GUI
  reset information. Old `missions.yml` files without the new fields keep
  loading with period `NONE` and no extra rewards.
- Upgrade effects for faction member and ally limits, claim limits and the
  per-player power cap. `factionAllyLimit` and `claimedLandsMaxHardCap`
  default to 0 (no limit); old `upgrades.yml` files with only `WARP_LIMIT`
  and `TNT_LIMIT` keep loading unchanged.
- Faction audit log: `/f logs [all|bank] [page]` filters bank and tax
  movements, the GUI lists the newest entry first, and role changes are
  recorded. Faction creation/disband and home/description changes are not
  audit events.
- Durable data files are versioned. `mailbox.json` and `idempotency.json`
  carry a root `version` field; `bank-journal.jsonl` starts with a
  `{"version":2}` header. Legacy files are migrated after a rotated
  `backups/<file>.<timestamp>.bak` copy, preserving claims, keys and entries.
  A corrupt file is moved to `<file>.corrupt-<timestamp>` and the plugin starts
  empty instead of overwriting it; a newer version is left untouched. The data
  folder version is now 2, so the first upgrade also takes a full
  pre-migration snapshot. See `docs/RECOVERY.md` for restore and reconcile
  procedures.
- Durable bank journal (`bank-journal.jsonl`): every movement is written as
  `PENDING` and then committed or aborted. A transfer whose credit and refund
  both fail is recorded as `COMPENSATION_PENDING`; the periodic retry and
  `/f admin bank reconcile` (usable from the console) credit it and write
  `COMPENSATED`. `/econfail on|off` in the local probe reproduces the failure.
- Persistent idempotency store (`idempotency.json`) with atomic writes. Tax
  periods claim `tax:<faction>:<period>` and mailbox deliveries claim
  `mailbox:<claim>`, refusing a replay after a restart.
- Offline payout mailbox (`mailbox.json`, `/f mail`, `/f mail claim [id|all]`).
  Payouts expire after `mailboxTtlDays`, respect `mailboxMaxPerPlayer`, and are
  returned to their source account through a retryable refund. `/f mail send`
  queues a payout from a faction bank or the console universe account, and the
  periodic land reward queues offline members instead of crediting them directly.
  Delivery is explicitly **at-most-once**: a crash between the idempotency key
  claim and the credit leaves the claim `DELIVERY_UNKNOWN`, never `DELIVERED`.
  An operator resolves it with `/f mail reconcile <id> paid|retry` after checking
  the economy; `retry` releases the key for one more claim. Recovery never turns
  an unknown claim back into `PENDING` automatically, even if its idempotency
  key is lost or corrupted, so an ambiguous payout cannot be retried by restart.
- Storage retention (`idempotencyRetentionDays`, default 180;
  `mailboxRetentionDays`, default 30): at enable and from the mailbox maintenance
  task the stores are compacted atomically. Old idempotency keys are removed,
  while keys for pending/ambiguous claims and unfinished refunds are protected.
  Delivered claims and fully refunded expired claims are dropped; pending,
  ambiguous and unrefunded claims are retained.
- Bank journal retention (`bankJournalRetentionDays`, default 30): terminal
  entries are compacted atomically with rotated backups at enable and from the
  compensation task. Pending movements and pending compensations are kept.
- Periodic tax periods are journaled (`tax-journal.jsonl`). A crash between
  the idempotency claim and the debit leaves the period `TAX_UNKNOWN`: the
  periodic run never charges it again and `/f tax run <faction> retry` is the
  only way to resolve it after checking the bank. Settled periods are never
  recharged by the periodic run; `force` may repeat one, but it also skips
  `TAX_UNKNOWN` periods.
- `./gradlew liveTest`: runs the local Paper crash harnesses (`live-journal.js`,
  `live-refund.js`) and skips with an explicit reason when `node`, the Paper jar
  or the local probe sources are missing.
- `/f admin bank status` (also from the console) prints a read-only snapshot:
  pending journal movements, pending compensations, pending mailbox payouts, the
  `DELIVERY_UNKNOWN` and `REFUND_UNKNOWN` counters, the retention windows and
  the last compaction time. `BankStatus` is pure and never writes.
- `BankService` now journals credits, debits, transfers, balance sets and
  universe movements; account locking was extracted to `AccountLocks`.
- Headless coverage for journal replay, compensation retry, idempotent claims,
  concurrent mailbox claims, TTL/refund, capacity, restart recovery and the
  crash window between key claim and delivery, plus `test-server/live-journal.js`
  for the real Paper restart path.
- Faction grace and shield windows (`shieldEnabled`, `shieldGraceHours`,
  `shieldMaxHours`): new factions start with grace, and leaders (or the
  console) can run `/f shield set <hours> [faction]`. While a window is active,
  enemies and neutrals cannot build, break, PvP or explode blocks in that
  faction's territory; members and allies are unaffected. `/f shield [faction]`
  shows the remaining time, and changes are written to the faction log as
  `SHIELD_SET`.
- Periodic faction tax (`/f tax info`, `/f tax run [force]`): a configurable
  base, per-member and per-chunk upkeep charged through the atomic faction
  bank. Missed periods accumulate arrears, and after `taxGracePeriods` the
  faction becomes insolvent; `taxInsolvencyAction` can stop at `NONE` or
  unclaim one chunk. The run is idempotent per interval, emits
  `FactionTaxCollectedEvent` / `FactionTaxFailedEvent` and writes `TAX_PAID`,
  `TAX_FAILED`, `TAX_INSOLVENT` and `TAX_UNCLAIM` to the faction log.
- `PluginYamlContractTest`: fails the build when a permission node used by the
  code is missing from `plugin.yml`, or when a `children` entry points at an
  undeclared node.
- `./gradlew smokeTest`: boots the built jar on a real Paper 26.3 server,
  dispatches every command root from the console and fails on ERROR/SEVERE,
  stack traces or unknown commands. The Paper jar is taken from
  `-PpaperServerJar`, `PAPER_SERVER_JAR` or `test-server/paper-26.3-259.jar`.
- GitHub Actions CI: build + unit tests on JDK 25, plus the Paper boot smoke
  test.
- Atomic faction bank (`bank/BankService`): deposits, withdrawals and
  faction-to-faction transfers lock the accounts involved, validate the amount
  in cents, debit before crediting and put the debit back when the credit
  fails. Successful movements are written to the faction log as `BANK_IN` /
  `BANK_OUT`.
- 11 headless bank tests: rejected amounts, insufficient funds, failed-credit
  compensation, concurrent overdraft attempts and deadlock-free opposite
  transfers.

### Fixed

- Corrupt `players.json`, `factions.json` or `board.json` is moved to a
  `*_bad` copy and FactionsOne disables itself instead of saving empty state
  over the original data.
- WorldGuard chunk checks use the inclusive maximum Y and document that any
  overlapping region blocks a claim while PvP flags keep WorldGuard priority.
- An interrupted mailbox refund is no longer treated as completed. When the
  refund key was claimed but the source credit is unconfirmed, the claim becomes
  `REFUND_UNKNOWN` instead of `refunded`; the periodic expiry run leaves it
  alone until an operator checks the source account and runs
  `/f mail reconcile <id> refunded|retry-refund`.
- Negative amounts no longer reverse a bank transfer: `/f money deposit -100`
  used to become a withdrawal for callers holding bank withdraw rights.
- A balance check followed by a withdrawal can no longer overdraw an account;
  providers that accept the overdraft are rolled back immediately.
- Console `/f money ff`, `fp` and `pf` crashed with a `NullPointerException`
  while logging a successful transfer.
- `Faction#getAccountId` no longer creates economy accounts as a side effect.
- `factions.ban`, `factions.banlist` and `factions.unban` were only referenced
  from the `factions.kit.halfplayer` children list and never declared. They are
  now real permission nodes, so `/f ban`, `/f unban` and `/f banlist` resolve
  the intended defaults instead of falling back to the server's implicit
  op-only default.

## [1.4.0]

Paper 26.3 port of FactionsOne (LGPL 2.1).

### Added

- Inventory GUI framework (`/f menu`, members, permissions, read-only pages)
  with click/drag cancellation and permission re-checks.
- Faction warps (`/f warp`, `/f setwarp`, `/f delwarp`, `/f warps`).
- Faction fly with automatic disable on territory exit.
- Faction TNT bank (`/f tnt`).
- Upgrades, missions and faction shop commands and config files.
- Faction audit log (`/f logs`) and faction bans (`/f ban`, `/f unban`,
  `/f banlist`).
- Extended roles/permissions and flags, including bank deposit/withdraw gates.
- Adventure chat rendering and action-bar territory map.

### Changed

- Atomic data writes (temporary file + atomic move) with rotated backups.
- Numbered data-version migrations with a pre-migration snapshot.
- Asynchronous save queue; the shutdown flush is bounded.
