/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.neoforge;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.recipe.condition.ContentExistsCondition;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod(Titanium.MODID)
public class TitaniumNeoForge {

    public TitaniumNeoForge(IEventBus modBus) {
        new Titanium();
        modBus.addListener((RegisterEvent event) -> event.register(NeoForgeRegistries.Keys.CONDITION_CODECS, registry ->
            registry.register(ContentExistsCondition.NAME, ContentExistsCondition.CODEC)));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) Titanium.onPlayerLoggedIn(player);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> Titanium.registerCommands(event.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post event) -> Titanium.onLevelTickPost(event.getLevel()));
    }
}
