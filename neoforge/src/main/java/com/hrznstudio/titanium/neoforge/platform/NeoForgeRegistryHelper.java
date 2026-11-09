/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.platform.services.IRegistryHelper;
import com.hrznstudio.titanium.registry.RegistryEntry;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    private static final Map<String, Map<ResourceKey<?>, DeferredRegister<?>>> REGISTERS = new ConcurrentHashMap<>();

    /**
     * @return the DeferredRegister Titanium uses for the given mod and registry, registered to the mod event bus
     */
    @SuppressWarnings("unchecked")
    public static <T> DeferredRegister<T> getDeferredRegister(String modId, ResourceKey<? extends Registry<T>> key) {
        return (DeferredRegister<T>) REGISTERS.computeIfAbsent(modId, id -> new ConcurrentHashMap<>()).computeIfAbsent(key, k -> {
            DeferredRegister<T> register = DeferredRegister.create(key, modId);
            register.register(NeoForgePlatformHelper.getModBus(modId));
            return register;
        });
    }

    @Override
    public <T, R extends T> RegistryEntry<T, R> register(String modId, ResourceKey<? extends Registry<T>> registry, String name, Function<Identifier, ? extends R> factory) {
        DeferredHolder<T, R> holder = getDeferredRegister(modId, registry).register(name, factory);
        return new Entry<>(holder);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory) {
        return IMenuTypeExtension.create(factory::create);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        player.openMenu(provider, extraData::accept);
    }

    @Override
    public CreativeModeTab.Builder creativeModeTabBuilder() {
        return CreativeModeTab.builder().withTabsBefore(CreativeModeTabs.SPAWN_EGGS);
    }

    /**
     * {@link RegistryEntry} backed by a {@link DeferredHolder}
     */
    public record Entry<T, R extends T>(DeferredHolder<T, R> deferredHolder) implements RegistryEntry<T, R> {

        @Override
        public R get() {
            return deferredHolder.get();
        }

        @Override
        public ResourceKey<T> getKey() {
            return deferredHolder.getKey();
        }

        @Override
        public Holder<T> holder() {
            return deferredHolder;
        }

        @Override
        public boolean isBound() {
            return deferredHolder.isBound();
        }
    }
}
