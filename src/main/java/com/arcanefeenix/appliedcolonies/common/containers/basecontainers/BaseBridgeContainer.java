package com.arcanefeenix.appliedcolonies.common.containers.basecontainers;

import appeng.blockentity.AEBaseBlockEntity;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.minecolonies.api.tileentities.AbstractTileEntityRack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class BaseBridgeContainer extends BaseContainer {
    public BridgeBlockEntity bridgeBlockEntity;
    protected Player player;
    protected BlockPos blockPos;
    public ContainerData data;

    public BaseBridgeContainer(@Nullable MenuType<?> menuType, int windowId, Inventory playerInventory, BlockPos blockPos) {
        super(menuType, windowId);
        this.blockPos = blockPos;
        this.player = playerInventory.player;
        BlockEntity blockEntity = player.level().getBlockEntity(blockPos);

        if (blockEntity instanceof BridgeBlockEntity bridgeBlockEntity) {
            this.bridgeBlockEntity = bridgeBlockEntity;
        }
    }

    public BaseBridgeContainer(@Nullable MenuType<?> p_i50105_1_, int p_i50105_2_) {
        super(p_i50105_1_, p_i50105_2_);
    }


//    @Override
//    public ItemStack quickMoveStack(Player playerIn, int index) {
//        ItemStack itemstack = ItemStack.EMPTY;
//        if (UPGRADE_SLOTS > 0) {
//            Slot slot = this.slots.get(index);
//            if (slot.hasItem()) {
//                ItemStack currentStack = slot.getItem();
//                if (index < UPGRADE_SLOTS) { //Machine Slots to Player Inventory
//                    if (!this.moveItemStackTo(currentStack, UPGRADE_SLOTS, UPGRADE_SLOTS + Inventory.INVENTORY_SIZE, true)) {
//                        return ItemStack.EMPTY;
//                    }
//                }
//                if (index >= UPGRADE_SLOTS) { //Player Inventory to Machine Slots
//                    if (!this.moveItemStackTo(currentStack, 0, UPGRADE_SLOTS, false)) {
//                        //No-Op, pass along to Filter Handler below
//                    }
//                }
//
//                if (currentStack.isEmpty()) {
//                    slot.set(ItemStack.EMPTY);
//                } else {
//                    slot.setChanged();
//                }
//
//                if (currentStack.getCount() == itemstack.getCount()) {
//                    return ItemStack.EMPTY;
//                }
//
//                slot.onTake(playerIn, currentStack);
//            }
//        }
//        return itemstack;
//    }
}
