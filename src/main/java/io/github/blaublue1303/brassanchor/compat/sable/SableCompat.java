package io.github.blaublue1303.brassanchor.compat.sable;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.ticket.SubLevelLoadingTicketType;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import io.github.blaublue1303.brassanchor.BrassAnchor;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Unterstuetzung fuer Sable (Physik-Schiffe, z. B. Create: Aeronautics).
 *
 * Diese Klasse wird nur geladen, wenn Sable installiert ist.
 *
 * Ein Anker auf einem Schiff macht zwei Dinge:
 *  1. Er gibt dem Schiff ein Sable-Lade-Ticket. Damit bleibt das Schiff samt aller Chunks, in denen es
 *     sich gerade befindet, geladen und tickend (Sable kuemmert sich selbst darum, auch ueber Neustarts).
 *  2. Er laedt per NeoForge-Ticket zusaetzlich ein Quadrat mit dem eingestellten Radius um das Schiff,
 *     damit Maschinen und Tiere in der Umgebung weiterlaufen. Das wird alle 10 Ticks nachgefuehrt.
 */
public final class SableCompat {
    private static SubLevelLoadingTicketType<BlockPos> ticketType;

    private SableCompat() {}

    private static final class ShipAnchor {
        UUID subLevelId;
        UUID owner;
        int radius;
    }

    public static void init() {
        ticketType = SubLevelLoadingTicketType.create(BrassAnchor.id("anchor"), BlockPos.CODEC);
    }

    public static boolean isOnShip(ServerLevel level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel;
    }

    public static void register(ServerLevel level, AnchorManager manager, BlockPos pos, int radius) {
        if (!(Sable.HELPER.getContaining(level, pos) instanceof ServerSubLevel ship)) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        ShipAnchor anchor = manager.sableAnchors.get(pos) instanceof ShipAnchor a ? a : null;
        if (anchor != null && !anchor.subLevelId.equals(ship.getUniqueId())) {
            unregister(level, manager, pos);
            anchor = null;
        }
        if (anchor == null) {
            anchor = new ShipAnchor();
            anchor.subLevelId = ship.getUniqueId();
            anchor.owner = ownerId(ship.getUniqueId(), pos);
            manager.sableAnchors.put(pos.immutable(), anchor);
        }
        anchor.radius = radius;
        container.addForceLoadTicket(ship, ticketType, pos.immutable());
        updateSurroundings(level, manager, ship, anchor);
    }

    public static void unregister(ServerLevel level, AnchorManager manager, BlockPos pos) {
        if (!(manager.sableAnchors.remove(pos) instanceof ShipAnchor anchor)) {
            return;
        }
        manager.removeMoving(level, anchor.owner);
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container != null && container.getSubLevel(anchor.subLevelId) instanceof ServerSubLevel ship) {
            container.removeForceLoadTicket(ship, ticketType, pos);
        }
    }

    /** Alle 10 Ticks: Umgebung der Schiffe nachfuehren. */
    public static void tick(ServerLevel level, AnchorManager manager) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Object>> it = manager.sableAnchors.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Object> e = it.next();
            if (!(e.getValue() instanceof ShipAnchor anchor)) {
                continue;
            }
            SubLevel sub = container.getSubLevel(anchor.subLevelId);
            if (!(sub instanceof ServerSubLevel ship) || ship.isRemoved()) {
                // Schiff zerlegt oder entfernt: Der Block ist dann wieder in der Welt bzw. weg und
                // meldet sich ueber seinen Block-Entity selbst neu an.
                manager.removeMoving(level, anchor.owner);
                it.remove();
                continue;
            }
            updateSurroundings(level, manager, ship, anchor);
        }
    }

    private static void updateSurroundings(ServerLevel level, AnchorManager manager, ServerSubLevel ship, ShipAnchor anchor) {
        var position = ship.logicalPose().position();
        long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(Mth.floor(position.x())),
                SectionPos.blockToSectionCoord(Mth.floor(position.z())));
        manager.updateMoving(level, anchor.owner, chunk, anchor.radius);
    }

    private static UUID ownerId(UUID shipId, BlockPos pos) {
        ByteBuffer buf = ByteBuffer.allocate(25);
        buf.put((byte) 's');
        buf.putLong(shipId.getMostSignificantBits());
        buf.putLong(shipId.getLeastSignificantBits());
        buf.putLong(pos.asLong());
        return UUID.nameUUIDFromBytes(buf.array());
    }
}
