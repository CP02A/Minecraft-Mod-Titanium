/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.itemstack;

import com.google.common.collect.Lists;
import com.hrznstudio.titanium.api.IFactory;
import com.hrznstudio.titanium.api.client.IScreenAddonProvider;
import com.hrznstudio.titanium.container.addon.IContainerAddon;
import com.hrznstudio.titanium.container.addon.IContainerAddonProvider;
import com.hrznstudio.titanium.network.IButtonHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ItemStackHarness implements IContainerAddonProvider, IScreenAddonProvider, IButtonHandler {
    private final ItemStack itemStack;
    private final IButtonHandler buttonHandler;
    private final Function<ItemStack, ?>[] capabilities;
    private final IScreenAddonProvider defaultProvider;

    public ItemStackHarness(ItemStack itemStack, IScreenAddonProvider defaultProvider, IButtonHandler buttonHandler, Function<ItemStack, ?>... capabilities) {
        this.itemStack = itemStack;
        this.defaultProvider = defaultProvider;
        this.buttonHandler = buttonHandler;
        this.capabilities = capabilities;
    }

    @Override
    @Nonnull
    public List<IFactory<?>> getScreenAddons() {
        List<IFactory<?>> screenAddons = Lists.newArrayList();
        if (defaultProvider != null) screenAddons.addAll(defaultProvider.getScreenAddons());
        for (var capability : capabilities) {
            screenAddons.addAll(Optional.ofNullable(capability.apply(itemStack))
                .filter(cap -> cap instanceof IScreenAddonProvider)
                .map(cap -> (IScreenAddonProvider)cap)
                .map(IScreenAddonProvider::getScreenAddons)
                .orElseGet(Lists::newArrayList));
        }
        return screenAddons;
    }

    @Override
    @Nonnull
    public List<IFactory<? extends IContainerAddon>> getContainerAddons() {
        List<IFactory<? extends IContainerAddon>> containerAddons = Lists.newArrayList();
        for (var capability : capabilities) {
            containerAddons.addAll(Optional.ofNullable(capability.apply(itemStack))
                .filter(cap -> cap instanceof IContainerAddonProvider)
                .map(cap -> (IContainerAddonProvider)cap)
                .map(IContainerAddonProvider::getContainerAddons)
                .orElseGet(Lists::newArrayList));
        }
        return containerAddons;
    }

    @Override
    public void handleButtonMessage(int id, Player playerEntity, CompoundTag compound) {
        if (buttonHandler != null) {
            buttonHandler.handleButtonMessage(id, playerEntity, compound);
        }
    }
}
