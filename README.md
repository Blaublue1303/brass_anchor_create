# Create: Brass Anchor

> **KI-Hinweis:** Diese Mod wurde mit Hilfe von KI (Claude von Anthropic) erstellt. Code, Texturen, 3D-Modell, Logo und Texte sind KI-unterstützt entstanden und vom Autor (Blaublue1303) geprüft.

Ein sparsamer Chunk-Loader für **Minecraft 1.21.1, NeoForge, Create 6.0.10**, mit optionaler Unterstützung für **Sable** (Physik-Schiffe, z. B. Create: Aeronautics).

## Was er kann

- **Ein Block, der Chunks wirklich am Leben hält.** Die Chunks laufen im Simulationsbereich: Create-Maschinen, Öfen, Pflanzen (Zufalls-Ticks) und **Tiere und andere Entities ticken weiter**, auch wenn kein Spieler in der Nähe ist.
- **Radius per Rechtsklick:** 0 (nur der eigene Chunk) bis `maxRadius` (Standard 2, also 5×5). Schleichen + Rechtsklick zeigt den Status.
- **Redstone schaltet ab:** Liegt ein Signal an, gibt der Anker seine Chunks frei.
- **Create-Contraptions und Züge:** Ein Anker auf einem Lager, Kolben, einer Lore oder einem Zug lädt die Chunks um seine aktuelle Position und wandert mit.
- **Sable-Schiffe:** Ein Anker auf einem Schiff hält das Schiff über Sables eigenes Lade-Ticket geladen (auch über Neustarts) und lädt zusätzlich die Umgebung im eingestellten Radius.
- **Create-Brille:** Mit der Engineer's Goggles siehst du Status und Ladebereich.
- **Admin-Befehl:** `/brassanchor list` zeigt pro Dimension, wie viele Anker und Chunks aktiv sind.

## Warum er sparsam ist

- Ein stehender Anker hat **keinen Tick**. Er setzt beim Platzieren einmal NeoForge-Tickets (`TicketController`) und entfernt sie beim Abbauen.
- Tickets werden immer **als Differenz** geändert: Nur Chunks, die wirklich dazukommen oder wegfallen, werden angefasst.
- Bewegte Anker setzen Tickets **nur, wenn sie eine Chunk-Grenze überqueren**. Sonst kostet ein Tick einen Vergleich.
- Kein natürliches Mob-Spawning in geladenen Chunks (das macht Minecraft nur in Spielernähe), also kein Mob-Stau.
- Beim Start werden verwaiste Tickets (z. B. nach einem Absturz) automatisch entfernt.

## Rezept

```
Messingbarren   Enderauge          Messingbarren
Obsidian        Messinggehäuse     Obsidian
Messingbarren   Präzisionsmechanismus  Messingbarren
```

## Konfiguration

`serverconfig/brassanchor-server.toml` in der Welt:

| Option | Standard | Bedeutung |
|---|---|---|
| `maxRadius` | 2 | größter Radius (0–8) |
| `defaultRadius` | 1 | Radius frisch platzierter Anker |
| `loadOnContraptions` | true | Anker auf Contraptions und Zügen laden mit |
| `loadOnSableShips` | true | Anker auf Sable-Schiffen laden mit |
| `maxAnchorsPerDimension` | 0 | Limit pro Dimension, 0 = unbegrenzt |

## Bauen

```
./gradlew build
```
Das Jar liegt danach in `build/libs/`. Benötigt Java 21. Create ist Pflicht, Sable optional.

## Veröffentlichen

Siehe [curseforge/ANLEITUNG.md](curseforge/ANLEITUNG.md). Neue Versionen gehen per Tag `v*` automatisch auf GitHub und CurseForge.
