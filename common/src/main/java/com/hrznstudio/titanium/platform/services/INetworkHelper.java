/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform.services;

import com.hrznstudio.titanium.network.MessageContext;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;

public interface INetworkHelper {

    /**
     * Registers a payload that can be sent in both directions. Handlers are executed on the main thread.
     */
    <T extends CustomPacketPayload> void registerPayload(String modId, CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec,
                                                         BiConsumer<T, MessageContext> serverHandler, BiConsumer<T, MessageContext> clientHandler);

    void sendToServer(CustomPacketPayload payload);

    void sendToPlayer(ServerPlayer player, CustomPacketPayload payload);

    void sendToPlayersNear(ServerLevel level, double x, double y, double z, double radius, CustomPacketPayload payload);
}
