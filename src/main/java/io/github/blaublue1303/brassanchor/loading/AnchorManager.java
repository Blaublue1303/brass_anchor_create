package io.github.blaublue1303.brassanchor.loading;

import io.github.blaublue1303.brassanchor.AnchorConfig;
import io.github.blaublue1303.brassanchor.BrassAnchor;
import io.github.blaublue1303.brassanchor.block.BrassAnchorBlock;
import io.github.blaublue1303.brassanchor.block.BrassAnchorBlockEntity;
import io.github.blaublue1303.brassanchor.compat.sable.SableCompat;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Buchfuehrung aller Anker einer Dimension. Gespeichert wird nur, welcher Besitzer welche Chunks
 * haelt, damit nach einem Neustart verwaiste Tickets erkannt und Tickets exakt wieder entfernt
 * werden koennen.
 *
 * Alle Aenderungen laufen als Differenz: Es werden nur Chunks angefasst, die wirklich dazukommen
 * oder wegfallen.
 */
public final class AnchorManager extends SavedData {
    private static final String NAME = BrassAnchor.MOD_ID + "_anchors";
    /** Ein bewegter Anker, der so lange nicht mehr gemeldet hat, gilt als verwaist (60 s). */
    private static final long MOVING_TIMEOUT = 20 * 60;

    /** Stehende Anker: Position -> geladene Chunks. */
    private final Map<BlockPos, LongOpenHashSet> statics = new HashMap<>();
    /** Bewegte Anker: Besitzer-UUID -> Zustand. */
    private final Map<UUID, Moving> moving = new HashMap<>();

    /** Anker, deren Zustand im naechsten Tick neu ausgewertet wird (nicht gespeichert). */
    private final Set<BlockPos> pending = new LinkedHashSet<>();
    /** Anker auf Sable-Schiffen; wird von {@link SableCompat} verwaltet (nicht gespeichert). */
    public final Map<BlockPos, Object> sableAnchors = new HashMap<>();

    private static final class Moving {
        final LongOpenHashSet chunks = new LongOpenHashSet();
        long center = Long.MIN_VALUE;
        int radius = -1;
        long lastSeen = -1;
    }

