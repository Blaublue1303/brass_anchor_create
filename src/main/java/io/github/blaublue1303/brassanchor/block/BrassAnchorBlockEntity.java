package io.github.blaublue1303.brassanchor.block;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import io.github.blaublue1303.brassanchor.AnchorConfig;
import io.github.blaublue1303.brassanchor.ModContent;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Speichert nur den Radius. Tickt nie: Laden und Entladen erledigt der {@link AnchorManager}.
 */
public class BrassAnchorBlockEntity extends BlockEntity implements IHaveGoggleInformation {
    public static final String RADIUS_KEY = "Radius";

    private int radius = -1;
    private boolean limited;
    private boolean chunkUnloading;

    public BrassAnchorBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.BRASS_ANCHOR_BE.get(), pos, state);
    }

    public int getRadius() {
        if (radius < 0) {
            radius = AnchorConfig.defaultRadius();
        }
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
        changed();
    }

    public boolean isLimited() {
        return limited;
    }

    public void setLimited(boolean limited) {
        if (this.limited != limited) {
            this.limited = limited;
            changed();
        }
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    public static int sideLength(int radius) {
        return 2 * radius + 1;
    }

    // ------------------------------------------------------------------ Lebenszyklus

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            AnchorManager.get(server).requestUpdate(worldPosition);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        // Chunk wird nur entladen (Server stoppt o. ae.): Tickets bleiben gespeichert.
        chunkUnloading = true;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!chunkUnloading && level instanceof ServerLevel server) {
            // Abgebaut, ersetzt, von Create in eine Contraption gepackt oder von Sable verschoben.
            AnchorManager.get(server).remove(server, worldPosition);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        chunkUnloading = false;
    }

    // ------------------------------------------------------------------ Speichern / Sync

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(RADIUS_KEY, getRadius());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(RADIUS_KEY)) {
            radius = tag.getInt(RADIUS_KEY);
        }
        limited = tag.getBoolean("Limited");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(RADIUS_KEY, getRadius());
        tag.putBoolean("Limited", limited);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ------------------------------------------------------------------ Anzeige

    public MutableComponent statusLine() {
        int r = getRadius();
        if (!getBlockState().getValue(BrassAnchorBlock.ACTIVE)) {
            return Component.translatable("brassanchor.status.off").withStyle(ChatFormatting.RED);
        }
        if (limited) {
            return Component.translatable("brassanchor.status.limited").withStyle(ChatFormatting.RED);
        }
        return Component.translatable("brassanchor.status.on", sideLength(r), sideLength(r) * sideLength(r))
                .withStyle(ChatFormatting.GREEN);
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(Component.translatable("brassanchor.goggles.title")));
        tooltip.add(Component.literal("    ").append(statusLine()));
        tooltip.add(Component.literal("    ").append(Component.translatable("brassanchor.goggles.ticking")
                .withStyle(ChatFormatting.GRAY)));
        return true;
    }
}
