/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.annotation.config;

/**
 * Loader independent config type, mapped to the ModConfig.Type of each platform.
 */
public enum ConfigType {
    /**
     * Loaded on both sides, not synced. NeoForge {@code LOCAL}, Forge Config API Port {@code COMMON}.
     */
    COMMON,
    /**
     * Only loaded on the client.
     */
    CLIENT,
    /**
     * Loaded per world on the server and synced to the clients. NeoForge {@code SYNCED}, Forge Config API Port {@code SERVER}.
     */
    SERVER,
    /**
     * Loaded right away on both sides, before registration.
     */
    STARTUP
}
