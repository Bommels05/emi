package dev.emi.emi.mixin;

import net.minecraft.client.gui.AchievementNotification;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.config.EmiConfig;
import net.minecraft.client.MinecraftClient;

@Mixin(AchievementNotification.class)
public class ToastManagerMixin {
	
	@Inject(at = @At("HEAD"), method = "render", cancellable = true)
	private void drawHead(CallbackInfo info) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.currentScreen != null && EmiConfig.enabled && EmiApi.getHandledScreen() != null) {
			info.cancel();
		}
	}
}
