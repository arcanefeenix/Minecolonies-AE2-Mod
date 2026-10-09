package com.arcanefeenix.appliedcolonies.common.configuration;

import net.minecraft.FieldsAreNonnullByDefault;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

@FieldsAreNonnullByDefault
public class GeneralConfig implements IACConfig {

    public final ModConfigSpec.BooleanValue enableDebugMode;
    public final ModConfigSpec.BooleanValue requireCourier;
    private final ModConfigSpec configSpec;

    GeneralConfig() {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Config to adjust general mod settings").push("General");

        enableDebugMode = builder.comment("Enables the debug mode. Only enable it if needed.").define("enableDebugMode", false);

        requireCourier = builder.comment(
                "If true (default), the Bridge requires a built warehouse with a hired courier to deliver items to requesters.",
                "Crafted items land in the bridge's rack and a pickup request is fired for a courier to deliver them, matching the vanilla Minecolonies delivery flow.",
                "If false, the bridge resolves builder requests directly and teleports items from the AE network into the requester's building rack with no courier required.").define("requireCourier", true);

        builder.pop();

        configSpec = builder.build();
    }


    @Override
    public ModConfigSpec getConfigSpec() {
        return configSpec;
    }

    @Override
    public String getFileName() {
        return "general";
    }

    @Override
    public ModConfig.Type getType() {
        return ModConfig.Type.COMMON;
    }
}
