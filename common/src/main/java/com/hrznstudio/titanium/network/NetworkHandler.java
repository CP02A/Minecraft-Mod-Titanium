/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.network;

import com.hrznstudio.titanium.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers and sends {@link Message}s of a mod. Messages are serialized reflectively from their fields and can be
 * sent in both directions.
 */
public class NetworkHandler {
    private final String namespace;
    private final Map<Class<? extends Message>, CustomPacketPayload.Type<MessageWrapper>> types = new HashMap<>();

    public NetworkHandler(String modid) {
        namespace = modid;
    }

    public <REQ extends Message> void registerMessage(String name, Class<REQ> message) {
        final Identifier id = Identifier.fromNamespaceAndPath(namespace, name);
        final CustomPacketPayload.Type<MessageWrapper> type = new CustomPacketPayload.Type<>(id);
        types.put(message, type);
        StreamCodec<RegistryFriendlyByteBuf, MessageWrapper> codec = StreamCodec.of((buffer, payload) -> payload.message.toBytes(buffer), buffer -> {
            try {
                REQ req = message.getConstructor().newInstance();
                req.fromBytes(buffer);
                return new MessageWrapper(type, req);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        Services.NETWORK.registerPayload(namespace, type, codec,
            (payload, context) -> payload.message.handleServerMessage(context),
            (payload, context) -> payload.message.handleClientMessage(context));
    }

    public void sendToServer(Message message) {
        Services.NETWORK.sendToServer(wrap(message));
    }

    public CustomPacketPayload wrap(Message message) {
        CustomPacketPayload.Type<MessageWrapper> type = types.get(message.getClass());
        if (type == null) throw new IllegalArgumentException("Message " + message.getClass().getName() + " isn't registered in " + namespace);
        return new MessageWrapper(type, message);
    }

    public void sendToNearby(Level world, BlockPos pos, int distance, Message message) {
        if (world instanceof ServerLevel serverLevel) {
            var centre = Vec3.atCenterOf(pos);
            Services.NETWORK.sendToPlayersNear(serverLevel, centre.x, centre.y, centre.z, distance, wrap(message));
        }
    }

    public void sendTo(Message message, ServerPlayer player) {
        Services.NETWORK.sendToPlayer(player, wrap(message));
    }

    public record MessageWrapper(CustomPacketPayload.Type<MessageWrapper> type, Message message) implements CustomPacketPayload {
    }
}
