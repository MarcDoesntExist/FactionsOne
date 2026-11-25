# Changelog

All notable changes to this fork are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/);
the project uses semantic versioning.

## [Unreleased]

### Added

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
