/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api.transfer;

import com.hrznstudio.titanium.fluid.TitaniumFluidStack;

/**
 * A single fluid tank, amounts in millibuckets. These are the primitives the loader specific transfer adapters journal
 * so transactions can be rolled back.
 */
public interface IFluidTank {

    /**
     * @return the current contents, must not be modified
     */
    TitaniumFluidStack getFluid();

    /**
     * Replaces the contents without firing change notifications.
     */
    void setFluid(TitaniumFluidStack stack);

    int getCapacity();

    /**
     * Checks if the fluid can be inserted by automation
     */
    boolean canFill(TitaniumFluidStack stack);

    /**
     * Checks if the fluid can be extracted by automation
     */
    boolean canDrain(TitaniumFluidStack stack);

    /**
     * Called after the contents of the tank changed (after the outermost transaction committed for loader adapters)
     */
    void onContentsChanged();

    default int getFluidAmount() {
        return getFluid().getAmount();
    }

    default int fill(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !canFill(resource)) return 0;
        TitaniumFluidStack current = getFluid();
        if (!current.isEmpty() && !TitaniumFluidStack.isSameFluidSameComponents(current, resource)) return 0;
        int filled = Math.min(getCapacity() - current.getAmount(), resource.getAmount());
        if (filled <= 0) return 0;
        if (!simulate) {
            setFluid(current.isEmpty() ? resource.copyWithAmount(filled) : current.grow(filled));
            onContentsChanged();
        }
        return filled;
    }

    default TitaniumFluidStack drain(int maxDrain, boolean simulate) {
        TitaniumFluidStack current = getFluid();
        if (maxDrain <= 0 || current.isEmpty()) return TitaniumFluidStack.EMPTY;
        int drained = Math.min(maxDrain, current.getAmount());
        TitaniumFluidStack result = current.copyWithAmount(drained);
        if (!canDrain(result)) return TitaniumFluidStack.EMPTY;
        if (!simulate) {
            setFluid(current.shrink(drained));
            onContentsChanged();
        }
        return result;
    }

    default TitaniumFluidStack drain(TitaniumFluidStack resource, boolean simulate) {
        if (resource.isEmpty() || !TitaniumFluidStack.isSameFluidSameComponents(resource, getFluid())) return TitaniumFluidStack.EMPTY;
        return drain(resource.getAmount(), simulate);
    }
}
