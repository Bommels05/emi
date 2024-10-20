package dev.emi.emi.mixin;

import dev.emi.emi.runtime.EmiLog;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.search.EmiSearch;
import dev.emi.emi.search.EmiSearch.CompiledQuery;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {
	@Shadow
	protected int backgroundWidth, backgroundHeight, x, y;

	private HandledScreenMixin() { super(); }

	@Intrinsic @Override
	public void renderBackground(int alpha) {
		super.renderBackground(alpha);
	}

	@Dynamic
	@Inject(at = @At("RETURN"), method = "renderBackground(I)V")
	private void renderBackground(int alpha, CallbackInfo info) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		MinecraftClient client = MinecraftClient.getInstance();
		Window window = new Window(client, client.width, client.height);
		int mouseX = (int) (this.client.mouse.x * window.getScaledWidth() / window.getWidth());
		int mouseY = (int) (this.client.mouse.y * window.getScaledHeight() / window.getHeight());
		EmiScreenManager.drawBackground(context, mouseX, mouseY, this.client.ticker.tickDelta);
	}

	@Inject(at = @At(value = "INVOKE",
			target = "net/minecraft/client/gui/screen/ingame/HandledScreen.drawForeground(II)V",
			shift = Shift.AFTER),
		method = "render")
	private void render(int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (EmiAgnos.isForge()) {
			return;
		}
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		MatrixStack viewStack = RenderSystem.getModelViewStack();
		viewStack.push();
		viewStack.translate(-x, -y, 0.0);
		RenderSystem.applyModelViewMatrix();
		EmiPort.setPositionTexShader();
		EmiScreenManager.render(context, mouseX, mouseY, delta);
		EmiScreenManager.drawForeground(context, mouseX, mouseY, delta);
		viewStack.pop();
		RenderSystem.applyModelViewMatrix();
	}
}