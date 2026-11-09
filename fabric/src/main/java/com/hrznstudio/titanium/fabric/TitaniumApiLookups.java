/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.api.transfer.IEnergyStorage;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

/**
 * Fabric has no standard energy API, Titanium exposes its energy storages through these lookups.
 */
public final class TitaniumApiLookups {

    public static final BlockApiLookup<IEnergyStorage, Direction> ENERGY =
        BlockApiLookup.get(Identifier.fromNamespaceAndPath(Titanium.MODID, "energy"), IEnergyStorage.class, Direction.class);

    public static final ItemApiLookup<IEnergyStorage, ContainerItemContext> ENERGY_ITEM =
        ItemApiLookup.get(Identifier.fromNamespaceAndPath(Titanium.MODID, "energy"), IEnergyStorage.class, ContainerItemContext.class);

    private TitaniumApiLookups() {
    }
}
