/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.platform.services.IPlatformHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class NeoForgePlatformHelper implements IPlatformHelper {

    public static IEventBus getModBus(String modId) {
        return ModList.get().getModContainerById(modId).orElseThrow(() -> new IllegalStateException("Mod " + modId + " isn't loaded")).getEventBus();
    }

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public boolean isPhysicalClient() {
        return FMLLoader.getCurrent().getDist().isClient();
    }

    @Override
    public Path getConfigDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation, @Nullable String modId) {
        Collection<ModFileScanData> scanData = modId == null ? ModList.get().getAllScanData() : List.of(ModList.get().getModFileById(modId).getFile().getScanResult());
        List<Class<?>> classList = new ArrayList<>();
        Type type = Type.getType(annotation);
        for (ModFileScanData data : scanData) {
            for (ModFileScanData.AnnotationData annotationData : data.getAnnotations()) {
                if (Objects.equals(annotationData.annotationType(), type)) {
                    try {
                        classList.add(Class.forName(annotationData.memberName()));
                    } catch (ClassNotFoundException e) {
                        Titanium.LOGGER.error(e);
                    }
                }
            }
        }
        return classList;
    }

    @Override
    public void onCommonSetup(String modId, Runnable task) {
        getModBus(modId).addListener((FMLCommonSetupEvent event) -> task.run());
    }

    @Override
    public void onClientSetup(String modId, Runnable task) {
        if (isPhysicalClient()) {
            getModBus(modId).addListener((FMLClientSetupEvent event) -> task.run());
        }
    }
}
