package com.arcanefeenix.appliedcolonies.gametest;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.core.definitions.AEItems;
import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.containers.BridgeMenu;
import com.arcanefeenix.appliedcolonies.common.setup.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Run with {@code ./gradlew runGameTestServer}. All tests share the empty 5x5x5 template.
 */
@GameTestHolder(AppliedColonies.MOD_ID)
@PrefixGameTestTemplate(false)
public class BridgeGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos BRIDGE = new BlockPos(2, 1, 2);

    private static BridgeBlockEntity placeBridge(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, Registration.Bridge.get().defaultBlockState());
        if (!(helper.getBlockEntity(pos) instanceof BridgeBlockEntity be)) {
            helper.fail("Bridge block did not create a BridgeBlockEntity", pos);
            throw new IllegalStateException();
        }
        return be;
    }

    private static int countInRack(BridgeBlockEntity be, ItemStack of) {
        int total = 0;
        for (int i = 0; i < be.getInventory().getSlots(); i++) {
            ItemStack s = be.getInventory().getStackInSlot(i);
            if (ItemStack.isSameItemSameComponents(s, of)) {
                total += s.getCount();
            }
        }
        return total;
    }

    @GameTest(template = TEMPLATE)
    public static void bridgePlacesWithBlockEntity(GameTestHelper helper) {
        placeBridge(helper, BRIDGE);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unconnectedBridgeIsNotPowered(GameTestHelper helper) {
        BridgeBlockEntity be = placeBridge(helper, BRIDGE);
        helper.assertTrue(!be.isPowered(), "Bridge without an ME network must not report power");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftedItemsSimulateThenInsert(GameTestHelper helper) {
        BridgeBlockEntity be = placeBridge(helper, BRIDGE);
        AEItemKey iron = AEItemKey.of(Items.IRON_INGOT);
        ItemStack ironStack = new ItemStack(Items.IRON_INGOT);

        long simulated = be.insertCraftedItems(null, iron, 10, Actionable.SIMULATE);
        helper.assertTrue(simulated == 10, "Simulated insert should accept 10, got " + simulated);
        helper.assertTrue(countInRack(be, ironStack) == 0, "SIMULATE must not change the rack");

        long inserted = be.insertCraftedItems(null, iron, 10, Actionable.MODULATE);
        helper.assertTrue(inserted == 10, "Insert should accept 10, got " + inserted);
        helper.assertTrue(countInRack(be, ironStack) == 10, "Rack should hold 10 iron, has " + countInRack(be, ironStack));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void craftedItemsNeverExceedCapacity(GameTestHelper helper) {
        BridgeBlockEntity be = placeBridge(helper, BRIDGE);
        ItemStack ironStack = new ItemStack(Items.IRON_INGOT);

        long inserted = be.insertCraftedItems(null, AEItemKey.of(Items.IRON_INGOT), 1_000_000, Actionable.MODULATE);
        helper.assertTrue(inserted > 0 && inserted < 1_000_000, "Insert should be capped by capacity, got " + inserted);
        helper.assertTrue(countInRack(be, ironStack) == inserted,
                "Returned amount (" + inserted + ") must match rack contents (" + countInRack(be, ironStack) + ")");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void inventorySurvivesSaveAndLoad(GameTestHelper helper) {
        BridgeBlockEntity be = placeBridge(helper, BRIDGE);
        be.insertCraftedItems(null, AEItemKey.of(Items.DIAMOND), 7, Actionable.MODULATE);
        be.upgradeSlot.setStackInSlot(0, AEItems.CRAFTING_CARD.stack());

        CompoundTag saved = be.saveWithoutMetadata(helper.getLevel().registryAccess());

        BridgeBlockEntity other = placeBridge(helper, new BlockPos(0, 1, 0));
        other.loadWithComponents(saved, helper.getLevel().registryAccess());

        int diamonds = countInRack(other, new ItemStack(Items.DIAMOND));
        helper.assertTrue(diamonds == 7, "Loaded rack should hold 7 diamonds, has " + diamonds);
        helper.assertTrue(other.upgradeSlot.getStackInSlot(0).is(AEItems.CRAFTING_CARD.asItem()), "Upgrade slot should survive reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shiftClickDoesNotCrash(GameTestHelper helper) {
        placeBridge(helper, BRIDGE);
        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        player.getInventory().setItem(0, new ItemStack(Items.COBBLESTONE, 16));

        BridgeMenu menu = new BridgeMenu(1, player.getInventory(), helper.absolutePos(BRIDGE));

        // Every slot index must be safe to shift-click, including empty ones, and never return null.
        for (int i = 0; i < menu.slots.size(); i++) {
            helper.assertTrue(menu.quickMoveStack(player, i) != null, "quickMoveStack returned null for slot " + i);
        }
        helper.succeed();
    }
}
