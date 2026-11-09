/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.platform;

import com.hrznstudio.titanium.platform.services.IClientHelper;

/**
 * Client only platform services. Only touch this class from client code.
 */
public final class ClientServices {

    public static final IClientHelper CLIENT = Services.load(IClientHelper.class);

    private ClientServices() {
    }
}
