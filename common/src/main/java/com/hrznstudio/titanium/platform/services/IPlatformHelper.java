/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.nio.file.Path;
import java.util.List;

public interface IPlatformHelper {

    /**
     * @return the name of the current platform, "NeoForge" or "Fabric"
     */
    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    /**
     * @return true when running on the physical client
     */
    boolean isPhysicalClient();

    Path getConfigDirectory();

    /**
     * Finds all the classes annotated with the given annotation.
     *
     * @param modId when not null, only the classes of the given mod are returned
     */
    List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation, @Nullable String modId);

    /**
     * Runs the task once the common setup of the platform happens for the given mod. Registries are frozen by then.
     */
    void onCommonSetup(String modId, Runnable task);

    /**
     * Runs the task once the client setup of the platform happens for the given mod. Only called on the physical client.
     */
    void onClientSetup(String modId, Runnable task);
}
