# CurseForge-Upload: Schritt für Schritt

## 1. Projekt anlegen (einmalig, nur du kannst das)

Auf https://authors.curseforge.com → **Create Project** → Minecraft → Mods.

| Feld | Wert |
|---|---|
| Name | `Create: Brass Anchor` |
| Summary | `A lightweight chunk loader for Create: keeps machines, crops and animals ticking, rides along on trains, contraptions and Sable ships.` |
| Description | Inhalt von `curseforge/description.md` (Editor auf Markdown stellen) |
| Logo | `curseforge/logo.png` (400×400) |
| Main Category | Utility & QoL → **Server Utility** (alternativ „Technology“) |
| Weitere Kategorien | Create (Addons), Technology, Storage/Automation nach Wunsch |
| License | MIT |
| Source URL | `https://github.com/Blaublue1303/brass_anchor_create` |
| Issues URL | `https://github.com/Blaublue1303/brass_anchor_create/issues` |

CurseForge prüft neue Projekte manuell (meist wenige Stunden bis 1–2 Tage).

## 2. Erste Datei hochladen

Datei: `brass-anchor-neoforge-1.21.1-0.1.0.jar` (aus `build/libs`).

| Feld | Wert |
|---|---|
| Release Type | Beta |
| Game Version | 1.21.1, NeoForge, Java 21, Client + Server |
| Changelog | Abschnitt 0.1.0 aus `CHANGELOG.md` |
| Relations | Create: **Required Dependency**, Sable: **Optional Dependency** |

## 3. Spätere Versionen automatisch

1. Projekt-ID (rechts auf der Projektseite, „Project ID“) in `gradle.properties` bei `curseforge_project_id` eintragen.
2. Auf https://legacy.curseforge.com/account/api-tokens bzw. im Author-Portal unter **API Tokens** einen Token erzeugen.
3. Im GitHub-Repo unter Settings → Secrets and variables → Actions ein Secret `CURSEFORGE_TOKEN` mit dem Token anlegen.
4. Neue Version: `mod_version` in `gradle.properties` erhöhen, Abschnitt in `CHANGELOG.md` ergänzen, committen und ein Tag `v<Version>` pushen. GitHub Actions baut, erstellt ein GitHub-Release und lädt auf CurseForge hoch.
