package io.github.blaublue1303.brassanchor;

import com.mojang.logging.LogUtils;
import com.simibubi.create.AllCreativeModeTabs;
import io.github.blaublue1303.brassanchor.command.AnchorCommand;
import io.github.blaublue1303.brassanchor.compat.create.CreateCompat;
import io.github.blaublue1303.brassanchor.compat.sable.SableCompat;
import io.github.blaublue1303.brassanchor.loading.AnchorManager;
import io.github.blaublue1303.brassanchor.loading.AnchorTickets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.slf4j.Logger;

/**
 * Brass Anchor: ein Chunk-Loader fuer Create.
 *
 * Grundprinzip fuer Performance: Ein stehender Anker hat keinen Block-Entity-Tick. Er setzt beim
 * Platzieren einmal NeoForge-Tickets (TicketController) und entfernt sie beim Abbauen wieder.
 * Nur Anker, die sich bewegen (Create-Contraptions, Zuege, Sable-Schiffe), werden nachgefuehrt,
 * und auch dann nur, wenn sie eine Chunk-Grenze ueberqueren.
 */
@Mod(BrassAnchor.MOD_ID)
public final class BrassAnchor {
    public static final String MOD_ID = "brassanchor";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static boolean sableLoaded;

    public BrassAnchor(IEventBus modBus, ModContainer container) {
        ModContent.register(modBus);
        modBus.addListener(AnchorTickets::register);
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::addCreativeTabItems);
        container.registerConfig(ModConfig.Type.SERVER, AnchorConfig.SPEC);

        sableLoaded = ModList.get().isLoaded("sable");
        if (sableLoaded) {
            // Der Ticket-Typ muss registriert sein, bevor Sable seine gespeicherten Tickets liest.
            SableCompat.init();
            LOGGER.info("Sable gefunden: Messing-Anker auf Sable-Schiffen werden unterstuetzt");
        }

        NeoForge.EVENT_BUS.addListener(this::onLevelTick);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isSableLoaded() {
        return sableLoaded;
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(CreateCompat::init);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (CreativeModeTabs.FUNCTIONAL_BLOCKS.equals(event.getTabKey())
                || AllCreativeModeTabs.BASE_CREATIVE_TAB.getKey().equals(event.getTabKey())) {
            event.accept(ModContent.BRASS_ANCHOR_ITEM.get());
        }
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            AnchorManager.get(level).tick(level);
        }
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        AnchorCommand.register(event.getDispatcher());
    }
}
