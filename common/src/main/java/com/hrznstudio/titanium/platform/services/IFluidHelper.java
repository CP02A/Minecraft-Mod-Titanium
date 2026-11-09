/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.fluid.TitaniumFluid;
import com.hrznstudio.titanium.fluid.TitaniumFluidInstance;
import com.hrznstudio.titanium.module.DeferredRegistryHelper;

public interface IFluidHelper {

    TitaniumFluid.Source createSourceFluid(TitaniumFluidInstance instance);

    TitaniumFluid.Flowing createFlowingFluid(TitaniumFluidInstance instance);

    /**
     * Registers the platform specific fluid attributes (NeoForge FluidType, Fabric variant attributes) of the instance.
     */
    void registerFluidAttributes(DeferredRegistryHelper helper, TitaniumFluidInstance instance);
}
