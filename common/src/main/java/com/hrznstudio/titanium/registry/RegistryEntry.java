/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.registry;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.function.Supplier;

/**
 * A loader independent reference to a registered object, the equivalent of NeoForge's DeferredHolder.
 *
 * @param <T> the type of the registry
 * @param <R> the type of the registered object
 */
public interface RegistryEntry<T, R extends T> extends Supplier<R> {

    /**
     * @return the registered object, throws if it isn't registered yet
     */
    @Override
    R get();

    /**
     * Same as {@link #get()}
     */
    default R value() {
        return get();
    }

    ResourceKey<T> getKey();

    default Identifier getId() {
        return getKey().identifier();
    }

    default ResourceKey<? extends Registry<T>> getRegistryKey() {
        return getKey().registryKey();
    }

    /**
     * @return the registry holder of the object
     */
    Holder<T> holder();

    boolean isBound();
}
