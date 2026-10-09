package com.arcanefeenix.appliedcolonies.common.containers;

import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.containers.basecontainers.BaseBridgeContainer;
import com.arcanefeenix.appliedcolonies.common.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public class BridgeMenu extends BaseBridgeContainer {
    public final BridgeBlockEntity bridgeBlockEntity;
    private final Level level;

    public BridgeMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, extraData.readBlockPos());
    }

    public BridgeMenu(int windowId, Inventory playerInventory, BlockPos blockPos) {
        super(Registration.BridgeMenuType.get(), windowId, playerInventory, blockPos);
        this.level = playerInventory.player.level();
        if (!(this.level.getBlockEntity(blockPos) instanceof BridgeBlockEntity be)) {
            throw new IllegalStateException("No BridgeBlockEntity at " + blockPos);
        }
        this.bridgeBlockEntity = be;
        addPlayerSlots(player.getInventory());

        addSlotBox(bridgeBlockEntity.getInventory(), 0, 8, 26, 9, 18, 3, 18);

        addUpgradeSlot();
    }

    public void addUpgradeSlot() {
        ItemStackHandler upgradeHandler = bridgeBlockEntity.upgradeSlot;
        addSlotRange(upgradeHandler, 0, 152, 5, 1, 18);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(player.level(), blockPos), player, Registration.Bridge.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        final int playerSlots = 36;
        final int totalSlots = slots.size();
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack current = slot.getItem();
        ItemStack original = current.copy();

        if (index < playerSlots) {
            // Player inventory -> upgrade slot (last) first, then the rack slots.
            if (!moveItemStackTo(current, totalSlots - 1, totalSlots, false)
                    && !moveItemStackTo(current, playerSlots, totalSlots - 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(current, 0, playerSlots, true)) {
            return ItemStack.EMPTY;
        }

        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (current.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        slot.onTake(player, current);
        return original;
    }

    @Override
    public void removed(Player playerIn) {
        super.removed(playerIn);
    }
}
