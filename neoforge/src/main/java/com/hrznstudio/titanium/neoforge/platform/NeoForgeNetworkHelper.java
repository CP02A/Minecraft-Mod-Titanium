/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge.platform;

import com.hrznstudio.titanium.network.MessageContext;
import com.hrznstudio.titanium.platform.services.INetworkHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class NeoForgeNetworkHelper implements INetworkHelper {

    private final Map<String, List<Consumer<PayloadRegistrar>>> registrations = new ConcurrentHashMap<>();

    private static MessageContext wrap(IPayloadContext context) {
        return new MessageContext() {
            @Override
            public Player player() {
                return context.player();
            }

            @Override
            public void enqueueWork(Runnable task) {
                context.enqueueWork(task);
            }
        };
    }

    @Override
    public <T extends CustomPacketPayload> void registerPayload(String modId, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                                BiConsumer<T, MessageContext> serverHandler, BiConsumer<T, MessageContext> clientHandler) {
        List<Consumer<PayloadRegistrar>> modRegistrations = registrations.computeIfAbsent(modId, id -> {
            List<Consumer<PayloadRegistrar>> list = new CopyOnWriteArrayList<>();
            NeoForgePlatformHelper.getModBus(id).addListener((RegisterPayloadHandlersEvent event) -> {
                PayloadRegistrar registrar = event.registrar(id).versioned(String.valueOf(list.size()));
                list.forEach(consumer -> consumer.accept(registrar));
            });
            return list;
        });
        modRegistrations.add(registrar -> registrar.playBidirectional(type, codec,
            (payload, context) -> serverHandler.accept(payload, wrap(context)),
            (payload, context) -> clientHandler.accept(payload, wrap(context))));
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayersNear(level, null, x, y, z, radius, payload);
    }
}
