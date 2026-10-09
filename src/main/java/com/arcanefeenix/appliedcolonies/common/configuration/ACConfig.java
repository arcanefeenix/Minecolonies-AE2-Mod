package com.arcanefeenix.appliedcolonies.common.configuration;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.HashMap;
import java.util.Map;

public class ACConfig {

    private static final Map<IConfigSpec, IACConfig> KNOWN_CONFIGS = new HashMap<>();

    public static final GeneralConfig GENERAL_CONFIG = new GeneralConfig();

    private ACConfig() {
    }

    public static void registerConfigs(ModLoadingContext context) {
        //Creates the config folder
        FMLPaths.getOrCreateGameRelativePath(FMLPaths.CONFIGDIR.get().resolve("AppliedColonies"));

        ModContainer modContainer = context.getActiveContainer();
        ACConfigHelper.registerConfig(KNOWN_CONFIGS, modContainer, GENERAL_CONFIG);
    }

    public static class ACConfigHelper {
        public static String getAPConfigFilePath(IACConfig config) {
            return "AppliedColonies/" + config.getFileName() + ".toml";
        }

        public static void registerConfig(Map<IConfigSpec, IACConfig> knownConfigs, ModContainer modContainer, IACConfig config) {
            modContainer.registerConfig(config.getType(), config.getConfigSpec(), ACConfigHelper.getAPConfigFilePath(config));
            knownConfigs.put(config.getConfigSpec(), config);
        }
    }
}