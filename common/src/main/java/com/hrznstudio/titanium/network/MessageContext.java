/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.network;

import net.minecraft.world.entity.player.Player;

/**
 * Loader independent context of a received {@link Message}.
 */
public interface MessageContext {

    /**
     * @return the player that sent the message on the server, the local player on the client
     */
    Player player();

    /**
     * Runs the task on the main thread of the receiving side.
     */
    void enqueueWork(Runnable task);
}
