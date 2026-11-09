/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform;

import com.hrznstudio.titanium.platform.services.IConfigHelper;
import com.hrznstudio.titanium.platform.services.IFluidHelper;
import com.hrznstudio.titanium.platform.services.INetworkHelper;
import com.hrznstudio.titanium.platform.services.IPlatformHelper;
import com.hrznstudio.titanium.platform.services.IRegistryHelper;
import com.hrznstudio.titanium.platform.services.ITransferHelper;

import java.util.ServiceLoader;

/**
 * Entry point to the loader specific implementations. Each loader module provides the implementations through
 * {@code META-INF/services} files.
 */
public final class Services {

    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);
    public static final IRegistryHelper REGISTRY = load(IRegistryHelper.class);
    public static final INetworkHelper NETWORK = load(INetworkHelper.class);
    public static final IConfigHelper CONFIG = load(IConfigHelper.class);
    public static final ITransferHelper TRANSFER = load(ITransferHelper.class);
    public static final IFluidHelper FLUID = load(IFluidHelper.class);

    private Services() {
    }

    public static <T> T load(Class<T> clazz) {
        return ServiceLoader.load(clazz, Services.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Failed to load Titanium platform service " + clazz.getName()));
    }
}
