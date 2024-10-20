package dev.emi.emi.runtime;

import dev.emi.emi.EmiPort;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.math.MatrixStack;
import dev.emi.emi.backport.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

public class EmiDrawContext {
	private final MinecraftClient client = MinecraftClient.getInstance();
	private final MatrixStack matrices;
	
	private EmiDrawContext(MatrixStack matrices) {
		this.matrices = matrices;
	}

	public static EmiDrawContext wrap(MatrixStack matrices) {
		return new EmiDrawContext(matrices);
	}

	public MatrixStack raw() {
		return matrices;
	}

	public MatrixStack matrices() {
		return matrices;
	}

	public void push() {
		matrices.push();
	}

	public void pop() {
		matrices.pop();
	}

	public void drawTexture(Identifier texture, int x, int y, int u, int v, int width, int height) {
		drawTexture(texture, x, y, width, height, u, v, width, height, 256, 256);
	}

	public void drawTexture(Identifier texture, int x, int y, int z, float u, float v, int width, int height) {
		drawTexture(texture, x, y, z, u, v, width, height, 256, 256);
	}

	public void drawTexture(Identifier texture, int x, int y, int z, float u, float v, int width, int height, int textureWidth, int textureHeight) {
		EmiPort.setPositionTexShader();
		MinecraftClient.getInstance().getTextureManager().bindTexture(texture);
		DrawableHelper.drawTexture(x, y, u, v, width, height, textureWidth, textureHeight);
	}

	public void drawTexture(Identifier texture, int x, int y, int width, int height, float u, float v, int regionWidth, int regionHeight, int textureWidth, int textureHeight) {
		EmiPort.setPositionTexShader();
		MinecraftClient.getInstance().getTextureManager().bindTexture(texture);
		DrawableHelper.drawTexture(x, y, u, v, regionWidth, regionHeight, width, height, textureWidth, textureHeight);
	}

	public void fill(int x, int y, int width, int height, int color) {
		DrawableHelper.fill( x, y, x + width, y + height, color);
		resetColor();
	}

	public void drawText(Text text, int x, int y) {
		drawText(text, x, y, -1);
	}

	public void drawText(Text text, int x, int y, int color) {
		client.textRenderer.draw(text.asFormattedString(), x, y, color);
	}

	public void drawText(String text, int x, int y, int color) {
		client.textRenderer.draw(text, x, y, color);
	}

	public void drawText(OrderedText text, int x, int y, int color) {
		client.textRenderer.draw(text.asString(), x, y, color);
	}

	public void drawTextWithShadow(Text text, int x, int y) {
		drawTextWithShadow(text, x, y, -1);
	}

	public void drawTextWithShadow(Text text, int x, int y, int color) {
		client.textRenderer.method_956(text.asFormattedString(), x, y, color);
		resetColor();
	}

	public void drawTextWithShadow(OrderedText text, int x, int y, int color) {
		client.textRenderer.method_956(text.asString(), x, y, color);
		resetColor();
	}

	public void drawCenteredText(Text text, int x, int y) {
		drawCenteredText(text, x, y, -1);
	}

	public void drawCenteredText(Text text, int x, int y, int color) {
		client.textRenderer.draw(text.asFormattedString(), x - (client.textRenderer.getStringWidth(text.asUnformattedString()) / 2), y, color);
		resetColor();
	}

	public void drawCenteredTextWithShadow(Text text, int x, int y) {
		drawCenteredTextWithShadow(text, x, y, -1);
	}

	public void drawCenteredTextWithShadow(Text text, int x, int y, int color) {
		client.textRenderer.method_956(text.asFormattedString(), x - (client.textRenderer.getStringWidth(text.asUnformattedString()) / 2), y, color);
		resetColor();
	}

	public void resetColor() {
		setColor(1f, 1f, 1f, 1f);
	}

	public void setColor(float r, float g, float b) {
		setColor(r, g, b, 1f);
	}

	public void setColor(float r, float g, float b, float a) {
		GL11.glColor4f(r, g, b, a);
	}

	public void drawStack(EmiIngredient stack, int x, int y) {
		stack.render(raw(), x, y, client.ticker.tickDelta);
	}

	public void drawStack(EmiIngredient stack, int x, int y, int flags) {
		drawStack(stack, x, y, client.ticker.tickDelta, flags);
	}

	public void drawStack(EmiIngredient stack, int x, int y, float delta, int flags) {
		stack.render(raw(), x, y, delta, flags);
	}
}
