/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.fabric.platform;

import com.hrznstudio.titanium.Titanium;
import com.hrznstudio.titanium.platform.services.IPlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class FabricPlatformHelper implements IPlatformHelper {

    private static final List<Runnable> COMMON_SETUP = new ArrayList<>();
    private static final List<Runnable> CLIENT_SETUP = new ArrayList<>();
    private static boolean commonSetupDone = false;
    private static boolean clientSetupDone = false;

    /**
     * Annotation descriptor -> class names, per mod id. Scanned lazily once per mod.
     */
    private static final Map<String, Map<String, List<String>>> SCAN_CACHE = new HashMap<>();

    /**
     * Fabric has no setup phases, Titanium runs the common setup tasks once the game (client) or the dedicated server
     * starts, after every mod initialized.
     */
    public static synchronized void runCommonSetup() {
        if (commonSetupDone) return;
        commonSetupDone = true;
        COMMON_SETUP.forEach(Runnable::run);
        COMMON_SETUP.clear();
    }

    /**
     * Runs the client setup tasks, called by the Titanium client entrypoint (after every main entrypoint ran)
     */
    public static synchronized void runClientSetup() {
        if (clientSetupDone) return;
        clientSetupDone = true;
        CLIENT_SETUP.forEach(Runnable::run);
        CLIENT_SETUP.clear();
    }

    @Override
    public String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public List<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation, @Nullable String modId) {
        Collection<ModContainer> mods;
        if (modId != null) {
            mods = FabricLoader.getInstance().getModContainer(modId).map(List::of).orElse(List.of());
        } else {
            // Only the mods built on Titanium can use its annotations
            mods = FabricLoader.getInstance().getAllMods().stream()
                .filter(mod -> mod.getMetadata().getId().equals(Titanium.MODID) || mod.getMetadata().getDependencies().stream().anyMatch(dependency -> dependency.getModId().equals(Titanium.MODID)))
                .toList();
        }
        String descriptor = Type.getDescriptor(annotation);
        List<Class<?>> classes = new ArrayList<>();
        for (ModContainer mod : mods) {
            for (String className : scan(mod).getOrDefault(descriptor, List.of())) {
                try {
                    classes.add(Class.forName(className, true, FabricPlatformHelper.class.getClassLoader()));
                } catch (ClassNotFoundException | LinkageError e) {
                    Titanium.LOGGER.error("Failed to load annotated class {}", className, e);
                }
            }
        }
        return classes;
    }

    private static synchronized Map<String, List<String>> scan(ModContainer mod) {
        return SCAN_CACHE.computeIfAbsent(mod.getMetadata().getId(), id -> {
            Map<String, List<String>> result = new HashMap<>();
            for (Path root : mod.getRootPaths()) {
                try (Stream<Path> files = Files.walk(root)) {
                    files.filter(path -> path.toString().endsWith(".class")).forEach(path -> {
                        try (InputStream stream = Files.newInputStream(path)) {
                            ClassReader reader = new ClassReader(stream);
                            Set<String> annotations = new HashSet<>();
                            reader.accept(new ClassVisitor(Opcodes.ASM9) {
                                @Override
                                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                                    annotations.add(descriptor);
                                    return null;
                                }
                            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                            String className = reader.getClassName().replace('/', '.');
                            annotations.forEach(descriptor -> result.computeIfAbsent(descriptor, key -> new ArrayList<>()).add(className));
                        } catch (IOException | RuntimeException e) {
                            Titanium.LOGGER.warn("Failed to scan {} for annotations", path, e);
                        }
                    });
                } catch (IOException e) {
                    Titanium.LOGGER.error("Failed to scan mod {} for annotations", id, e);
                }
            }
            Titanium.LOGGER.debug("Scanned mod {} ({}) for annotations, found {} annotation types", id, mod.getRootPaths(), result.size());
            return result;
        });
    }

    @Override
    public synchronized void onCommonSetup(String modId, Runnable task) {
        if (commonSetupDone) {
            task.run();
        } else {
            COMMON_SETUP.add(task);
        }
    }

    @Override
    public synchronized void onClientSetup(String modId, Runnable task) {
        if (!isPhysicalClient()) return;
        if (clientSetupDone) {
            task.run();
        } else {
            CLIENT_SETUP.add(task);
        }
    }
}
