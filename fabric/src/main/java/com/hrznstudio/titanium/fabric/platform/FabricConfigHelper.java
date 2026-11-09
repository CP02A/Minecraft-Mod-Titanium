/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.platform;

import com.hrznstudio.titanium.annotation.config.ConfigType;
import com.hrznstudio.titanium.platform.services.IConfigHelper;
import fuzs.forgeconfigapiport.fabric.api.v5.ConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.v5.ModConfigEvents;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

import java.util.function.Consumer;

/**
 * Configs on Fabric are handled by Forge Config API Port.
 */
public class FabricConfigHelper implements IConfigHelper {

    public static ModConfig.Type toForgeConfigApiPort(ConfigType type) {
        return switch (type) {
            case COMMON -> ModConfig.Type.COMMON;
            case CLIENT -> ModConfig.Type.CLIENT;
            case SERVER -> ModConfig.Type.SERVER;
            case STARTUP -> ModConfig.Type.STARTUP;
        };
    }

    @Override
    public void registerConfig(String modId, ConfigType type, IConfigSpec spec, String fileName) {
        ConfigRegistry.INSTANCE.register(modId, toForgeConfigApiPort(type), spec, fileName);
    }

    @Override
    public void onConfigLoading(String modId, Consumer<IConfigSpec> listener) {
        ModConfigEvents.loading(modId).register(config -> listener.accept(config.getSpec()));
    }

    @Override
    public void onConfigReloading(String modId, Consumer<IConfigSpec> listener) {
        ModConfigEvents.reloading(modId).register(config -> listener.accept(config.getSpec()));
    }
}
