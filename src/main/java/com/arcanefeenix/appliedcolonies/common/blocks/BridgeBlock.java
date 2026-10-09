package com.arcanefeenix.appliedcolonies.common.blocks;

import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.containers.BridgeMenu;
import com.arcanefeenix.appliedcolonies.common.setup.Registration;
import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.blocks.interfaces.IRSComponentBlock;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class BridgeBlock extends AbstractBlockHut<BridgeBlock> implements IRSComponentBlock {
    @Override
    public String getHutName() {
        return "bridge";
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        final TileEntityColonyBuilding building = Registration.BridgeBE.get().create(pos, state);
        building.registryName = this.getBuildingEntry().getRegistryName();
        return building;
    }

    @Override
    public BuildingEntry getBuildingEntry() {
        return Registration.BridgeBuildingEntry.get();
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos blockPos, Player player, InteractionHand hand, BlockHitResult ray) {
        if (level.isClientSide)
            return ItemInteractionResult.SUCCESS;

        openMenu(player, blockPos);

        return ItemInteractionResult.SUCCESS;
    }

    public void openMenu(Player player, BlockPos blockPos) {
        player.openMenu(new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new BridgeMenu(windowId, playerInventory, blockPos), Component.translatable("container.appliedcolonies.bridge")), (buf -> {
            buf.writeBlockPos(blockPos);
        }));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof BridgeBlockEntity bridge) {
                bridge.handleTick(lvl, st, type);
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof BridgeBlockEntity bridge) {
                if (!level.isClientSide && bridge.getMainNode().getNode() != null) {
                    bridge.getMainNode().destroy();
                    AppliedColonies.debug("Grid node destroyed on server side.");
                }
            }
        }

        // Always call super to let vanilla clean up
        super.onRemove(state, level, pos, newState, isMoving);
    }

}
