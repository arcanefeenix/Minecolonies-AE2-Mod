package com.arcanefeenix.appliedcolonies.common.setup;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.client.render.BridgeBlockEntityRenderer;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.blocks.BridgeBlock;
import com.arcanefeenix.appliedcolonies.common.buildings.BridgeBuilding;
import com.arcanefeenix.appliedcolonies.common.containers.BridgeMenu;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.apiimp.CommonMinecoloniesAPIImpl;
import com.minecolonies.core.colony.buildings.views.EmptyView;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static com.arcanefeenix.appliedcolonies.AppliedColonies.MOD_ID;

public class Registration {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.createBlocks(AppliedColonies.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, AppliedColonies.MOD_ID);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(Registries.MENU, MOD_ID);
    public static final DeferredRegister<BuildingEntry> BUILDINGS = DeferredRegister.create(CommonMinecoloniesAPIImpl.BUILDINGS, AppliedColonies.MOD_ID);

    public static void init(IEventBus eventBus) {
        BLOCKS.register(eventBus);
        BLOCK_ENTITIES.register(eventBus);
        ITEMS.register(eventBus);
        CONTAINERS.register(eventBus);
        BUILDINGS.register(eventBus);
    }

    public static final DeferredHolder<Block, BridgeBlock> Bridge = BLOCKS.register("bridge", BridgeBlock::new);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BridgeBlockEntity>> BridgeBE = BLOCK_ENTITIES.register("bridge", () -> BlockEntityType.Builder.of(BridgeBlockEntity::new, Bridge.get()).build(null));

    public static final Supplier<Item> BridgeItem = ITEMS.register("bridge", () -> new BlockItem(Registration.Bridge.get(), new Item.Properties()));

    public static final DeferredHolder<MenuType<?>, MenuType<BridgeMenu>> BridgeMenuType = CONTAINERS.register("bridge_menu",
            () -> IMenuTypeExtension.create(BridgeMenu::new));

    public static final DeferredHolder<BuildingEntry, BuildingEntry> BridgeBuildingEntry = BUILDINGS.register("bridge",
                    () -> new BuildingEntry.Builder()
                            .setBuildingBlock(Registration.Bridge.get())
                            .setBuildingProducer(BridgeBuilding::new)
                            .setBuildingViewProducer(() -> EmptyView::new)
                            .setRegistryName(ResourceLocation.parse("appliedcolonies:bridge"))
                            .createBuildingEntry()
            );
}
