package com.arcanefeenix.appliedcolonies.client.render;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders a hovering, slowly-rotating crystal above the pedestal. The crystal glows (full-bright,
 * pulsing) when the bridge is connected to a powered ME network and dims to a faint light otherwise.
 */
@OnlyIn(Dist.CLIENT)
public class BridgeBlockEntityRenderer implements BlockEntityRenderer<BridgeBlockEntity> {

    /**
     * Standalone model registered in {@code ClientSetup#registerAdditionalModels}. Must stay in sync
     * with the JSON file at {@code assets/appliedcolonies/models/block/crystal.json}.
     */
    public static final ModelResourceLocation CRYSTAL_MODEL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(AppliedColonies.MOD_ID, "block/crystal"));

    /** Rotation per in-game tick, degrees. */
    private static final float ROTATION_SPEED = 0.5F;
    /** Vertical bob amplitude, in block units (0.0625 = 1 pixel). */
    private static final float BOB_AMPLITUDE = 0.0625F;
    /** Vertical bob angular frequency, radians per game tick. */
    private static final float BOB_FREQUENCY = 0.1F;
    /** Glow pulse angular frequency, radians per game tick. */
    private static final float PULSE_FREQUENCY = 0.15F;
    /** Y-position of the crystal's centre above the block origin. Pedestal top is at 0.75; the
     *  scaled bipyramid has half-height ~0.16, so 1.0 leaves a visible ~1-pixel floating gap at
     *  rest and bob never clips through the cap. */
    private static final float HOVER_Y = 1.0F;

    public BridgeBlockEntityRenderer(BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(BridgeBlockEntity be, float partialTicks, PoseStack poseStack,
                       MultiBufferSource bufferSource, int combinedLight, int combinedOverlay) {
        final Minecraft minecraft = Minecraft.getInstance();
        final BakedModel crystal = minecraft.getModelManager().getModel(CRYSTAL_MODEL);
        final boolean powered = be.isPowered();

        final float gameTime = be.getLevel().getGameTime() + partialTicks;
        final float bob = (float) Math.sin(gameTime * BOB_FREQUENCY) * BOB_AMPLITUDE;

        poseStack.pushPose();
        poseStack.translate(0.5F, HOVER_Y + bob, 0.5F);

        if (powered) {
            poseStack.mulPose(Axis.YP.rotationDegrees((gameTime * ROTATION_SPEED) % 360F));
        }

        // Scale to 50% so the diamond prism sits like a hovering gem instead of a chunky block.
        // After scaling, shift so the model's own origin (model-space 0) aligns with the pose origin.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Base pass: solid crystal, shaded by surrounding light.
        drawCrystal(minecraft, poseStack, bufferSource.getBuffer(RenderType.cutout()),
                crystal, 1F, 1F, 1F, combinedLight, combinedOverlay);

        // Glow pass: additive-like overlay on top. Full-bright, with a pulsing intensity when powered
        // (gives the crystal a "breathing" glow) and a faint constant glow when not.
        final float pulse = powered
                ? 0.65F + 0.35F * (float) Math.sin(gameTime * PULSE_FREQUENCY)
                : 0.25F;
        drawCrystal(minecraft, poseStack, bufferSource.getBuffer(RenderType.tripwire()),
                crystal, pulse, pulse, pulse, LightTexture.FULL_BRIGHT, combinedOverlay);

        poseStack.popPose();
    }

    private static void drawCrystal(Minecraft minecraft, PoseStack poseStack, VertexConsumer buffer,
                                    BakedModel model, float r, float g, float b, int light, int overlay) {
        minecraft.getBlockRenderer().getModelRenderer().renderModel(
                poseStack.last(), buffer, null, model, r, g, b, light, overlay, ModelData.EMPTY, null);
    }
}
