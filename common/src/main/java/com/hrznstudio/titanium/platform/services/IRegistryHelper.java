/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.registry.RegistryEntry;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;

import java.util.function.Consumer;
import java.util.function.Function;

public interface IRegistryHelper {

    /**
     * Registers an object for the given mod. On NeoForge this goes through a DeferredRegister, on Fabric it is
     * registered right away.
     *
     * @param factory creates the object from its registry id
     */
    <T, R extends T> RegistryEntry<T, R> register(String modId, ResourceKey<? extends Registry<T>> registry, String name, Function<Identifier, ? extends R> factory);

    /**
     * Creates a menu type which receives extra data from the server when opened.
     */
    <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory);

    /**
     * Opens a menu created with {@link #createMenuType(MenuFactory)}, writing the extra data for the client.
     */
    void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData);

    /**
     * @return a creative tab builder placed after the vanilla tabs
     */
    CreativeModeTab.Builder creativeModeTabBuilder();

    @FunctionalInterface
    interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int windowId, Inventory inventory, RegistryFriendlyByteBuf buffer);
    }
}
