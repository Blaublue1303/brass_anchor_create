package io.github.blaublue1303.brassanchor.loading;

import io.github.blaublue1303.brassanchor.BrassAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Der einzige Weg, auf dem diese Mod Chunks laedt: der NeoForge-{@link TicketController}.
 *
 * Alle Tickets sind "ticking", denn nur ticking Tickets setzen den Chunk in den Simulationsbereich.
 * Erst dann ticken Block-Entities (also Create-Maschinen), Zufalls-Ticks (Pflanzen) und Entities
 * (Tiere wachsen, Zuege, Items). Ohne ticking waere der Chunk zwar geladen, aber eingefroren.
 *
 * Besitzer:
 *  - BlockPos fuer stehende Anker
 *  - UUID fuer bewegte Anker (Contraption, Zug, Sable-Schiff)
 */
public final class AnchorTickets {
    public static final TicketController CONTROLLER =
            new TicketController(BrassAnchor.id("anchor"), AnchorTickets::validate);

    private AnchorTickets() {}

    public static void register(RegisterTicketControllersEvent event) {
        event.register(CONTROLLER);
    }

    static void force(ServerLevel level, BlockPos owner, long chunk, boolean add) {
        CONTROLLER.forceChunk(level, owner, chunkX(chunk), chunkZ(chunk), add, true);
    }

    static void force(ServerLevel level, UUID owner, long chunk, boolean add) {
        CONTROLLER.forceChunk(level, owner, chunkX(chunk), chunkZ(chunk), add, true);
    }

    /**
     * Wird beim Laden der Welt aufgerufen, bevor NeoForge die gespeicherten Tickets wieder setzt.
     * Alles, was unser AnchorManager nicht kennt (z. B. nach einem Absturz), fliegt raus.
     */
    private static void validate(ServerLevel level, TicketHelper helper) {
        AnchorManager manager = AnchorManager.get(level);
        int removed = 0;
        for (BlockPos owner : new ArrayList<>(helper.getBlockTickets().keySet())) {
            if (!manager.knowsStatic(owner)) {
                helper.removeAllTickets(owner);
                removed++;
            }
        }
        for (UUID owner : new ArrayList<>(helper.getEntityTickets().keySet())) {
            if (!manager.knowsMoving(owner)) {
                helper.removeAllTickets(owner);
                removed++;
            }
        }
        if (removed > 0) {
            BrassAnchor.LOGGER.info("{} verwaiste Messing-Anker-Tickets in {} entfernt", removed, level.dimension().location());
        }
    }

    static int chunkX(long chunk) {
        return (int) chunk;
    }

    static int chunkZ(long chunk) {
        return (int) (chunk >>> 32);
    }
}
