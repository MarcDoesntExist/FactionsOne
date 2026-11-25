FactionsOne - 1.5.0 - Paper 26.3
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

Features
---------
Factions with claims, relations and raids; an atomic faction bank with a
journal; warps, upgrades, missions and a points shop; faction shields, periodic
taxes, TNT bank fill/siphon, audit logs and a public `SkyAPI` with cancellable
events. Optional integrations are detected at runtime: Vault, EssentialsX,
PlaceholderAPI and WorldGuard.

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

