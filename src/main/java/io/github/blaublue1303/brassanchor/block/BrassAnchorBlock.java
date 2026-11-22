package io.github.blaublue1303.brassanchor.block;

import com.mojang.serialization.MapCodec;
import io.github.blaublue1303.brassanchor.AnchorConfig;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Der Messing-Anker.
 *
 * Rechtsklick: Radius durchschalten. Schleichen + Rechtsklick: Status anzeigen.
 * Ein Redstone-Signal schaltet den Anker ab (z. B. um eine Farm nur bei Bedarf laufen zu lassen).
 */
public class BrassAnchorBlock extends BaseEntityBlock {
    public static final MapCodec<BrassAnchorBlock> CODEC = simpleCodec(BrassAnchorBlock::new);
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public BrassAnchorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ACTIVE, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrassAnchorBlockEntity(pos, state);
    }

    // Absichtlich kein getTicker(): ein stehender Anker kostet pro Tick nichts.

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) {
            return;
        }
        boolean active = !level.hasNeighborSignal(pos);
        if (active != state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, active), Block.UPDATE_CLIENTS);
            AnchorManager.get((ServerLevel) level).requestUpdate(pos);
            level.playSound(null, pos, active ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS, 0.4F, 1.4F);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BrassAnchorBlockEntity anchor)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown()) {
            player.displayClientMessage(anchor.statusLine(), true);
            return InteractionResult.SUCCESS;
        }
        int max = AnchorConfig.maxRadius();
        int next = anchor.getRadius() >= max ? 0 : anchor.getRadius() + 1;
        anchor.setRadius(next);
        AnchorManager.get((ServerLevel) level).requestUpdate(pos);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.6F + next * 0.15F);
        player.displayClientMessage(Component.translatable("brassanchor.message.radius",
                BrassAnchorBlockEntity.sideLength(next), next).withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.3, 0.2, (random.nextDouble() - 0.5) * 0.3);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("brassanchor.tooltip.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("brassanchor.tooltip.2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
