/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.fabric.platform.FabricPlatformHelper;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class TitaniumFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        new Titanium();
        ServerPlayConnectionEvents.JOIN.register((listener, sender, server) -> Titanium.onPlayerLoggedIn(listener.getPlayer()));
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> Titanium.registerCommands(dispatcher));
        ServerTickEvents.END_LEVEL_TICK.register(Titanium::onLevelTickPost);
        // On a dedicated server the common setup runs once the server starts, the client runs it once the game started
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            if (server.isDedicatedServer()) FabricPlatformHelper.runCommonSetup();
        });
    }
}
