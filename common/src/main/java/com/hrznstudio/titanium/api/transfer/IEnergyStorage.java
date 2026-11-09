/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api.transfer;

/**
 * Loader independent energy storage. Exposed to other mods through the NeoForge energy capability and, on Fabric,
 * through {@code TitaniumApiLookups.ENERGY} by the platform modules.
 */
public interface IEnergyStorage {

    int getEnergyStored();

    int getMaxEnergyStored();

    /**
     * @return the amount that was (or would have been) received
     */
    int receiveEnergy(int maxReceive, boolean simulate);

    /**
     * @return the amount that was (or would have been) extracted
     */
    int extractEnergy(int maxExtract, boolean simulate);

    default boolean canReceive() {
        return true;
    }

    default boolean canExtract() {
        return true;
    }
}
