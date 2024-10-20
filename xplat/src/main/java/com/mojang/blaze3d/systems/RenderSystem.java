package com.mojang.blaze3d.systems;

import static org.lwjgl.opengl.GL11.*;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RenderSystem {

	public static void enableDepthTest() {
		glEnable(GL_DEPTH_TEST);
	}

	public static void disableDepthTest() {
		glDisable(GL_DEPTH_TEST);
	}

	public static void enableBlend() {
		glEnable(GL_BLEND);
	}

	public static void disableBlend() {
		glDisable(GL_BLEND);
	}

	public static void defaultBlendFunc() {
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
	}

	public static MatrixStack getModelViewStack() {
		return MatrixStack.INSTANCE;
	}

	public static void applyModelViewMatrix() {
		// it's already applied
	}

	public static void setShaderColor(float r, float g, float b, float a) {
		glColor4f(r, g, b, a);
	}

	public static void setShaderTexture(int i, Identifier id) {
		glBindTexture(GL_TEXTURE_2D+i, MinecraftClient.getInstance().getTextureManager().getTexture(id).getGlId());
	}

	public static void colorMask(boolean r, boolean g, boolean b, boolean a) {
		glColorMask(r, g, b, a);
	}

}
