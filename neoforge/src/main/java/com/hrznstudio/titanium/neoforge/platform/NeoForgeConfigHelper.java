/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.annotation.config.ConfigType;
import com.hrznstudio.titanium.platform.services.IConfigHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;

import java.util.function.Consumer;

public class NeoForgeConfigHelper implements IConfigHelper {

    public static ModConfig.Type toNeoForge(ConfigType type) {
        return switch (type) {
            case COMMON -> ModConfig.Type.LOCAL;
            case CLIENT -> ModConfig.Type.CLIENT;
            case SERVER -> ModConfig.Type.SYNCED;
            case STARTUP -> ModConfig.Type.STARTUP;
        };
    }

    @Override
    public void registerConfig(String modId, ConfigType type, IConfigSpec spec, String fileName) {
        ModList.get().getModContainerById(modId).orElseThrow().registerConfig(toNeoForge(type), spec, fileName);
    }

    @Override
    public void onConfigLoading(String modId, Consumer<IConfigSpec> listener) {
        NeoForgePlatformHelper.getModBus(modId).addListener((ModConfigEvent.Loading event) -> listener.accept(event.getConfig().getSpec()));
    }

    @Override
    public void onConfigReloading(String modId, Consumer<IConfigSpec> listener) {
        NeoForgePlatformHelper.getModBus(modId).addListener((ModConfigEvent.Reloading event) -> listener.accept(event.getConfig().getSpec()));
    }
}
