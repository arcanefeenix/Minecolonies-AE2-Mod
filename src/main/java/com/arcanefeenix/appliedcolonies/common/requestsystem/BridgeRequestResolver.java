package com.arcanefeenix.appliedcolonies.common.requestsystem;

import appeng.api.config.Actionable;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.MEStorage;
import appeng.api.storage.StorageHelper;
import appeng.me.helpers.MachineSource;
import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;
import com.arcanefeenix.appliedcolonies.common.configuration.ACConfig;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.manager.IRequestManager;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.request.RequestState;
import com.minecolonies.api.colony.requestsystem.requestable.IDeliverable;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.IItemHandlerCapProvider;
import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.constant.TypeConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.minecolonies.api.util.constant.RSConstants.CONST_BUILDING_RESOLVER_PRIORITY;

/**
 * Resolves builder item requests by pulling from the AE2 network (or the bridge's internal rack)
 * and inserting directly into the requesting building's inventory.
 */
public class BridgeRequestResolver implements IRequestResolver<IDeliverable>
{
    @NotNull private final ILocation location;
    @NotNull private final IToken<?> token;

    public BridgeRequestResolver(@NotNull final ILocation location, @NotNull final IToken<?> token)
    {
        this.location = location;
        this.token = token;
    }

    @Override
    public TypeToken<? extends IDeliverable> getRequestType()
    {
        return TypeConstants.DELIVERABLE;
    }

    @Override
    public IToken<?> getId()
    {
        return token;
    }

    @NotNull
    @Override
    public ILocation getLocation()
    {
        return location;
    }

