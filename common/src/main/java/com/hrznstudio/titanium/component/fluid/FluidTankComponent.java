/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.component.fluid;

import com.google.common.collect.Lists;
import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.transfer.IFluidStorage;
import com.hrznstudio.titanium.api.transfer.IFluidTank;
import com.hrznstudio.titanium.fluid.TitaniumFluidStack;
import com.hrznstudio.titanium.util.IValueIOSerializable;
import com.hrznstudio.titanium.api.client.AssetTypes;
import com.hrznstudio.titanium.api.client.IAssetType;
import com.hrznstudio.titanium.api.client.IScreenAddonProvider;
import com.hrznstudio.titanium.api.client.assets.types.ITankAsset;
import com.hrznstudio.titanium.client.screen.addon.TankScreenAddon;
import com.hrznstudio.titanium.component.IComponentHarness;
import com.hrznstudio.titanium.container.addon.IContainerAddon;
import com.hrznstudio.titanium.container.addon.IContainerAddonProvider;
import com.hrznstudio.titanium.container.addon.IntArrayReferenceHolderAddon;
import com.hrznstudio.titanium.container.referenceholder.FluidTankReferenceHolder;
import com.hrznstudio.titanium.nbthandler.INBTSerializable;
import com.hrznstudio.titanium.util.ValueIOSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class FluidTankComponent<T extends IComponentHarness> implements IFluidTank, IFluidStorage, IValueIOSerializable, IScreenAddonProvider,
        IContainerAddonProvider, INBTSerializable<CompoundTag> {

    private final int posX;
    private final int posY;
    private String name;
    private T componentHarness;
    private Type tankType;
    private Action tankAction;
    private Runnable onContentChange;
    private int capacity;
    private TitaniumFluidStack fluid;
    private Predicate<TitaniumFluidStack> validator;

    public FluidTankComponent(String name, int amount, int posX, int posY) {
        this.capacity = amount;
        this.fluid = TitaniumFluidStack.EMPTY;
        this.validator = stack -> true;
        this.posX = posX;
        this.posY = posY;
        this.name = name;
        this.tankType = Type.NORMAL;
        this.tankAction = Action.BOTH;
        this.onContentChange = () -> {
        };
    }

    /**
     * Sets the tile to be automatically marked dirty when the contents change
     *
     * @param componentHarness The tile where the tank is
     * @return itself
     */
    public FluidTankComponent<T> setComponentHarness(T componentHarness) {
        this.componentHarness = componentHarness;
        return this;
    }

    public T getComponentHarness() {
        return componentHarness;
    }

    @Override
    public void onContentsChanged() {
        if (componentHarness != null) {
            componentHarness.markComponentForUpdate(true);
        }
        onContentChange.run();
    }

    public String getName() {
        return name;
    }

    public int getPosX() {
        return posX;
    }

    public int getPosY() {
        return posY;
    }

    public Type getTankType() {
        return tankType;
    }

    public FluidTankComponent<T> setTankType(Type tankType) {
        this.tankType = tankType;
        return this;
    }

    public FluidTankComponent<T> setOnContentChange(Runnable onContentChange) {
        this.onContentChange = onContentChange;
        return this;
    }

    public Action getTankAction() {
        return tankAction;
    }

    public FluidTankComponent<T> setTankAction(Action tankAction) {
        this.tankAction = tankAction;
        return this;
    }

    @Override
    public TitaniumFluidStack getFluid() {
        return fluid;
    }

    @Override
    public void setFluid(TitaniumFluidStack stack) {
        this.fluid = stack;
    }

    /**
     * Sets the contents of the tank, ignoring filters, and notifies the change
     */
    public void setFluidStack(TitaniumFluidStack fluidStack) {
        this.fluid = fluidStack;
        onContentsChanged();
    }

    @Override
    public int getFluidAmount() {
        return fluid.getAmount();
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    public FluidTankComponent<T> setCapacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    /**
     * Sets the predicate that checks which fluids are valid for the tank
     */
    public FluidTankComponent<T> setValidator(Predicate<TitaniumFluidStack> validator) {
        this.validator = validator;
        return this;
    }

    public boolean isFluidValid(TitaniumFluidStack stack) {
        return !stack.isEmpty() && validator.test(stack);
    }

    @Override
    public boolean canFill(TitaniumFluidStack stack) {
        return getTankAction().canFill() && isFluidValid(stack);
    }

    @Override
    public boolean canDrain(TitaniumFluidStack stack) {
        return getTankAction().canDrain();
    }

    /**
     * Fills the tank ignoring the {@link Action} of the tank, used by the owner of the tank
     */
    public int fillForced(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !isFluidValid(resource)) return 0;
        if (!fluid.isEmpty() && !TitaniumFluidStack.isSameFluidSameComponents(fluid, resource)) return 0;
        int filled = Math.min(capacity - fluid.getAmount(), resource.getAmount());
        if (filled <= 0) return 0;
        if (!simulate) {
            this.fluid = fluid.isEmpty() ? resource.copyWithAmount(filled) : fluid.grow(filled);
            onContentsChanged();
        }
        return filled;
    }

    /**
     * Drains the tank ignoring the {@link Action} of the tank, used by the owner of the tank
     */
    public TitaniumFluidStack drainForced(int maxDrain, boolean simulate) {
        if (maxDrain <= 0 || fluid.isEmpty()) return TitaniumFluidStack.EMPTY;
        int drained = Math.min(maxDrain, fluid.getAmount());
        TitaniumFluidStack result = fluid.copyWithAmount(drained);
        if (!simulate) {
            this.fluid = fluid.shrink(drained);
            onContentsChanged();
        }
        return result;
    }

    /**
     * Drains the given fluid ignoring the {@link Action} of the tank, used by the owner of the tank
     */
    public TitaniumFluidStack drainForced(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !TitaniumFluidStack.isSameFluidSameComponents(resource, fluid)) return TitaniumFluidStack.EMPTY;
        return drainForced(resource.getAmount(), simulate);
    }

    @Override
    public int fill(TitaniumFluidStack resource, boolean simulate) {
        return IFluidTank.super.fill(resource, simulate);
    }

    @Override
    public TitaniumFluidStack drain(TitaniumFluidStack resource, boolean simulate) {
        return IFluidTank.super.drain(resource, simulate);
    }

    @Override
    public TitaniumFluidStack drain(int maxDrain, boolean simulate) {
        return IFluidTank.super.drain(maxDrain, simulate);
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public IFluidTank getTank(int tank) {
        return this;
    }

    /**
     * Same key as NeoForge's StacksResourceHandler so saved data stays compatible
     */
    @Override
    public void serialize(ValueOutput output) {
        output.store("stacks", TitaniumFluidStack.OPTIONAL_CODEC.listOf(), List.of(fluid));
    }

    @Override
    public void deserialize(ValueInput input) {
        input.read("stacks", TitaniumFluidStack.OPTIONAL_CODEC.listOf()).ifPresent(list -> this.fluid = list.isEmpty() ? TitaniumFluidStack.EMPTY : list.get(0));
    }

    @Override
    public List<IFactory<?>> getScreenAddons() {
        List<IFactory<?>> addons = new ArrayList<>();
        addons.add(() -> new TankScreenAddon(posX, posY, this, tankType));
        return addons;
    }

    @Override
    public List<IFactory<? extends IContainerAddon>> getContainerAddons() {
        return Lists.newArrayList(
                () -> new IntArrayReferenceHolderAddon(new FluidTankReferenceHolder(this))
        );
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return ValueIOSerialization.save(provider, this);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        ValueIOSerialization.load(provider, nbt, this);
    }

    public enum Type {
        NORMAL(AssetTypes.TANK_NORMAL),
        SMALL(AssetTypes.TANK_SMALL);

        private final IAssetType<ITankAsset> assetType;

        Type(IAssetType<ITankAsset> assetType) {
            this.assetType = assetType;
        }

        public IAssetType<ITankAsset> getAssetType() {
            return assetType;
        }
    }

    public enum Action {
        FILL(true, false),
        DRAIN(false, true),
        BOTH(true, true),
        NONE(false, false);

        private final boolean fill;
        private final boolean drain;

        Action(boolean fill, boolean drain) {
            this.fill = fill;
            this.drain = drain;
        }

        public boolean canFill() {
            return fill;
        }

        public boolean canDrain() {
            return drain;
        }
    }
}
