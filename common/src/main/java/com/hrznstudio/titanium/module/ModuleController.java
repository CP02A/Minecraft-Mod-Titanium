/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.module;

import com.hrznstudio.titanium.annotation.config.ConfigFile;
import com.hrznstudio.titanium.annotation.plugin.FeaturePlugin;
import com.hrznstudio.titanium.api.ISpecialCreativeTabItem;
import com.hrznstudio.titanium.config.AnnotationConfigManager;
import com.hrznstudio.titanium.platform.Services;
import com.hrznstudio.titanium.plugin.PluginManager;
import com.hrznstudio.titanium.plugin.PluginPhase;
import com.hrznstudio.titanium.registry.RegistryEntry;
import com.hrznstudio.titanium.tab.TitaniumTab;
import com.hrznstudio.titanium.util.AnnotationUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Base class for mods built on Titanium. Construct it from the mod entrypoint of each loader (the {@code @Mod} class
 * constructor on NeoForge, {@code ModInitializer#onInitialize} on Fabric).
 */
public abstract class ModuleController {
    private final String modid;
    private final AnnotationConfigManager configManager = new AnnotationConfigManager();
    private final PluginManager modPluginManager;
    private final DeferredRegistryHelper deferredRegistryHelper;
    private final List<TitaniumTab> titaniumTabs;

    public ModuleController(String modid) {
        this.modid = modid;
        this.modPluginManager = new PluginManager(modid, FeaturePlugin.FeaturePluginType.MOD, featurePlugin -> Services.PLATFORM.isModLoaded(featurePlugin.value()), true);
        this.modPluginManager.execute(PluginPhase.CONSTRUCTION);
        this.deferredRegistryHelper = new DeferredRegistryHelper(this.modid);
        this.titaniumTabs = new ArrayList<>();
        onPreInit();
        onInit();
        onPostInit();
    }

    public String getModId() {
        return modid;
    }

    private void addConfig(AnnotationConfigManager.Type type) {
        for (Class configClass : type.getConfigClass()) {
            if (configManager.isClassManaged(configClass)) return;
        }
        configManager.add(modid, type);
    }

    public RegistryEntry<CreativeModeTab, CreativeModeTab> addCreativeTab(String name, Supplier<ItemStack> icon, String title, TitaniumTab tab) {
        this.titaniumTabs.add(tab);
        return this.getRegistries().registerGeneric(Registries.CREATIVE_MODE_TAB, name, () -> Services.REGISTRY.creativeModeTabBuilder()
            .icon(icon)
            .title(Component.translatable("itemGroup." + title))
            .displayItems((parameters, output) -> {
                for (Item item : tab.getTabList()) {
                    if (item instanceof ISpecialCreativeTabItem specialCreativeTabItem) {
                        specialCreativeTabItem.addToTab(output);
                    } else {
                        output.accept(item);
                    }
                }
            })
            .build());
    }

    public void onPreInit() {
        this.modPluginManager.execute(PluginPhase.PRE_INIT);
    }

    public void onInit() {
        initModules();

        this.modPluginManager.execute(PluginPhase.INIT);
    }

    public void onPostInit() {
        Services.CONFIG.onConfigLoading(modid, spec -> {
            configManager.inject(spec);
            this.modPluginManager.execute(PluginPhase.CONFIG_LOAD);
        });
        Services.CONFIG.onConfigReloading(modid, spec -> {
            configManager.inject(spec);
            this.modPluginManager.execute(PluginPhase.CONFIG_RELOAD);
        });
        AnnotationUtil.getFilteredAnnotatedClasses(ConfigFile.class, modid).forEach(aClass -> {
            ConfigFile annotation = (ConfigFile) aClass.getAnnotation(ConfigFile.class);
            addConfig(AnnotationConfigManager.Type.of(annotation.type(), aClass).setName(annotation.value()));
        });
        Services.PLATFORM.onClientSetup(modid, () -> this.modPluginManager.execute(PluginPhase.CLIENT_SETUP));
        Services.PLATFORM.onCommonSetup(modid, () -> this.modPluginManager.execute(PluginPhase.COMMON_SETUP));
        this.modPluginManager.execute(PluginPhase.POST_INIT);
    }

    protected abstract void initModules();

    public DeferredRegistryHelper getRegistries() {
        return deferredRegistryHelper;
    }
}
