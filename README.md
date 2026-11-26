FactionsOne - 1.6.3 - Paper 26.3
====================
Note from externo6:<br>
Credit to [RenegadeEagle](https://github.com/RenegadeEagle) and [drtshock](https://github.com/drtshock) for their code
in this.<br>
If you do use this fork make sure you backup all data. <br>
I am not responsible for any loss of data.<br>
<br>
This fork was built upon adapting the changes [drtshock's](https://github.com/drtshock)
1.6 [Factions UUID update](https://github.com/drtshock/Factions). <br>
https://github.com/externo6/Factions/releases

====================
This plugin will allow the players on the server to create factions/guilds. The factions can claim territory that will
be protected from non-members. Factions can forge alliances and declare themselves enemies with others. Land may be
taken from other factions through war.

The goals of this plugin:

* The players should be able to take care of anti-griefing themselves.
* Inspire politics and intrigues on your server.
* Guilding and team spirit! :)

Usage
---------
<b>Read the full userguide here: [http://massivecraft.com/dev/factions](http://massivecraft.com/dev/factions)</b>

A default config file will be created on the first run.

## YAML configuration

On first start FactionsOne creates `config.yml` and `messages.yml` in `plugins/Factions`:

- `config.yml` mirrors every editable `Conf` setting. It is loaded after the
  legacy `conf.json`; keys present in the YAML override the JSON value. Remove a
  key to fall back to `conf.json`. `/f reload conf` reapplies it, and edits made
  through `/f config` are written back to both files.
- `messages.yml` exposes every centralized `Lang` message. Existing files keep
  their custom values and missing keys use the compiled default, so upgrades do
  not require deleting the file.
- `gui.yml`, `missions.yml`, `shop.yml` and `upgrades.yml` remain the registries
  for menus and feature content.

## PlaceholderAPI

When PlaceholderAPI is installed, the optional `factionsone` expansion exposes
player, faction, relation, ranking, territory, warp, tax and shield values.
Examples: `%factionsone_faction%`, `%factionsone_faction_power%`,
`%factionsone_faction_bank%`, `%factionsone_faction_shield_remaining%`,
`%factionsone_relation_enemy%`, `%factionsone_rank_power%` and
`%factionsone_top_power_1_tag%`. The complete list is in `docs/API.md`.

Optional web map
---------
BlueMap is supported as an optional, read-only claim overlay. It is disabled by
default; set `mapEnabled: true` in `conf.json` and install BlueMap with its
web resources accepted. The layer name is `mapLayerName` (default `Factions`),
and `mapReferenceFaction` can name a faction id or tag: when set, claims are
colored by relation to that faction instead of per-faction.
`mapRefreshDebounceTicks` and `mapShapeY` tune refresh coalescing and marker
height. Without BlueMap the plugin starts normally and logs that the
integration is disabled; a failed API hook is also non-fatal.

Features
---------
Factions with claims, relations and raids; an atomic faction bank with a
journal; warps, upgrades, missions and a points shop; faction shields, periodic
taxes, TNT bank fill/siphon, audit logs and a public `FactionsAPI` with cancellable
events. Optional integrations are detected at runtime: Vault, EssentialsX,
PlaceholderAPI, WorldGuard and BlueMap. All configuration is available in YAML
(`config.yml`, `messages.yml`, `gui.yml`, `missions.yml`, `shop.yml`,
`upgrades.yml`); `conf.json` remains as the backward-compatible fallback.

Build and verification
----------
Requires JDK 25 and a Paper 26.3 server jar for the smoke test
(`test-server/paper-26.3-259.jar`, `-PpaperServerJar=...` or
`PAPER_SERVER_JAR`).

```
JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true ./gradlew --no-daemon cleanTest test smokeTest
```

`./scripts/check-authors.sh` verifies the author headers. `./gradlew liveTest`
runs the optional Paper crash harnesses when Node and the server jar are
available.

Documentation
----------
- `docs/API.md` — public API and cancellable events
- `docs/ARCHITETTURA.md` — packages, threading and persistence
- `docs/TEST_MANUALI.md` — live checks that the headless suite cannot cover
- `docs/RECOVERY.md` — backup and recovery procedures
- `CHANGELOG.md` — release history

License
----------
This project has a LGPL license just like the Bukkit project.<br>
This project uses [GSON](http://code.google.com/p/google-gson/) which has
a [Apache 2.0 license](http://www.apache.org/licenses/LICENSE-2.0 ).

