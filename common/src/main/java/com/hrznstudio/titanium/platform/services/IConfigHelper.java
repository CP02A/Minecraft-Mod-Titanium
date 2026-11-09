/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.annotation.config.ConfigType;
import net.neoforged.fml.config.IConfigSpec;

import java.util.function.Consumer;

/**
 * Config registration. {@link IConfigSpec} and {@code ModConfigSpec} are provided by NeoForge and by Forge Config API
 * Port on Fabric.
 */
public interface IConfigHelper {

    void registerConfig(String modId, ConfigType type, IConfigSpec spec, String fileName);

    /**
     * Called with the spec of the config of the given mod when it gets loaded.
     */
    void onConfigLoading(String modId, Consumer<IConfigSpec> listener);

    /**
     * Called with the spec of the config of the given mod when it gets reloaded.
     */
    void onConfigReloading(String modId, Consumer<IConfigSpec> listener);
}
