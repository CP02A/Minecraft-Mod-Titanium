/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api.transfer;

import com.hrznstudio.titanium.fluid.TitaniumFluidStack;

/**
 * Loader independent multi tank fluid storage, amounts in millibuckets. Exposed to other mods through the NeoForge fluid
 * capability and the Fabric fluid storage lookup by the platform modules.
 */
public interface IFluidStorage {

    int getTanks();

    IFluidTank getTank(int tank);

    default TitaniumFluidStack getFluidInTank(int tank) {
        return getTank(tank).getFluid();
    }

    default int getTankCapacity(int tank) {
        return getTank(tank).getCapacity();
    }

    default boolean isFluidValid(int tank, TitaniumFluidStack stack) {
        return getTank(tank).canFill(stack);
    }

    /**
     * Fills the tanks in order.
     *
     * @return the amount that was (or would have been) filled
     */
    default int fill(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return 0;
        int filled = 0;
        TitaniumFluidStack remaining = resource;
        for (int i = 0; i < getTanks() && !remaining.isEmpty(); i++) {
            int tankFilled = getTank(i).fill(remaining, simulate);
            filled += tankFilled;
            remaining = remaining.shrink(tankFilled);
        }
        return filled;
    }

    /**
     * Drains the given fluid from the tanks in order.
     */
    default TitaniumFluidStack drain(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty()) return TitaniumFluidStack.EMPTY;
        int drained = 0;
        for (int i = 0; i < getTanks() && drained < resource.getAmount(); i++) {
            drained += getTank(i).drain(resource.copyWithAmount(resource.getAmount() - drained), simulate).getAmount();
        }
        return resource.copyWithAmount(drained);
    }

    /**
     * Drains up to the given amount of the first drainable fluid.
     */
    default TitaniumFluidStack drain(int maxDrain, boolean simulate) {
        for (int i = 0; i < getTanks(); i++) {
            TitaniumFluidStack fluid = getTank(i).getFluid();
            if (!fluid.isEmpty() && getTank(i).canDrain(fluid.copyWithAmount(Math.min(maxDrain, fluid.getAmount())))) {
                return drain(fluid.copyWithAmount(maxDrain), simulate);
            }
        }
        return TitaniumFluidStack.EMPTY;
    }
}
