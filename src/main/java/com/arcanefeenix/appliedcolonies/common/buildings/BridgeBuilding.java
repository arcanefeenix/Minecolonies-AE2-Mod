package com.arcanefeenix.appliedcolonies.common.buildings;

import com.arcanefeenix.appliedcolonies.common.requestsystem.BridgeRequestResolver;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.ldtteam.structurize.api.RotationMirror;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.buildings.IRSComponent;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolver;
import com.minecolonies.api.util.constant.TypeConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import net.minecraft.core.BlockPos;

public class BridgeBuilding extends AbstractBuilding implements IRSComponent {
    public BridgeBuilding(IColony c, BlockPos l) {
        super(c, l);
    }

    public String getSchematicName() {
        return "bridge";
    }

    @Override
    public ImmutableCollection<IRequestResolver<?>> createResolvers() {
        final ImmutableList.Builder<IRequestResolver<?>> builder = ImmutableList.builder();
        builder.addAll(super.createResolvers());
        final BridgeRequestResolver resolver = new BridgeRequestResolver(
                getRequester().getLocation(),
                getColony().getRequestManager().getFactoryController().getNewInstance(TypeConstants.ITOKEN));
        builder.add(resolver);
        com.arcanefeenix.appliedcolonies.AppliedColonies.debug(
                "[Bridge] createResolvers() produced resolver " + resolver.getId()
                        + " at " + getRequester().getLocation());
        return builder.build();
    }

    public int getMaxBuildingLevel() {
        return 0;
    }

    public RotationMirror getRotationMirror() {
        return RotationMirror.NONE;
    }
}
