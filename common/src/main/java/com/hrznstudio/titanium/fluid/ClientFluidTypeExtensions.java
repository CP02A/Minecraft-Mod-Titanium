/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fluid;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

/**
 * Simple {@link TitaniumFluidRenderProperties} implementation. Override the tint methods to color the fluid.
 */
public class ClientFluidTypeExtensions implements TitaniumFluidRenderProperties {

    private final Identifier still;
    private final Identifier flow;
    @Nullable
    private final Identifier overlay;

    public ClientFluidTypeExtensions(Identifier still, Identifier flow) {
        this(still, flow, null);
    }

    public ClientFluidTypeExtensions(Identifier still, Identifier flow, @Nullable Identifier overlay) {
        this.still = still;
        this.flow = flow;
        this.overlay = overlay;
    }

    @Override
    public Identifier getStillTexture() {
        return still;
    }

    @Override
    public Identifier getFlowingTexture() {
        return flow;
    }

    @Override
    @Nullable
    public Identifier getOverlayTexture() {
        return overlay;
    }
}
