/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.platform;

import com.hrznstudio.titanium.platform.services.IRegistryHelper;
import com.hrznstudio.titanium.registry.RegistryEntry;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registers objects right away into the built-in registries.
 */
public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    @SuppressWarnings("unchecked")
    public <T, R extends T> RegistryEntry<T, R> register(String modId, ResourceKey<? extends Registry<T>> registry, String name, Function<Identifier, ? extends R> factory) {
        Registry<T> builtIn = (Registry<T>) BuiltInRegistries.REGISTRY.getValue(registry.identifier());
        if (builtIn == null) {
            throw new IllegalArgumentException("Unknown built-in registry " + registry.identifier() + ", data driven registries can't be registered to");
        }
        Identifier id = Identifier.fromNamespaceAndPath(modId, name);
        ResourceKey<T> key = ResourceKey.create(registry, id);
        Holder.Reference<R> reference = Registry.registerForHolder(builtIn, key, factory.apply(id));
        return new Entry<>((Holder.Reference<T>) (Holder.Reference<?>) reference);
    }

    @Override
    public <T extends AbstractContainerMenu> MenuType<T> createMenuType(MenuFactory<T> factory) {
        // Fabric extended menus send typed data, the extra data written by Titanium is sent as raw bytes
        return new ExtendedMenuType<T, byte[]>((windowId, inventory, data) ->
            factory.create(windowId, inventory, new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), inventory.player.registryAccess())),
            ByteBufCodecs.BYTE_ARRAY);
    }

    @Override
    public void openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> extraData) {
        player.openMenu(new ExtendedMenuProvider<byte[]>() {
            @Override
            public byte[] getScreenOpeningData(ServerPlayer serverPlayer) {
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), serverPlayer.registryAccess());
                try {
                    extraData.accept(buffer);
                    byte[] data = new byte[buffer.readableBytes()];
                    buffer.readBytes(data);
                    return data;
                } finally {
                    buffer.release();
                }
            }

            @Override
            public Component getDisplayName() {
                return provider.getDisplayName();
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                return provider.createMenu(windowId, inventory, player);
            }
        });
    }

    @Override
    public CreativeModeTab.Builder creativeModeTabBuilder() {
        return FabricCreativeModeTab.builder();
    }

    public record Entry<T, R extends T>(Holder.Reference<T> reference) implements RegistryEntry<T, R> {

        @Override
        @SuppressWarnings("unchecked")
        public R get() {
            return (R) reference.value();
        }

        @Override
        public ResourceKey<T> getKey() {
            return reference.key();
        }

        @Override
        public Holder<T> holder() {
            return reference;
        }

        @Override
        public boolean isBound() {
            return reference.isBound();
        }
    }
}