    @Override
    public int getPriority()
    {
        // Higher than the warehouse (150) but lower than the requesting building's own self-resolve (200)
        return CONST_BUILDING_RESOLVER_PRIORITY - 25;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @NotNull
    @Override
    public MutableComponent getRequesterDisplayName(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request)
    {
        return Component.translatable("container.appliedcolonies.bridge");
    }

    @Override
    public boolean canResolveRequest(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {
        // Opt-out: when the server requires a courier, we leave resolution entirely to the vanilla
        // warehouse+courier chain and only use the bridge as a crafting trigger + pickup source.
        if (ACConfig.GENERAL_CONFIG.requireCourier.get())
        {
            return false;
        }

        if (request.getRequester().getLocation().equals(getLocation()))
        {
            return false;
        }

        final BridgeBlockEntity bridge = getBridge(manager);
        if (bridge == null)
        {
            AppliedColonies.warn("[Bridge] canResolveRequest: bridge BE not found at " + getLocation());
            return false;
        }

        final int minimum = request.getRequest().getMinimumCount();
        final int available = availableFor(bridge, request);
        final boolean can = available >= minimum;
        // Only log the positive case - the negative fires on every retry cycle for every open
        // request and would flood logs in a busy colony.
        if (can) {
            AppliedColonies.debug("[Bridge] canResolveRequest for " + request.getShortDisplayString().getString()
                    + " (need min " + minimum + ", have " + available + ") -> true");
        }
        return can;
    }

    @Nullable
    @Override
    public List<IToken<?>> attemptResolveRequest(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {
        final BridgeBlockEntity bridge = getBridge(manager);
        if (bridge == null)
        {
            return null;
        }

        if (availableFor(bridge, request) < request.getRequest().getMinimumCount())
        {
            return null;
        }

        return Lists.newArrayList();
    }

    @Override
    public void resolveRequest(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request)
    {
        final BridgeBlockEntity bridge = getBridge(manager);
        if (bridge == null)
        {
            AppliedColonies.warn("[Bridge] resolveRequest: bridge BE missing; marking RESOLVED empty");
            manager.updateRequestState(request.getId(), RequestState.RESOLVED);
            return;
        }

        final int wanted = request.getRequest().getCount();
        final List<ItemStack> delivered = deliverTo(bridge, manager, request, wanted);
        AppliedColonies.debug("[Bridge] resolveRequest delivered " + delivered.size()
                + " stacks to " + request.getRequester().getLocation());

        if (!delivered.isEmpty())
        {
            request.addDelivery(delivered);
        }
        manager.updateRequestState(request.getId(), RequestState.RESOLVED);
    }

    @Nullable
    @Override
    public List<IRequest<?>> getFollowupRequestForCompletion(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> completedRequest)
    {
        return null;
    }

    @Override
    public void onAssignedRequestBeingCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request) {}

    @Override
    public void onAssignedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<? extends IDeliverable> request) {}

    @Override
    public void onRequestedRequestComplete(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request) {}

    @Override
    public void onRequestedRequestCancelled(@NotNull final IRequestManager manager, @NotNull final IRequest<?> request) {}

    // ----- helpers -----

    @Nullable
    private BridgeBlockEntity getBridge(@NotNull final IRequestManager manager)
    {
        final Level world = manager.getColony().getWorld();
        if (world == null || world.isClientSide)
        {
            return null;
        }
        final BlockPos pos = location.getInDimensionLocation();
        final BlockEntity be = world.getBlockEntity(pos);
        return be instanceof BridgeBlockEntity bridge ? bridge : null;
    }

    /**
     * How many of the requested item can be delivered right now (rack + AE storage).
     */
    private int availableFor(@NotNull final BridgeBlockEntity bridge, @NotNull final IRequest<? extends IDeliverable> request)
    {
        int total = 0;

        final IItemHandler rack = bridge.getInventory();
        for (int i = 0; i < rack.getSlots(); i++)
        {
            final ItemStack s = rack.getStackInSlot(i);
            if (!s.isEmpty() && request.getRequest().matches(s))
            {
                total += s.getCount();
                if (total >= request.getRequest().getCount())
                {
                    return total;
                }
            }
        }

        final MEStorage storage = bridge.getMonitor();
        if (storage != null)
        {
            final ItemStack sample = pickSample(request);
            if (!sample.isEmpty())
            {
                final AEItemKey key = AEItemKey.of(sample);
                if (key != null)
                {
                    final int remaining = Math.max(0, request.getRequest().getCount() - total);
                    final long simulated = StorageHelper.poweredExtraction(
                            bridge.getEnergyService(),
                            storage,
                            key,
                            remaining,
                            new MachineSource(bridge),
                            Actionable.SIMULATE);
                    total += (int) Math.min(Integer.MAX_VALUE, simulated);
                }
            }
        }

        return total;
    }

    /**
     * Extract items (rack first, then AE) and insert them into the requester's building rack.
     *
     * @return the stacks that were actually delivered.
     */
    private List<ItemStack> deliverTo(
            @NotNull final BridgeBlockEntity bridge,
            @NotNull final IRequestManager manager,
            @NotNull final IRequest<? extends IDeliverable> request,
            final int wanted)
    {
        final List<ItemStack> delivered = new ArrayList<>();
        int remaining = wanted;

        final var requesterBuildingEntity = manager.getColony()
                .getServerBuildingManager()
                .getBuilding(request.getRequester().getLocation().getInDimensionLocation());
        if (requesterBuildingEntity == null)
        {
            AppliedColonies.warn("[Bridge] deliverTo: no building found at requester location " + request.getRequester().getLocation());
            return delivered;
        }
        final BlockEntity requesterTile = requesterBuildingEntity.getTileEntity();
        if (!(requesterTile instanceof IItemHandlerCapProvider requesterInv))
        {
            AppliedColonies.warn("[Bridge] deliverTo: requester TE not an IItemHandlerCapProvider: " + requesterTile);
            return delivered;
        }

        final IItemHandler rack = bridge.getInventory();
        for (int i = 0; i < rack.getSlots() && remaining > 0; i++)
        {
            final ItemStack s = rack.getStackInSlot(i);
            if (s.isEmpty() || !request.getRequest().matches(s))
            {
                continue;
            }
            final int takeCount = Math.min(remaining, s.getCount());
            final ItemStack extracted = rack.extractItem(i, takeCount, false);
            if (extracted.isEmpty())
            {
                continue;
            }
            final ItemStack leftover = InventoryUtils.addItemStackToProviderWithResult(requesterInv, extracted);
            final int placed = extracted.getCount() - leftover.getCount();
            if (placed > 0)
            {
                delivered.add(extracted.copyWithCount(placed));
                remaining -= placed;
            }
            if (!leftover.isEmpty())
            {
                // Couldn't fit; put the overflow back in the rack
                InventoryUtils.addItemStackToItemHandler(rack, leftover);
            }
        }

        if (remaining <= 0)
        {
            return delivered;
        }

        final MEStorage storage = bridge.getMonitor();
        final IEnergyService energy = bridge.getEnergyService();
        if (storage == null || energy == null)
        {
            return delivered;
        }

        final ItemStack sample = pickSample(request);
        if (sample.isEmpty())
        {
            return delivered;
        }
        final AEItemKey key = AEItemKey.of(sample);
        if (key == null)
        {
            return delivered;
        }

        final MachineSource source = new MachineSource(bridge);
        final long simulated = StorageHelper.poweredExtraction(
                energy, storage, key, remaining, source, Actionable.SIMULATE);
        if (simulated <= 0)
        {
            return delivered;
        }

        final int toExtract = (int) Math.min(remaining, simulated);
        final long extracted = StorageHelper.poweredExtraction(
                energy, storage, key, toExtract, source, Actionable.MODULATE);
        if (extracted <= 0)
        {
            return delivered;
        }

        final ItemStack toDeliver = key.toStack((int) extracted);
        final ItemStack leftover = InventoryUtils.addItemStackToProviderWithResult(requesterInv, toDeliver.copy());
        final int deliveredFromAe = (int) extracted - leftover.getCount();
        if (deliveredFromAe > 0)
        {
            delivered.add(key.toStack(deliveredFromAe));
        }

        // Anything that didn't fit in the requester goes back into AE so we don't leak items.
        if (!leftover.isEmpty())
        {
            storage.insert(key, leftover.getCount(), Actionable.MODULATE, source);
            AppliedColonies.debug("[Bridge] Returned " + leftover.getCount()
                    + "x " + leftover.getItem() + " to AE storage (requester had no room)");
        }

        return delivered;
    }

    private ItemStack pickSample(@NotNull final IRequest<? extends IDeliverable> request)
    {
        for (final ItemStack s : request.getDisplayStacks())
        {
            if (!s.isEmpty())
            {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (!(o instanceof BridgeRequestResolver other)) return false;
        return token.equals(other.token);
    }

    @Override
    public int hashCode()
    {
        return token.hashCode();
    }
}
