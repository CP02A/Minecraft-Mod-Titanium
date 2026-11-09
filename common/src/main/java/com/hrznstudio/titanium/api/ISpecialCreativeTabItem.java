/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.api;

import net.minecraft.world.item.CreativeModeTab;

public interface ISpecialCreativeTabItem {

    /**
     * Adds the stacks of this item to the creative tab it belongs to
     */
    void addToTab(CreativeModeTab.Output output);
}
