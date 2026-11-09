/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.config;

import com.hrznstudio.titanium.annotation.config.ConfigFile;
import com.hrznstudio.titanium.annotation.config.ConfigVal;
import com.hrznstudio.titanium.util.AnnotationUtil;
import com.hrznstudio.titanium.annotation.config.ConfigType;
import com.hrznstudio.titanium.platform.Services;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AnnotationConfigManager {

    public List<Type> configClasses;
    public List<SpecCache> specCaches;

    public AnnotationConfigManager() {
        configClasses = new ArrayList<>();
        specCaches = new ArrayList<>();
    }

    public void add(String modId, Type type) {
        configClasses.add(type);
        SpecCache specCache = new SpecCache();
        // SCANNING CLASSES
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        for (Class configClass : type.configClass) {
            scanClass(modId, configClass, builder, specCache);
        }
        // REGISTERING CONFIG
        File folder = Services.PLATFORM.getConfigDirectory().resolve(modId).toFile();
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String fileName = modId + "/" + (type.fileName.isEmpty() ? modId : type.fileName);
        if (!fileName.endsWith(".toml")) fileName = fileName + ".toml";
        specCache.spec = builder.build();
        specCaches.add(specCache);
        Services.CONFIG.registerConfig(modId, type.type, specCache.spec, fileName);
    }

    private void scanClass(String modId, Class configClass, ModConfigSpec.Builder builder, SpecCache specCache) {
        builder.push(configClass.getSimpleName());
        try {
            for (Field field : configClass.getFields()) {
                if (Modifier.isStatic(field.getModifiers()) && field.isAnnotationPresent(ConfigVal.class)) {
                    if (field.getType().isPrimitive() || field.getType().equals(String.class)) {
                        ConfigVal value = field.getAnnotation(ConfigVal.class);
                        ModConfigSpec.ConfigValue configValue = null;
                        if (!value.comment().isEmpty()) builder.comment(value.comment());

                        if (field.isAnnotationPresent(ConfigVal.InRangeDouble.class))
                            configValue = builder.defineInRange(value.value().isEmpty() ? field.getName() : value.value(), (Double) field.get(null),
                                    field.getAnnotation(ConfigVal.InRangeDouble.class).min(), field.getAnnotation(ConfigVal.InRangeDouble.class).max());
                        if (field.isAnnotationPresent(ConfigVal.InRangeLong.class))
                            configValue = builder.defineInRange(value.value().isEmpty() ? field.getName() : value.value(), (Long) field.get(null),
                                    field.getAnnotation(ConfigVal.InRangeLong.class).min(), field.getAnnotation(ConfigVal.InRangeLong.class).max());
                        if (field.isAnnotationPresent(ConfigVal.InRangeInt.class))
                            configValue = builder.defineInRange(value.value().isEmpty() ? field.getName() : value.value(), (Integer) field.get(null),
                                    field.getAnnotation(ConfigVal.InRangeInt.class).min(), field.getAnnotation(ConfigVal.InRangeInt.class).max());

                        if (configValue == null)
                            configValue = builder.define(value.value().isEmpty() ? field.getName() : value.value(), field.get(null));
                        specCache.cachedConfigValues.put(field, configValue);
                    } if (field.getType().equals(List.class)) {
                        ConfigVal value = field.getAnnotation(ConfigVal.class);
                        ModConfigSpec.ConfigValue configValue = null;
                        if (!value.comment().isEmpty()) builder.comment(value.comment());
                        configValue = builder.defineList(value.value().isEmpty() ? field.getName() : value.value(), (List<String>) field.get(null), (object) -> true);
                        specCache.cachedConfigValues.put(field, configValue);
                    } else {
                        scanClass(modId, field.getType(), builder, specCache);
                    }
                }
            }
            AnnotationUtil.getFilteredAnnotatedClasses(ConfigFile.Child.class, modId).stream()
                .filter(aClass -> ((ConfigFile.Child) aClass.getAnnotation(ConfigFile.Child.class)).value().equals(configClass)).forEach(aClass -> scanClass(modId, aClass, builder, specCache));
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        builder.pop();
    }

    public void inject(IConfigSpec spec) {
        // MODIFYING CLASSES VALUES FROM CONFIG
        specCaches.forEach(specCache -> {
            if (specCache.spec == spec) {
                specCache.cachedConfigValues.forEach((field, configValue) -> {
                    try {
                        field.set(null, configValue.get());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
            }
        });
    }

    public boolean isClassManaged(Class clazz) {
        for (Type configClass : configClasses) {
            for (Class aClass : configClass.configClass) {
                if (clazz.equals(aClass)) return true;
            }
        }
        return false;
    }

    public static class Type {
        private ConfigType type;
        private Class[] configClass;
        private String fileName;

        private Type(ConfigType type, Class... configClass) {
            this.type = type;
            this.configClass = configClass;
            this.fileName = "";
        }

        public static Type client(Class... classes) {
            return new Type(ConfigType.CLIENT, classes);
        }

        public static Type common(Class... classes) {
            return new Type(ConfigType.COMMON, classes);
        }

        public static Type server(Class... classes) {
            return new Type(ConfigType.SERVER, classes);
        }

        public static Type of(ConfigType type, Class... classes) {
            return new Type(type, classes);
        }

        public Type setName(String name) {
            this.fileName = name;
            return this;
        }

        public Class[] getConfigClass() {
            return configClass;
        }
    }

    public static class SpecCache {

        public ModConfigSpec spec;
        public HashMap<Field, ModConfigSpec.ConfigValue> cachedConfigValues = new HashMap<>();

    }
}
