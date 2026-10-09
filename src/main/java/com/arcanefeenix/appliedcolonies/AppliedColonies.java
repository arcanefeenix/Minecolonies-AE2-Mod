package com.arcanefeenix.appliedcolonies;

import appeng.api.AECapabilities;
import com.arcanefeenix.appliedcolonies.client.render.BridgeBlockEntityRenderer;
import com.arcanefeenix.appliedcolonies.common.addons.ACAddons;
import com.arcanefeenix.appliedcolonies.common.configuration.ACConfig;
import com.arcanefeenix.appliedcolonies.common.requestsystem.BridgeRequestResolverFactory;
import com.arcanefeenix.appliedcolonies.common.setup.Registration;
import com.arcanefeenix.appliedcolonies.common.setup.ClientSetup;
import com.minecolonies.api.colony.requestsystem.StandardFactoryController;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(AppliedColonies.MOD_ID)
public class AppliedColonies {
    public static final String MOD_ID = "appliedcolonies";
    private static final Logger LOGGER = LogManager.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public AppliedColonies(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("AppliedColonies welcomes you.");
        ACAddons.setup();

        ACConfig.registerConfigs(ModLoadingContext.get());

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);
        if (FMLLoader.getDist().isClient()) {
            modEventBus.addListener(ClientSetup::init);
        }

        Registration.init(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> StandardFactoryController.getInstance()
                .registerNewFactory(new BridgeRequestResolverFactory()));
    }

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                AECapabilities.IN_WORLD_GRID_NODE_HOST,
                Registration.BridgeBE.get(),
                (be, context) -> be);
    }

    public static void debug(String message) {
        if (ACConfig.GENERAL_CONFIG.enableDebugMode.get()) {
            LOGGER.debug("{}", message);
        }
    }

    public static void debug(String message, Level level) {
        if (ACConfig.GENERAL_CONFIG.enableDebugMode.get()) {
            LOGGER.log(level, "{}", message);
        }
    }

    /** Always logs at WARN. Use for actionable problems the player should see. */
    public static void warn(String message) {
        LOGGER.warn("{}", message);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {

    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {

    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }

        @SubscribeEvent
        public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerBlockEntityRenderer(
                    Registration.BridgeBE.get(),
                    BridgeBlockEntityRenderer::new
            );
        }
    }
}
