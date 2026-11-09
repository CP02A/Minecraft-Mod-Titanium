/*
 * This file is part of Titanium
 * Copyright (C) 2026, Horizon Studio <contact@hrznstudio.com>.
 *
 * This code is licensed under GNU Lesser General Public License v3.0, the full license text can be found in LICENSE.txt
 */

package com.hrznstudio.titanium.client.screen.container;

import net.minecraft.world.inventory.Slot;
import net.minecraft.network.chat.TextColor;
import com.hrznstudio.titanium.api.client.AssetTypes;
import com.hrznstudio.titanium.api.client.IScreenAddon;
import com.hrznstudio.titanium.client.screen.IScreenAddonConsumer;
import com.hrznstudio.titanium.client.screen.addon.AssetScreenAddon;
import com.hrznstudio.titanium.client.screen.addon.WidgetScreenAddon;
import com.hrznstudio.titanium.client.screen.asset.IAssetProvider;
import com.hrznstudio.titanium.container.BasicAddonContainer;
import com.hrznstudio.titanium.util.AssetUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.mojang.blaze3d.platform.InputConstants;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class BasicContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> implements IScreenAddonConsumer {
    private final T container;
    private final Component title;
    private IAssetProvider assetProvider;
    protected int xCenter;
    protected int yCenter;
    private List<IScreenAddon> addons;

    private int dragX;
    private int dragY;
    private boolean isMouseDragging;

    public BasicContainerScreen(T container, Inventory inventory, Component title) {
        super(container, inventory, title,
            IAssetProvider.getAsset(IAssetProvider.DEFAULT_PROVIDER, AssetTypes.BACKGROUND).getArea().width,
            IAssetProvider.getAsset(IAssetProvider.DEFAULT_PROVIDER, AssetTypes.BACKGROUND).getArea().height);
        this.container = container;
        this.title = title;
        this.assetProvider = IAssetProvider.DEFAULT_PROVIDER;
        this.isMouseDragging = false;
        this.addons = new ArrayList<>();
    }

    public BasicContainerScreen(T container, Inventory inventory, Component title, IAssetProvider provider) {
        super(container, inventory, title,
            IAssetProvider.getAsset(provider, AssetTypes.BACKGROUND).getArea().width,
            IAssetProvider.getAsset(provider, AssetTypes.BACKGROUND).getArea().height);
        this.container = container;
        this.title = title;
        this.assetProvider = provider;
        this.addons = new ArrayList<>();
    }

    // init
    @Override
    protected void init() {
        super.init();
        this.getAddons().forEach(screenAddon -> screenAddon.init(this.leftPos, this.topPos));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        // Width
        xCenter = (width - imageWidth) / 2;
        // Height
        yCenter = (height - imageHeight) / 2;
        //BG RENDERING
        AssetUtil.blit(guiGraphics, IAssetProvider.getAsset(assetProvider, AssetTypes.BACKGROUND).getIdentifier(), xCenter, yCenter, 0, 0, imageWidth, imageHeight);
        guiGraphics.text(this.font, title.getString(), (int) getTitleX(xCenter), (int) getTitleY(yCenter), getTitleColor(), false);
        addons.stream().filter(IScreenAddon::isBackground).forEach(iGuiAddon -> {
            iGuiAddon.drawBackgroundLayer(guiGraphics, this, assetProvider, xCenter, yCenter, mouseX, mouseY, partialTicks);
        });
        addons.stream().filter(iScreenAddon -> !iScreenAddon.isBackground()).forEach(iGuiAddon -> {
            iGuiAddon.drawBackgroundLayer(guiGraphics, this, assetProvider, xCenter, yCenter, mouseX, mouseY, partialTicks);
        });
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        addons.forEach(iGuiAddon -> {
            if (iGuiAddon instanceof AssetScreenAddon assetGuiAddon) {
                if (!assetGuiAddon.isBackground()) {
                    iGuiAddon.drawForegroundLayer(guiGraphics, this, assetProvider, xCenter, yCenter, mouseX, mouseY, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
                }
            } else {
                iGuiAddon.drawForegroundLayer(guiGraphics, this, assetProvider, xCenter, yCenter, mouseX, mouseY, minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false));
            }
        });
        // renderHoveredToolTip
        for (IScreenAddon iScreenAddon : addons) {
            if (iScreenAddon.isMouseOver(mouseX, mouseY) && !iScreenAddon.getTooltipLines().isEmpty()) {
                guiGraphics.setComponentTooltipForNextFrame(this.font, iScreenAddon.getTooltipLines(), mouseX, mouseY);
            }
        }
    }

    private boolean skipChildClickDispatch = false;

    /**
     * Since 26.x vanilla only offers a click to the first child under the mouse and consumes the click even if that
     * child ignores it. Titanium addons overlap (backgrounds, bars, panels), so a passive addon would swallow clicks
     * meant for buttons, tanks and slots below it. Offer the click to every child under the mouse until one handles it,
     * then let the container handle the slots.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (GuiEventListener child : this.children()) {
            if (child.isMouseOver(event.x(), event.y()) && child.mouseClicked(event, doubleClick)) {
                if (child.shouldTakeFocusAfterInteraction()) {
                    this.setFocused(child);
                }
                if (event.button() == 1) { // same check as vanilla ContainerEventHandler
                    this.setDragging(true);
                }
                return true;
            }
        }
        this.skipChildClickDispatch = true;
        try {
            return super.mouseClicked(event, doubleClick);
        } finally {
            this.skipChildClickDispatch = false;
        }
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        return this.skipChildClickDispatch ? Optional.empty() : super.getChildAt(mouseX, mouseY);
    }

    /**
     * Titanium places slots outside of the main background (bundles, side panels). Vanilla treats every click outside
     * of the background as a click outside of the screen, which makes those slots impossible to take items from.
     */
    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top) {
        if (!super.hasClickedOutside(mouseX, mouseY, left, top)) return false;
        for (Slot slot : this.menu.slots) {
            if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.children() != null) {
            for (GuiEventListener listener : this.children()) {
                if (listener instanceof WidgetScreenAddon addon) {
                    AbstractWidget widget = addon.getWidget();
                    if (widget.keyPressed(event)) {
                        return true;
                    }
                    if (widget.isFocused()) {
                        if (this.minecraft.options.keyInventory.matches(event)) {
                            return true;
                        }
                    }
                }
            }
        }
        return super.keyPressed(event);
    }

    private void checkForMouseDrag(int mouseX, int mouseY) {
        int pressedButton = InputConstants.MOUSE_BUTTON_LEFT;
        if (Minecraft.getInstance().mouseHandler.isLeftPressed()) {//Main Window
            if (!this.isMouseDragging) {
                this.isMouseDragging = true;
            } else {
                for (IScreenAddon iScreenAddon : this.addons) {
                    if (iScreenAddon.isMouseOver(mouseX, mouseY)) {
                        iScreenAddon.mouseDragged(mouseX - this.xCenter, mouseY - this.yCenter, pressedButton, dragX, dragY);
                    }
                }
            }
            this.dragX = mouseX;
            this.dragY = mouseY;
        } else {
            this.isMouseDragging = false;
        }
    }

    public int getX() {
        return xCenter;
    }

    public int getY() {
        return yCenter;
    }

    @Override
    @Nonnull
    public T getMenu() {
        return container;
    }

    @Override
    public List<IScreenAddon> getAddons() {
        return addons;
    }

    @Override
    public List<? extends GuiEventListener> children() {
        if (super.children() != null) {
            List<GuiEventListener> collect = super.children().stream().map(guiEventListener -> (GuiEventListener) guiEventListener).collect(Collectors.toList());
            collect.addAll(getAddons());
            return collect;
        }
        return new ArrayList<>();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return super.mouseScrolled(mouseX - xCenter, mouseY - yCenter, scrollX, scrollY);
    }

    public void setAddons(List<IScreenAddon> addons) {
        this.addons = addons;
    }

    public IAssetProvider getAssetProvider() {
        return assetProvider;
    }

    public void setAssetProvider(IAssetProvider assetProvider) {
        this.assetProvider = assetProvider;
    }

    public int getTitleColor() {
        if (container instanceof BasicAddonContainer addonContainer) {
            return addonContainer.getTitleColorFromProvider();
        }
        return TextColor.DARK_GRAY.getValue();
    }

    public float getTitleX(float xCenter) {
        if (container instanceof BasicAddonContainer addonContainer) {
            return addonContainer.getTitleXPos(font.width(title.getString()), width, height, imageWidth, imageHeight);
        }
        return xCenter / 2 + imageHeight / 2 - font.width(title.getString()) / 2;
    }

    public float getTitleY(float yCenter) {
        if (container instanceof BasicAddonContainer addonContainer) {
            return addonContainer.getTitleYPos(font.width(title.getString()), width, height, imageWidth, imageHeight);
        }
        return yCenter / 2 + 6;
    }
}
