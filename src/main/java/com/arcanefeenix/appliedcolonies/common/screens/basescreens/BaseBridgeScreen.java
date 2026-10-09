package com.arcanefeenix.appliedcolonies.common.screens.basescreens;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.containers.basecontainers.BaseBridgeContainer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public abstract class BaseBridgeScreen<T extends BaseBridgeContainer> extends BaseScreen<T> {
    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(AppliedColonies.MOD_ID, "textures/gui/ae_bridge.png");
    private static final ResourceLocation CRAFTING_CARD_SLOT =
            ResourceLocation.fromNamespaceAndPath("ae2", "textures/item/card_crafting.png");

    protected final BaseBridgeContainer container;
    protected BridgeBlockEntity bridgeBlockEntity;

    public BaseBridgeScreen(T container, Inventory pPlayerInventory, Component pTitle) {
        super(container, pPlayerInventory, pTitle);
        this.container = container;
        this.bridgeBlockEntity = container.bridgeBlockEntity;
    }

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (bridgeBlockEntity != null) {
            Component title = bridgeBlockEntity.getBlockState().getBlock().getName();
            guiGraphics.drawString(this.font, title, titleLabelX, titleLabelY, 4210752, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (isMouseOverCraftingCardSlot(mouseX, mouseY)) {
            guiGraphics.renderTooltip(font, Component.translatable("tooltip.appliedcolonies.bridge.crafting_card_slot"), mouseX, mouseY);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.setShaderTexture(0, BACKGROUND);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(BACKGROUND, x, y, 0, 0, imageWidth, imageHeight);

        // Draw faded crafting card icon
        RenderSystem.enableBlend();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 0.4f); // 40% opacity
        guiGraphics.blit(CRAFTING_CARD_SLOT, leftPos + 152, topPos + 5, 0, 0, 16, 16, 16, 16);
        RenderSystem.disableBlend();
        guiGraphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private boolean isMouseOverCraftingCardSlot(int mouseX, int mouseY) {
        return mouseX >= leftPos + 152 && mouseX <= leftPos + 168 && mouseY >= topPos + 5 && mouseY <= topPos + 21;
    }
}
