/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.nbthandler.data;

import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.hrznstudio.titanium.api.INBTHandler;
import com.hrznstudio.titanium.util.ValueIOSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TankNBTHandler implements INBTHandler<FluidTankComponent> {

    @Override
    public boolean isClassValid(Class<?> aClass) {
        return FluidTankComponent.class.isAssignableFrom(aClass);
    }

    @Override
    public boolean storeToNBT(HolderLookup.Provider provider, @Nonnull CompoundTag compound, @Nonnull String name, @Nonnull FluidTankComponent object) {
        compound.put(name, ValueIOSerialization.save(provider, object));
        return true;
    }

    @Override
    public FluidTankComponent readFromNBT(HolderLookup.Provider provider, @Nonnull CompoundTag compound, @Nonnull String name, @Nullable FluidTankComponent currentValue) {
        if (compound.contains(name)) {
            ValueIOSerialization.load(provider, compound.getCompoundOrEmpty(name), currentValue);
            return currentValue;
        }
        return currentValue;
    }
}
