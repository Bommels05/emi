package dev.emi.emi.mixin;

import java.util.concurrent.Executors;

import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

	@Inject(at = @At("HEAD"), method = "connect(Lnet/minecraft/client/world/ClientWorld;Ljava/lang/String;)V")
	private void connect(ClientWorld world, String loadingMessage, CallbackInfo ci) {
		if (world == null) {
			EmiLog.info("Disconnecting from server, EMI data cleared");
			//EmiReloadManager.clear();
			EmiClient.onServer = false;
		} else {
			Executors.newFixedThreadPool(1).submit(() -> {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client.world != null) {
					//EmiReloadManager.reload();
				}
			});
		}
	}
}
