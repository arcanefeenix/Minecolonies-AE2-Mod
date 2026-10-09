package com.arcanefeenix.appliedcolonies.common.requestsystem;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.resolver.IRequestResolverFactory;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.constant.TypeConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class BridgeRequestResolverFactory implements IRequestResolverFactory<BridgeRequestResolver>
{
    private static final String NBT_TOKEN    = "Token";
    private static final String NBT_LOCATION = "Location";

    /**
     * Unique within the Minecolonies factory controller. Minecolonies' own IDs top out around 60;
     * use a value well outside that range to avoid collisions with the base game or other add-ons.
     */
    private static final short SERIALIZATION_ID = (short) 20001;

    @NotNull
    @Override
    public TypeToken<? extends BridgeRequestResolver> getFactoryOutputType()
    {
        return TypeToken.of(BridgeRequestResolver.class);
    }

    @NotNull
    @Override
    public TypeToken<? extends ILocation> getFactoryInputType()
    {
        return TypeConstants.ILOCATION;
    }

    @NotNull
    @Override
    public BridgeRequestResolver getNewInstance(
            @NotNull final IFactoryController factoryController,
            @NotNull final ILocation location,
            @NotNull final Object... context)
    {
        return new BridgeRequestResolver(location, factoryController.getNewInstance(TypeConstants.ITOKEN));
    }

    @NotNull
    @Override
    public CompoundTag serialize(
            @NotNull final HolderLookup.Provider provider,
            @NotNull final IFactoryController controller,
            @NotNull final BridgeRequestResolver resolver)
    {
        final CompoundTag tag = new CompoundTag();
        tag.put(NBT_TOKEN, controller.serializeTag(provider, resolver.getId()));
        tag.put(NBT_LOCATION, controller.serializeTag(provider, resolver.getLocation()));
        return tag;
    }

    @NotNull
    @Override
    public BridgeRequestResolver deserialize(
            @NotNull final HolderLookup.Provider provider,
            @NotNull final IFactoryController controller,
            @NotNull final CompoundTag tag)
    {
        final IToken<?> token = controller.deserializeTag(provider, tag.getCompound(NBT_TOKEN));
        final ILocation location = controller.deserializeTag(provider, tag.getCompound(NBT_LOCATION));
        return new BridgeRequestResolver(location, token);
    }

    @Override
    public void serialize(final IFactoryController controller, final BridgeRequestResolver input, final RegistryFriendlyByteBuf buffer)
    {
        controller.serialize(buffer, input.getId());
        controller.serialize(buffer, input.getLocation());
    }

    @Override
    public BridgeRequestResolver deserialize(final IFactoryController controller, final RegistryFriendlyByteBuf buffer) throws Throwable
    {
        final IToken<?> token = controller.deserialize(buffer);
        final ILocation location = controller.deserialize(buffer);
        return new BridgeRequestResolver(location, token);
    }

    @Override
    public short getSerializationId()
    {
        return SERIALIZATION_ID;
    }
}
