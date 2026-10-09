package com.arcanefeenix.appliedcolonies.common.addons;

import net.neoforged.fml.ModList;

public class ACAddons {

    public static final String AE2_MODID = "ae2";
    public static final String MINECOLONIES_MODID = "minecolonies";

    public static boolean ae2Loaded;
    public static boolean minecoloniesLoaded;

    public static void setup() {
        ModList modList = ModList.get();
        ae2Loaded = modList.isLoaded(AE2_MODID);
        minecoloniesLoaded = modList.isLoaded(MINECOLONIES_MODID);
    }
}
