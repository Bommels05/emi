package dev.emi.emi.platform.forge;

import cpw.mods.fml.client.IModGuiFactory;
import dev.emi.emi.screen.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.Set;

public class EmiGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(MinecraftClient arg) {}

    @Override
    public Class<? extends Screen> mainConfigGuiClass() {
        return ConfigScreen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return dev.emi.emi.backport.java.Set.of();
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement runtimeOptionCategoryElement) {
        return null;
    }
}
