package com.arcanefeenix.appliedcolonies.common.blockentities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.*;
import appeng.api.networking.crafting.*;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IStorageService;
import appeng.api.orientation.BlockOrientation;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.api.util.AECableType;
import appeng.core.definitions.AEItems;
import appeng.helpers.MultiCraftingTracker;
import appeng.me.helpers.IGridConnectedBlockEntity;
import appeng.me.helpers.MachineSource;
import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.addons.appliedenergistics.MeBridgeEntityListener;
import com.arcanefeenix.appliedcolonies.common.configuration.ACConfig;
import com.arcanefeenix.appliedcolonies.common.setup.Registration;
import com.google.common.collect.ImmutableSet;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.resolver.player.IPlayerRequestResolver;
import com.minecolonies.api.colony.requestsystem.resolver.retrying.IRetryingRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.minecolonies.api.colony.requestsystem.requestable.deliveryman.AbstractDeliverymanRequestable.getPlayerActionPriority;

public class BridgeBlockEntity extends TileEntityColonyBuilding implements MenuProvider, IActionSource, IActionHost,
        IInWorldGridNodeHost, IGridConnectedBlockEntity, ICraftingSimulationRequester, ICraftingRequester, IUpgradeableObject {

    public final ItemStackHandler upgradeSlot = new ItemStackHandler(1) {
        @Override
        protected int getStackLimit(int slot, ItemStack stack) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(AEItems.CRAFTING_CARD.asItem());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };
    private float rotation;

    public boolean aeInitialized = false;
    private final IManagedGridNode mainNode = GridHelper.createManagedNode(this, MeBridgeEntityListener.INSTANCE);
    private boolean hasCraftingCard() {
        return !upgradeSlot.getStackInSlot(0).isEmpty() && upgradeSlot.getStackInSlot(0).is(AEItems.CRAFTING_CARD.asItem());
    }
    private final MultiCraftingTracker craftingTracker;
    private static final double IDLE_POWER = 1.0;  // AE/t while connected
    private static final List<Item> TOOL_PRIORITY = List.of(
            Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL,
            Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL,
            Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL,
            Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_SHOVEL,
            Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_SHOVEL
    );

    public boolean minecoloniesInitialized = false;
    private IColony mainColony = null;
    IRequestManager requestManager;
    // Tokens of open requests we've already asked AE to craft, to avoid re-triggering each tick.
    private final Set<IToken<?>> inProgressCrafts = new HashSet<>();
    // Courier mode: tokens we've already pulled items for and handed to a courier. Kept until the
    // underlying request leaves the open-request list (meaning the warehouse chain finally resolved
    // it), at which point retainAll in triggerPendingCrafts drops the entry.
    private final Set<IToken<?>> pulledForTokens = new HashSet<>();

    private static boolean upgradesRegistered = false;

    private static synchronized void registerUpgrades() {
        if (!upgradesRegistered) {
            Upgrades.add(AEItems.CRAFTING_CARD, Registration.Bridge.get(), 1);
            upgradesRegistered = true;
        }
    }

    public BridgeBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.BridgeBE.get(), pos, state);
        Set<Direction> sides = getGridConnectableSides(null);
        AppliedColonies.debug("Grid connectable sides: " + sides);
        mainNode.setExposedOnSides(sides);

        this.craftingTracker = new MultiCraftingTracker(this, 9);
        registerUpgrades();
    }

    public float getRenderingRotation() {
        rotation += 0.25F;
        if (rotation >= 360) {
            rotation = 0F;
        }
        return rotation;
    }

    public <T extends BlockEntity> void handleTick(Level level, BlockState state, BlockEntityType<T> type) {
        if (level != null && !level.isClientSide) {
            if (!aeInitialized) {
                mainNode.setFlags(GridFlags.REQUIRE_CHANNEL);
                mainNode.setIdlePowerUsage(IDLE_POWER);
                mainNode.setVisualRepresentation(new ItemStack(Registration.Bridge.get()));
                mainNode.setInWorldNode(true);
                mainNode.addService(ICraftingRequester.class, this);
                mainNode.create(level, getBlockPos());
                if (mainNode.getNode() == null) {
                    AppliedColonies.debug("Node creation failed!");
                } else {
                    aeInitialized = true;
                }
            }

            if (!minecoloniesInitialized) {
                mainColony = getMineColoniesColony();
                if (!(mainColony == null)) {
                    this.requestManager = mainColony.getRequestManager();
                    minecoloniesInitialized = true;
                }
            }

            if ((minecoloniesInitialized && mainColony != null) && (aeInitialized && mainNode.isOnline())) {
                if (level.getGameTime() % 20 == 0) { // Every 1 second
                    drainRackToNetwork();
                }
                if (level.getGameTime() % 100 == 0) { // Every 5 seconds
                    triggerPendingCrafts();
                }
            }
        }
    }

    public IColony getMineColoniesColony () {
        if (level instanceof ServerLevel) {
            MinecoloniesAPIProxy api = MinecoloniesAPIProxy.getInstance();
            return api.getColonyManager().getColonyByPosFromWorld(level, worldPosition);
        }
        return null;
    }

    /**
     * Scan for IDeliverable requests that no resolver (including ours) could take, and kick off AE
     * crafting jobs for them. Once the item exists in AE storage (or lands in our rack via
     * {@link #insertCraftedItems}), the resolver's next retry will pick the request up and deliver it.
     */
    private void triggerPendingCrafts() {
        final IPlayerRequestResolver playerResolver = requestManager.getPlayerResolver();
        final IRetryingRequestResolver retryingResolver = requestManager.getRetryingRequestResolver();

        final Set<IToken<?>> openTokens = new HashSet<>();
        openTokens.addAll(playerResolver.getAllAssignedRequests());
        openTokens.addAll(retryingResolver.getAllAssignedRequests());

        inProgressCrafts.retainAll(openTokens);
        pulledForTokens.retainAll(openTokens);

        final IGrid grid = mainNode.getGrid();
        if (grid == null) {
            return;
        }
        // Crafting card is only required to kick off AE autocrafting; pulls from existing storage
        // work without it. The card check now lives at the point of handleCrafting() in
        // tryTriggerCraft and pullOrCraft.
        final ICraftingService craftingService = grid.getService(ICraftingService.class);
        final MEStorage storage = grid.getStorageService().getInventory();
        final IEnergyService energy = grid.getEnergyService();
        if (craftingService == null || storage == null || energy == null) {
            return;
        }

        final boolean requireCourier = ACConfig.GENERAL_CONFIG.requireCourier.get();

        for (final IToken<?> token : openTokens) {
            final IRequest<?> request = requestManager.getRequestForToken(token);
            if (request == null || !(request.getRequest() instanceof IDeliverable deliverable)) {
                continue;
            }

            if (requireCourier) {
                // Already satisfied this request once; wait for the warehouse chain to actually
                // close it before pulling again (otherwise we'd loop every tick while the courier
                // is in transit).
                if (pulledForTokens.contains(token) || inProgressCrafts.contains(token)) {
                    continue;
                }
                // Courier mode: proactively pull available items from AE into the rack so the
                // NotifyingRackInventory fires a pickup request and the courier can collect them.
                // If the item isn't in AE but is craftable, kick off a craft.
                pullOrCraft(request, token, deliverable, craftingService, storage, energy);
                continue;
            }

            // Direct-delivery mode: nudge the resolver if we already have stock on hand, else craft.
            if (hasStockFor(request, deliverable, storage, energy)) {
                try {
                    requestManager.reassignRequest(token, Collections.emptySet());
                    AppliedColonies.debug("[Bridge] Nudged retrying request " + token
                            + " (" + request.getShortDisplayString().getString() + ") - stock on hand");
                } catch (Exception e) {
                    AppliedColonies.warn("[Bridge] Nudge failed for " + token + ": " + e);
                }
                continue;
            }

            if (inProgressCrafts.contains(token)) {
                continue;
            }
            tryTriggerCraft(request, token, deliverable, craftingService, storage, energy);
        }
    }

    /**
     * Courier-mode handler: if the requested item is available in AE storage, extract it and
     * deposit it into the bridge rack (which triggers a pickup request). Otherwise, if it's
     * craftable, schedule the craft and let {@link #insertCraftedItems} deposit the result.
     *
     * We don't check whether the rack already holds the item - if it does, a prior pickup request
     * is pending and we don't want to pull more.
     */
    private void pullOrCraft(
            final IRequest<?> request,
            final IToken<?> token,
            final IDeliverable deliverable,
            final ICraftingService craftingService,
            final MEStorage storage,
            final IEnergyService energy) {
        final int needed = deliverable.getCount();

        for (final ItemStack candidate : request.getDisplayStacks()) {
            if (candidate.isEmpty()) {
                continue;
            }
            final AEItemKey key = AEItemKey.of(candidate);
            if (key == null) {
                continue;
            }

            // Already waiting in the rack for a courier? Don't duplicate.
            if (rackHoldsAtLeast(key, 1)) {
                return;
            }

            final MachineSource source = new MachineSource(this);
            final long simulated = StorageHelper.poweredExtraction(energy, storage, key, needed, source, Actionable.SIMULATE);
            if (simulated > 0) {
                // Only pull as much as the rack can accept.
                final int wanted = (int) Math.min(needed, simulated);
                final int fits = wanted - insertIntoInventory(key.toStack(wanted), true);
                if (fits <= 0) {
                    return;
                }
                final long extracted = StorageHelper.poweredExtraction(energy, storage, key, fits, source, Actionable.MODULATE);
                if (extracted > 0) {
                    insertIntoInventory(key.toStack((int) extracted), false);
                    pulledForTokens.add(token);
                    AppliedColonies.debug("[Bridge] Pulled " + extracted + "x " + key
                            + " from AE into rack for courier pickup (request " + token + ")");
                }
                return;
            }

            if (inProgressCrafts.contains(token)) {
                return;
            }
            if (!craftingService.isCraftable(key)) {
                continue;
            }
            if (!hasCraftingCard()) {
                continue;
            }
            final int slot = inProgressCrafts.size() % 9;
            if (craftingTracker.handleCrafting(slot, key, needed, level, craftingService, source)) {
                inProgressCrafts.add(token);
                AppliedColonies.debug("[Bridge] Scheduled AE craft (courier mode) for " + key + " x " + needed);
                return;
            }
        }
    }

    private boolean rackHoldsAtLeast(final AEItemKey key, final int count) {
        int total = 0;
        for (int i = 0; i < inventory.getSlots(); i++) {
            final ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && key.matches(s)) {
                total += s.getCount();
                if (total >= count) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Does the bridge (rack + AE storage) hold at least the request's minimum right now?
     */
    private boolean hasStockFor(
            final IRequest<?> request,
            final IDeliverable deliverable,
            final MEStorage storage,
            final IEnergyService energy) {
        final int needed = deliverable.getMinimumCount();
        int have = 0;

        for (int i = 0; i < inventory.getSlots(); i++) {
            final ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && deliverable.matches(s)) {
                have += s.getCount();
                if (have >= needed) {
                    return true;
                }
            }
        }

        for (final ItemStack candidate : request.getDisplayStacks()) {
            if (candidate.isEmpty()) {
                continue;
            }
            final AEItemKey key = AEItemKey.of(candidate);
            if (key == null) {
                continue;
            }
            final long simulated = StorageHelper.poweredExtraction(
                    energy, storage, key, needed - have, new MachineSource(this), Actionable.SIMULATE);
            have += (int) Math.min(Integer.MAX_VALUE, simulated);
            if (have >= needed) {
                return true;
            }
        }
        return false;
    }

    private void tryTriggerCraft(
            final IRequest<?> request,
            final IToken<?> token,
            final IDeliverable deliverable,
            final ICraftingService craftingService,
            final MEStorage storage,
            final IEnergyService energy) {
        List<ItemStack> candidates = request.getDisplayStacks();

        final Optional<ItemStack> bestTool = getBestCraftableTool(candidates, craftingService);
        if (bestTool.isPresent()) {
            candidates = List.of(bestTool.get());
        }

        final int needed = deliverable.getCount();
        for (final ItemStack candidate : candidates) {
            final AEItemKey key = AEItemKey.of(candidate);
            if (key == null) {
                continue;
            }

            // If the item (or enough of it) is already available in AE, the resolver will handle it.
            final long present = storage.extract(key, needed, Actionable.SIMULATE, new MachineSource(this));
            if (present >= needed) {
                return;
            }

            if (!craftingService.isCraftable(key)) {
                continue;
            }
            if (!hasCraftingCard()) {
                // Craftable but we lack the card to initiate - skip quietly, player sees the empty
                // upgrade slot as the hint.
                continue;
            }

            final int slot = inProgressCrafts.size() % 9;
            if (craftingTracker.handleCrafting(slot, key, needed - present, level, craftingService, new MachineSource(this))) {
                inProgressCrafts.add(token);
                AppliedColonies.debug("[Bridge] Scheduled AE craft for " + key + " x " + (needed - present));
                return;
            }
        }
    }

    private Optional<ItemStack> getBestCraftableTool(List<ItemStack> requestedItems, ICraftingService craftingService) {
        Set<Item> requestedTools = requestedItems.stream().map(ItemStack::getItem).collect(Collectors.toSet());
        for (Item item : TOOL_PRIORITY) {
            if (requestedTools.contains(item)) {
                AEItemKey toolKey = AEItemKey.of(new ItemStack(item));
                if (toolKey != null && craftingService.isCraftable(toolKey)) {
                    return Optional.of(toolKey.toStack());
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Inserts into the internal rack inventory.
     *
     * @return the number of items that did not fit
     */
    private int insertIntoInventory(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < inventory.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = inventory.insertItem(slot, remaining, simulate);
        }
        return remaining.getCount();
    }

    @Override
    public ItemStackHandler createInventory(final int slots)
    {
        return new NotifyingRackInventory(slots);
    }

    /**
     * Rack subclass that fires a pickup request on every content change when
     * {@link ACConfig#GENERAL_CONFIG}.requireCourier is true, so couriers come collect the crafted
     * items and deliver them. When the config is false the resolver handles delivery directly and
     * no pickup request is needed.
     */
    public class NotifyingRackInventory extends RackInventory {
        public NotifyingRackInventory(final int defaultSize) {
            super(defaultSize);
        }

        @Override
        protected void onContentsChanged(final int slot) {
            super.onContentsChanged(slot);

            if (level == null || level.isClientSide) {
                return;
            }
            if (!ACConfig.GENERAL_CONFIG.requireCourier.get()) {
                return;
            }
            if (isEmpty()) {
                return;
            }
            if (!IColonyManager.getInstance().isCoordinateInAnyColony(level, worldPosition)) {
                return;
            }
            final IColony colony = IColonyManager.getInstance().getClosestColony(level, worldPosition);
            if (colony == null) {
                return;
            }
            final IBuilding building = colony.getServerBuildingManager().getBuilding(worldPosition);
            if (building == null) {
                return;
            }
            // createPickupRequest internally dedupes so firing on every change is safe.
            building.createPickupRequest(getPlayerActionPriority(true), false);
        }
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingTracker.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        if (!(what instanceof AEItemKey itemKey) || amount <= 0) {
            return 0;
        }

        // NetworkStorage.insert refuses re-entrant calls during an active crafting delivery
        // (its `mountsInUse` guard), so we always buffer the craft output in our rack and let
        // the next tick drain it into the ME network via drainRackToNetwork().
        ItemStack stack = itemKey.toStack((int) Math.min(amount, Integer.MAX_VALUE));
        int fits = stack.getCount() - insertIntoInventory(stack, true);
        if (fits <= 0) {
            return 0;
        }

        if (mode == Actionable.MODULATE) {
            insertIntoInventory(stack.copyWithCount(fits), false);
            if (ACConfig.GENERAL_CONFIG.requireCourier.get()) {
                // Courier mode: the resolver is disabled, so poking it does nothing. Instead move
                // the in-progress craft token into pulledForTokens so we don't immediately re-craft
                // after the courier picks the item up.
                markCraftsReadyForPickup(itemKey);
            } else {
                pokeMatchingRetries(itemKey);
            }
            playDeliveryEffect();
        }
        return fits;
    }

    /**
     * Courier-mode counterpart to {@link #pokeMatchingRetries}: when a craft lands, mark the
     * matching open requests as "pulled" so the trigger loop stops cycling them. The warehouse
     * chain will close the request once the courier delivers to the warehouse and the warehouse
     * resolver fulfils the builder's delivery.
     */
    private void markCraftsReadyForPickup(final AEItemKey itemKey) {
        if (requestManager == null || inProgressCrafts.isEmpty()) {
            return;
        }
        final ItemStack sample = itemKey.toStack();
        final List<IToken<?>> matched = new ArrayList<>();
        for (final IToken<?> token : inProgressCrafts) {
            final IRequest<?> request = requestManager.getRequestForToken(token);
            if (request == null || !(request.getRequest() instanceof IDeliverable deliverable)) {
                continue;
            }
            if (deliverable.matches(sample)) {
                matched.add(token);
            }
        }
        for (final IToken<?> token : matched) {
            inProgressCrafts.remove(token);
            pulledForTokens.add(token);
            AppliedColonies.debug("[Bridge] Craft landed; token " + token
                    + " now awaiting courier pickup");
        }
    }

    /**
     * Spawns a brief particle + sound burst at the crystal's position when a craft lands. The server
     * broadcasts particle packets to nearby clients automatically, so no manual BE sync is needed.
     */
    private void playDeliveryEffect() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        final double x = worldPosition.getX() + 0.5;
        final double y = worldPosition.getY() + 1.0;   // crystal centre
        final double z = worldPosition.getZ() + 0.5;

        // END_ROD = white drifting sparkles, reads as "magical teleport".
        serverLevel.sendParticles(ParticleTypes.END_ROD, x, y, z, 12, 0.15, 0.15, 0.15, 0.03);
        // A couple of FIREWORK dots for a sharper initial pop.
        serverLevel.sendParticles(ParticleTypes.FIREWORK, x, y, z, 6, 0.1, 0.1, 0.1, 0.02);
        // Amethyst chime is a quiet sample by design; stack 3 copies at slightly different pitches
        // so it reads as a richer chord rather than a single soft ding. Volume > 1 only widens the
        // audible range, not perceived loudness, so stacking is the only way to make it stand out.
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 1.2F);
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 1.5F);
        serverLevel.playSound(null, worldPosition, SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.BLOCKS, 1.0F, 1.8F);
    }

    /**
     * Push any items sitting in the rack into the ME network, leaving behind only what the
     * network couldn't accept. Runs outside crafting delivery so {@link appeng.me.storage.NetworkStorage}'s
     * re-entrancy guard isn't in effect.
     *
     * In courier mode the rack acts as a staging buffer for pickup, so we don't drain it back -
     * otherwise we'd race with pulls in {@link #pullOrCraft} and never let a courier pick anything up.
     */
    private void drainRackToNetwork() {
        if (ACConfig.GENERAL_CONFIG.requireCourier.get()) {
            return;
        }
        final MEStorage storage = getMonitor();
        if (storage == null) {
            return;
        }
        for (int i = 0; i < inventory.getSlots(); i++) {
            final ItemStack s = inventory.getStackInSlot(i);
            if (s.isEmpty()) {
                continue;
            }
            final AEItemKey key = AEItemKey.of(s);
            if (key == null) {
                continue;
            }
            final long accepted = storage.insert(key, s.getCount(), Actionable.MODULATE, this);
            if (accepted > 0) {
                inventory.extractItem(i, (int) Math.min(Integer.MAX_VALUE, accepted), false);
            }
        }
    }

    /**
     * For each token we were crafting for whose request matches the just-arrived item, ask the
     * manager to try to reassign it right now. If the resolver (ours) now has stock, it will take it.
     */
    private void pokeMatchingRetries(final AEItemKey itemKey) {
        if (requestManager == null || inProgressCrafts.isEmpty()) {
            return;
        }
        final ItemStack sample = itemKey.toStack();
        final List<IToken<?>> toRetry = new ArrayList<>();
        for (final IToken<?> token : inProgressCrafts) {
            final IRequest<?> request = requestManager.getRequestForToken(token);
            if (request == null || !(request.getRequest() instanceof IDeliverable deliverable)) {
                continue;
            }
            if (deliverable.matches(sample)) {
                toRetry.add(token);
            }
        }
        for (final IToken<?> token : toRetry) {
            try {
                requestManager.reassignRequest(token, Collections.emptySet());
                inProgressCrafts.remove(token);
                AppliedColonies.debug("[Bridge] Poked reassignment for " + token);
            } catch (Exception e) {
                AppliedColonies.warn("[Bridge] Reassign failed for " + token + ": " + e);
            }
        }
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        this.craftingTracker.jobStateChange(link);
    }

    public boolean hasPower(double amount) {
        return mainNode.getGrid() != null && mainNode.getGrid().getEnergyService().extractAEPower(amount, Actionable.SIMULATE, PowerMultiplier.ONE) == amount;
    }

    // NBT for Saving and Loading Inventory & State

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("upgradeSlot", upgradeSlot.serializeNBT(provider));
        tag.put("inventory", inventory.serializeNBT(provider));
        mainNode.saveToNBT(tag);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("upgradeSlot")) {
            upgradeSlot.deserializeNBT(provider, tag.getCompound("upgradeSlot"));
        }
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(provider, tag.getCompound("inventory"));
        }
        mainNode.loadFromNBT(tag);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.appliedcolonies.bridge");
    }

    @NotNull
    @Override
    public Optional<Player> player() {
        return Optional.empty();
    }

    @NotNull
    @Override
    public Optional<IActionHost> machine() {
        return Optional.of(this);
    }

    @NotNull
    @Override
    public <T> Optional<T> context(@NotNull Class<T> key) {
        return Optional.empty();
    }

    @Nullable
    @Override
    public IGridNode getActionableNode() {
        return mainNode.getNode();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        mainNode.destroy();
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        mainNode.destroy();
    }

    @Override
    public IManagedGridNode getMainNode() {
        return mainNode;
    }

    // Both IGridConnectedBlockEntity and ICraftingSimulationRequester declare getGridNode() as a
    // default; explicitly resolve the diamond here.
    @Override
    public @Nullable IGridNode getGridNode() {
        return mainNode.getNode();
    }

    @Override
    public Set<Direction> getGridConnectableSides(BlockOrientation orientation) {
        // Pedestal top is occupied by the crystal visual; only accept cables on the sides and
        // underneath. This also drives mainNode.setExposedOnSides() in the constructor.
        return EnumSet.of(Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @NotNull
    @Override
    public AECableType getCableConnectionType(@NotNull Direction dir) {
        return AECableType.SMART;
    }

    @Nullable
    @Override
    public IActionSource getActionSource() {
        return this;
    }

    /**
     * The ME network's storage view, or null if we're not connected to a grid yet.
     */
    @Nullable
    public MEStorage getMonitor() {
        final IGrid grid = mainNode.getGrid();
        return grid == null ? null : grid.getService(IStorageService.class).getInventory();
    }

    /**
     * The ME network's energy service, or null if we're not connected to a grid yet.
     */
    @Nullable
    public IEnergyService getEnergyService() {
        final IGrid grid = mainNode.getGrid();
        return grid == null ? null : grid.getEnergyService();
    }

    public boolean isPowered() {
        return mainNode.getNode() != null && mainNode.isPowered();
    }

    // NeoForge extension (IBlockEntityExtension); not an @Override from base BlockEntity.
    public AABB getRenderBoundingBox() {
        // The crystal hovers above the top of the block (and bobs); extend upward by a full
        // block so the renderer isn't culled when the camera looks at the pedestal from a
        // shallow angle.
        return new AABB(worldPosition).expandTowards(0, 1.0, 0);
    }
}
