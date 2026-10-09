package com.arcanefeenix.appliedcolonies.common.setup;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.client.render.BridgeBlockEntityRenderer;
import com.arcanefeenix.appliedcolonies.common.screens.BridgeScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = AppliedColonies.MOD_ID, value = Dist.CLIENT)
public class ClientSetup {
    public static void init(final FMLClientSetupEvent event) {

    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Registration.BridgeMenuType.get(), BridgeScreen::new);
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        // The crystal is rendered dynamically by the BER, not via a blockstate, so Minecraft
        // won't discover it unless we register it here.
        event.register(BridgeBlockEntityRenderer.CRYSTAL_MODEL);
    }
}