    public static AnchorManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(AnchorManager::new, AnchorManager::load, null), NAME);
    }

    // ------------------------------------------------------------------ Lebenszyklus der Bloecke

    /** Merkt einen Anker zur Auswertung im naechsten Tick vor (Platzieren, Laden, Redstone, Radius). */
    public void requestUpdate(BlockPos pos) {
        pending.add(pos.immutable());
    }

    /** Entfernt einen Anker sofort (Block abgebaut oder in eine Contraption/ein Schiff verschoben). */
    public void remove(ServerLevel level, BlockPos pos) {
        pending.remove(pos);
        removeStatic(level, pos);
        if (BrassAnchor.isSableLoaded()) {
            SableCompat.unregister(level, this, pos);
        }
    }

    public void tick(ServerLevel level) {
        if (!pending.isEmpty()) {
            processPending(level);
        }
        long time = level.getGameTime();
        if (BrassAnchor.isSableLoaded() && !sableAnchors.isEmpty() && time % 10 == 0) {
            SableCompat.tick(level, this);
        }
        if (!moving.isEmpty() && time % 200 == 0) {
            expireMoving(level, time);
        }
        if (!statics.isEmpty() && time % 6000 == 0) {
            verifyStatics(level);
        }
    }

    private void processPending(ServerLevel level) {
        BlockPos[] todo = pending.toArray(new BlockPos[0]);
        pending.clear();
        for (BlockPos pos : todo) {
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof BrassAnchorBlockEntity anchor)) {
                remove(level, pos);
                continue;
            }
            BlockState state = be.getBlockState();
            if (!state.getValue(BrassAnchorBlock.ACTIVE)) {
                remove(level, pos);
                anchor.setLimited(false);
                continue;
            }
            int radius = Math.min(anchor.getRadius(), AnchorConfig.maxRadius());

            if (BrassAnchor.isSableLoaded() && AnchorConfig.sable() && SableCompat.isOnShip(level, pos)) {
                removeStatic(level, pos);
                boolean ok = isKnown(pos) || hasCapacity();
                if (ok) {
                    SableCompat.register(level, this, pos, radius);
                }
                anchor.setLimited(!ok);
                continue;
            }

            boolean ok = isKnown(pos) || hasCapacity();
            if (ok) {
                ChunkPos c = new ChunkPos(pos);
                setStatic(level, pos, square(c.x, c.z, radius));
            }
            anchor.setLimited(!ok);
        }
    }

    private boolean isKnown(BlockPos pos) {
        return statics.containsKey(pos) || sableAnchors.containsKey(pos);
    }

    private boolean hasCapacity() {
        int max = AnchorConfig.maxAnchorsPerDimension();
        return max <= 0 || statics.size() + sableAnchors.size() < max;
    }

    /** Alle 5 Minuten: Anker, deren Block auf unerwartetem Weg verschwunden ist, aufraeumen. */
    private void verifyStatics(ServerLevel level) {
        for (BlockPos pos : statics.keySet().toArray(new BlockPos[0])) {
            if (level.isLoaded(pos) && !(level.getBlockEntity(pos) instanceof BrassAnchorBlockEntity)) {
                removeStatic(level, pos);
            }
        }
    }

    // ------------------------------------------------------------------ stehende Anker

    private void setStatic(ServerLevel level, BlockPos pos, LongOpenHashSet wanted) {
        LongOpenHashSet current = statics.get(pos);
        if (current == null) {
            current = new LongOpenHashSet();
            statics.put(pos, current);
        }
        if (applyDiff(current, wanted, chunk -> AnchorTickets.force(level, pos, chunk, true),
                chunk -> AnchorTickets.force(level, pos, chunk, false))) {
            setDirty();
        }
        if (current.isEmpty()) {
            statics.remove(pos);
        }
    }

    private void removeStatic(ServerLevel level, BlockPos pos) {
        LongOpenHashSet current = statics.remove(pos);
        if (current != null) {
            for (LongIterator it = current.iterator(); it.hasNext(); ) {
                AnchorTickets.force(level, pos, it.nextLong(), false);
            }
            setDirty();
        }
    }

    public boolean knowsStatic(BlockPos pos) {
        return statics.containsKey(pos);
    }

    // ------------------------------------------------------------------ bewegte Anker

    /**
     * Setzt die Tickets eines bewegten Ankers auf ein Quadrat um {@code center}. Billig, wenn sich
     * nichts geaendert hat: dann wird nur der Zeitstempel erneuert.
     */
    public void updateMoving(ServerLevel level, UUID owner, long center, int radius) {
        Moving m = moving.computeIfAbsent(owner, k -> new Moving());
        m.lastSeen = level.getGameTime();
        if (m.center == center && m.radius == radius) {
            return;
        }
        m.center = center;
        m.radius = radius;
        LongOpenHashSet wanted = square(ChunkPos.getX(center), ChunkPos.getZ(center), radius);
        if (applyDiff(m.chunks, wanted, chunk -> AnchorTickets.force(level, owner, chunk, true),
                chunk -> AnchorTickets.force(level, owner, chunk, false))) {
            setDirty();
        }
    }

    /** Lebenszeichen eines bewegten Ankers, der seinen Chunk nicht gewechselt hat. */
    public void touchMoving(ServerLevel level, UUID owner) {
        Moving m = moving.get(owner);
        if (m != null) {
            m.lastSeen = level.getGameTime();
        }
    }

    public void removeMoving(ServerLevel level, UUID owner) {
        Moving m = moving.remove(owner);
        if (m != null) {
            for (LongIterator it = m.chunks.iterator(); it.hasNext(); ) {
                AnchorTickets.force(level, owner, it.nextLong(), false);
            }
            setDirty();
        }
    }

    public boolean knowsMoving(UUID owner) {
        return moving.containsKey(owner);
    }

    private void expireMoving(ServerLevel level, long time) {
        Iterator<Map.Entry<UUID, Moving>> it = moving.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Moving> e = it.next();
            Moving m = e.getValue();
            if (m.lastSeen < 0) {
                // Frisch aus dem Speicher geladen: Gnadenfrist, bis die Contraption wieder tickt.
                m.lastSeen = time;
            } else if (time - m.lastSeen > MOVING_TIMEOUT) {
                for (LongIterator ci = m.chunks.iterator(); ci.hasNext(); ) {
                    AnchorTickets.force(level, e.getKey(), ci.nextLong(), false);
                }
                it.remove();
                setDirty();
            }
        }
    }

    // ------------------------------------------------------------------ Hilfen

    public static LongOpenHashSet square(int cx, int cz, int radius) {
        LongOpenHashSet set = new LongOpenHashSet((2 * radius + 1) * (2 * radius + 1));
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int z = cz - radius; z <= cz + radius; z++) {
                set.add(ChunkPos.asLong(x, z));
            }
        }
        return set;
    }

    @FunctionalInterface
    private interface ChunkAction {
        void apply(long chunk);
    }

    /** Bringt {@code current} auf {@code wanted}. Gibt zurueck, ob sich etwas geaendert hat. */
    private static boolean applyDiff(LongOpenHashSet current, LongOpenHashSet wanted, ChunkAction add, ChunkAction remove) {
        boolean changed = false;
        // Erst neue Chunks setzen, dann alte loesen: so faellt ein Chunk, der in beiden liegt,
        // nie kurz aus dem geladenen Bereich.
        for (LongIterator it = wanted.iterator(); it.hasNext(); ) {
            long c = it.nextLong();
            if (current.add(c)) {
                add.apply(c);
                changed = true;
            }
        }
        for (LongIterator it = current.iterator(); it.hasNext(); ) {
            long c = it.nextLong();
            if (!wanted.contains(c)) {
                it.remove();
                remove.apply(c);
                changed = true;
            }
        }
        return changed;
    }

    // ------------------------------------------------------------------ Statistik (/brassanchor)

    public int staticCount() {
        return statics.size();
    }

    public int movingCount() {
        return moving.size();
    }

    public int sableCount() {
        return sableAnchors.size();
    }

    public int loadedChunkCount() {
        LongOpenHashSet all = new LongOpenHashSet();
        statics.values().forEach(all::addAll);
        moving.values().forEach(m -> all.addAll(m.chunks));
        return all.size();
    }

    public Set<BlockPos> staticPositions() {
        return statics.keySet();
    }

    // ------------------------------------------------------------------ Speichern

    private static AnchorManager load(CompoundTag tag, HolderLookup.Provider registries) {
        AnchorManager m = new AnchorManager();
        for (Tag t : tag.getList("Static", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            LongOpenHashSet chunks = new LongOpenHashSet(c.getLongArray("Chunks"));
            if (!chunks.isEmpty()) {
                m.statics.put(BlockPos.of(c.getLong("Pos")), chunks);
            }
        }
        for (Tag t : tag.getList("Moving", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Moving mv = new Moving();
            mv.chunks.addAll(new LongOpenHashSet(c.getLongArray("Chunks")));
            if (c.hasUUID("Id") && !mv.chunks.isEmpty()) {
                m.moving.put(c.getUUID("Id"), mv);
            }
        }
        return m;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag statics = new ListTag();
        this.statics.forEach((pos, chunks) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("Pos", pos.asLong());
            c.putLongArray("Chunks", chunks.toLongArray());
            statics.add(c);
        });
        tag.put("Static", statics);

        ListTag moving = new ListTag();
        this.moving.forEach((id, m) -> {
            CompoundTag c = new CompoundTag();
            c.putUUID("Id", id);
            c.putLongArray("Chunks", m.chunks.toLongArray());
            moving.add(c);
        });
        tag.put("Moving", moving);
        return tag;
    }
}
