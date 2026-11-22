package io.github.blaublue1303.brassanchor.compat.create;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import io.github.blaublue1303.brassanchor.AnchorConfig;
import io.github.blaublue1303.brassanchor.block.BrassAnchorBlock;
import io.github.blaublue1303.brassanchor.block.BrassAnchorBlockEntity;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Messing-Anker auf Create-Contraptions (Lager, Kolben, Loren, Zuege ...).
 *
 * Laedt ein Quadrat um die aktuelle Position des Ankers. Die Tickets werden nur neu gesetzt, wenn
 * der Anker eine Chunk-Grenze ueberquert; sonst kostet ein Tick einen Vergleich zweier longs.
 * Der Radius ist auf Contraptions mindestens 1, damit der naechste Chunk schon geladen ist, bevor
 * die Contraption ihn erreicht (wichtig fuer schnelle Zuege).
 */
public class AnchorMovementBehaviour implements MovementBehaviour {

    /** Laufzeitdaten pro Anker, liegen in {@link MovementContext#temporaryData}. */
    private static final class State {
        UUID owner;
        long chunk = Long.MIN_VALUE;
        int idleTicks;
    }

    @Override
    public boolean mustTickWhileDisabled() {
        return true;
    }

    @Override
    public void tick(MovementContext context) {
        if (!(context.world instanceof ServerLevel level) || context.position == null) {
            return;
        }
        if (!AnchorConfig.contraptions() || !context.state.getValue(BrassAnchorBlock.ACTIVE)) {
            return;
        }
        AbstractContraptionEntity entity = context.contraption.entity;
        if (entity == null) {
            return;
        }

        State s = context.temporaryData instanceof State st ? st : null;
        if (s == null) {
            s = new State();
            s.owner = ownerId(entity.getUUID(), context);
            context.temporaryData = s;
        }

        Vec3 p = context.position;
        long chunk = ChunkPos.asLong(SectionPos.blockToSectionCoord(Mth.floor(p.x)),
                SectionPos.blockToSectionCoord(Mth.floor(p.z)));
        AnchorManager manager = AnchorManager.get(level);
        if (chunk != s.chunk) {
            s.chunk = chunk;
            s.idleTicks = 0;
            manager.updateMoving(level, s.owner, chunk, radius(context));
        } else if (++s.idleTicks >= 100) {
            s.idleTicks = 0;
            manager.touchMoving(level, s.owner);
        }
    }

    @Override
    public void stopMoving(MovementContext context) {
        if (!(context.world instanceof ServerLevel level)) {
            return;
        }
        UUID owner = null;
        if (context.temporaryData instanceof State s) {
            owner = s.owner;
        } else if (context.contraption != null && context.contraption.entity != null) {
            owner = ownerId(context.contraption.entity.getUUID(), context);
        }
        if (owner != null) {
            AnchorManager.get(level).removeMoving(level, owner);
        }
        context.temporaryData = null;
    }

    private static int radius(MovementContext context) {
        int r = context.blockEntityData != null && context.blockEntityData.contains(BrassAnchorBlockEntity.RADIUS_KEY)
                ? context.blockEntityData.getInt(BrassAnchorBlockEntity.RADIUS_KEY)
                : AnchorConfig.defaultRadius();
        return Mth.clamp(r, 1, Math.max(1, AnchorConfig.maxRadius()));
    }

    /** Stabile ID aus Contraption-Entity und Position des Ankers in der Contraption. */
    private static UUID ownerId(UUID entityId, MovementContext context) {
        ByteBuffer buf = ByteBuffer.allocate(24);
        buf.putLong(entityId.getMostSignificantBits());
        buf.putLong(entityId.getLeastSignificantBits());
        buf.putLong(context.localPos.asLong());
        return UUID.nameUUIDFromBytes(buf.array());
    }
}
