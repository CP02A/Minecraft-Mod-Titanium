/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.util;

import com.hrznstudio.titanium.platform.Services;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Finds annotated classes. Uses the mod scan data on NeoForge and scans the mod files on Fabric.
 */
public class AnnotationUtil {

    public static List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        return Services.PLATFORM.getAnnotatedClasses(annotation, null);
    }

    public static List<Class<?>> getFilteredAnnotatedClasses(Class<? extends Annotation> annotation, String filter) {
        return Services.PLATFORM.getAnnotatedClasses(annotation, filter);
    }
}
