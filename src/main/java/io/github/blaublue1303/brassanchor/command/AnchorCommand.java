package io.github.blaublue1303.brassanchor.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

/** /brassanchor list: zeigt pro Dimension, wie viele Anker und Chunks aktiv sind (OP-Level 2). */
public final class AnchorCommand {
    private AnchorCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("brassanchor")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("list").executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    int total = 0;
                    for (ServerLevel level : src.getServer().getAllLevels()) {
                        AnchorManager m = AnchorManager.get(level);
                        int anchors = m.staticCount() + m.sableCount() + m.movingCount();
                        if (anchors == 0) {
                            continue;
                        }
                        total += anchors;
                        src.sendSuccess(() -> Component.translatable("brassanchor.command.level",
                                level.dimension().location().toString(), m.staticCount(), m.movingCount(),
                                m.sableCount(), m.loadedChunkCount()).withStyle(ChatFormatting.GOLD), false);
                        int shown = 0;
                        for (BlockPos pos : m.staticPositions()) {
                            if (shown++ >= 20) {
                                src.sendSuccess(() -> Component.literal("  ..."), false);
                                break;
                            }
                            src.sendSuccess(() -> Component.literal("  " + pos.toShortString()).withStyle(ChatFormatting.GRAY), false);
                        }
                    }
                    if (total == 0) {
                        src.sendSuccess(() -> Component.translatable("brassanchor.command.none"), false);
                    }
                    return total;
                })));
    }
}
