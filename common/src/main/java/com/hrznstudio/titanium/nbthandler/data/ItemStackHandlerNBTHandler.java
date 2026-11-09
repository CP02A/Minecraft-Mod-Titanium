/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.nbthandler.data;


import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import com.hrznstudio.titanium.api.INBTHandler;
import com.hrznstudio.titanium.util.ValueIOSerialization;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemStackHandlerNBTHandler implements INBTHandler<InventoryComponent> {
    @Override
    public boolean isClassValid(Class<?> aClass) {
        return InventoryComponent.class.isAssignableFrom(aClass);
    }

    @Override
    public boolean storeToNBT(HolderLookup.Provider provider, @Nonnull CompoundTag compound, @Nonnull String name, @Nonnull InventoryComponent object) {
        compound.put(name, ValueIOSerialization.save(provider, object));
        return true;
    }

    @Override
    public InventoryComponent readFromNBT(HolderLookup.Provider provider, @Nonnull CompoundTag compound, @Nonnull String name, @Nullable InventoryComponent current) {
        if (compound.contains(name)) {
            if (current == null) current = new InventoryComponent<>("", 0, 0, 1);
            ValueIOSerialization.load(provider, compound.getCompoundOrEmpty(name), current);
            return current;
        }
        return current;
    }
}
