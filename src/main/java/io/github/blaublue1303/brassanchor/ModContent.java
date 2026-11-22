package io.github.blaublue1303.brassanchor;

import io.github.blaublue1303.brassanchor.block.BrassAnchorBlock;
import io.github.blaublue1303.brassanchor.block.BrassAnchorBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(BrassAnchor.MOD_ID);
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BrassAnchor.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, BrassAnchor.MOD_ID);

    public static final DeferredBlock<BrassAnchorBlock> BRASS_ANCHOR = BLOCKS.register("brass_anchor",
            () -> new BrassAnchorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.TERRACOTTA_YELLOW)
                    .strength(5.0F, 1200.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.NETHERITE_BLOCK)
                    .lightLevel(state -> state.getValue(BrassAnchorBlock.ACTIVE) ? 7 : 0)));

    public static final DeferredItem<BlockItem> BRASS_ANCHOR_ITEM = ITEMS.register("brass_anchor",
            () -> new BlockItem(BRASS_ANCHOR.get(), new Item.Properties().rarity(Rarity.UNCOMMON)));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrassAnchorBlockEntity>> BRASS_ANCHOR_BE =
            BLOCK_ENTITIES.register("brass_anchor",
                    () -> BlockEntityType.Builder.of(BrassAnchorBlockEntity::new, BRASS_ANCHOR.get()).build(null));

    private ModContent() {}

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
