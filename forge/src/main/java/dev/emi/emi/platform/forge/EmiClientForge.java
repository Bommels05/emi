package dev.emi.emi.platform.forge;

import com.mojang.blaze3d.systems.RenderSystem;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import dev.emi.emi.EmiPort;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;

public class EmiClientForge {

	@SubscribeEvent
	public void postRenderScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		Screen screen = event.gui;
		if (!(screen instanceof HandledScreen)) {
			return;
		}
		EmiScreenBase base = EmiScreenBase.of(screen);
		if (base != null) {
			//Foreground
			MinecraftClient client = MinecraftClient.getInstance();
			MatrixStack viewStack = RenderSystem.getModelViewStack();
			viewStack.push();
			RenderSystem.applyModelViewMatrix();
			EmiPort.setPositionTexShader();
			GL11.glColor4f(1f, 1f, 1f, 1f);
			boolean enabledBefore = GL11.glIsEnabled(GL11.GL_LIGHTING);
			GL11.glDisable(GL11.GL_LIGHTING);
			EmiScreenManager.render(context, event.mouseX, event.mouseY, client.ticker.tickDelta);
			viewStack.pop();
			RenderSystem.applyModelViewMatrix();
			//Post
			context.push();
			EmiPort.setPositionTexShader();
			EmiScreenManager.drawForeground(context, event.mouseX, event.mouseY, event.renderPartialTicks);
			context.pop();
			if (enabledBefore) {
				GL11.glEnable(GL11.GL_LIGHTING);
			}
		}
	}
}
