/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.util;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Something that can be saved to and loaded from the vanilla value IO system.
 */
public interface IValueIOSerializable {

    void serialize(ValueOutput output);

    void deserialize(ValueInput input);
}
