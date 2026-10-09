package com.arcanefeenix.appliedcolonies.common.screens;

import com.arcanefeenix.appliedcolonies.common.containers.BridgeMenu;
import com.arcanefeenix.appliedcolonies.common.screens.basescreens.BaseBridgeScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BridgeScreen extends BaseBridgeScreen<BridgeMenu> {
    public BridgeScreen(BridgeMenu container, Inventory inv, Component name) {
        super(container, inv, name);
    }

    @Override
    public void init() {
        super.init();
    }
}
