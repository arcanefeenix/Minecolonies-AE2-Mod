package com.arcanefeenix.appliedcolonies.common.addons.appliedenergistics;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridNodeListener;
import com.arcanefeenix.appliedcolonies.AppliedColonies;
import com.arcanefeenix.appliedcolonies.common.blockentities.BridgeBlockEntity;

public enum MeBridgeEntityListener implements IGridNodeListener<BridgeBlockEntity> {
    INSTANCE;

    @Override
    public void onSaveChanges(BridgeBlockEntity host, IGridNode node) {
        // Optional: persist grid node state
        AppliedColonies.debug("Saving changes");
    }

    @Override
    public void onInWorldConnectionChanged(BridgeBlockEntity nodeOwner, IGridNode node) {
        IGridNodeListener.super.onInWorldConnectionChanged(nodeOwner, node);
        AppliedColonies.debug("In world connection changed: " + node);
    }

    @Override
    public void onOwnerChanged(BridgeBlockEntity nodeOwner, IGridNode node) {
        IGridNodeListener.super.onOwnerChanged(nodeOwner, node);
        AppliedColonies.debug("Node owner changed");
    }

    @Override
    public void onGridChanged(BridgeBlockEntity nodeOwner, IGridNode node) {
        IGridNodeListener.super.onGridChanged(nodeOwner, node);
        AppliedColonies.debug("Grid changed: " + node);
//        System.out.println(node);
    }

    @Override
    public void onStateChanged(BridgeBlockEntity nodeOwner, IGridNode node, State state) {
        IGridNodeListener.super.onStateChanged(nodeOwner, node, state);
        AppliedColonies.debug("State changed: " + node);
    }
}

