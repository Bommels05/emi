package net.minecraft.client.gui.tooltip;

import net.minecraft.client.font.TextRenderer;
import org.lwjgl.opengl.GL11;

public class TextTooltipComponent implements TooltipComponent {
	private final String text;

	public TextTooltipComponent(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
	
	@Override
	public int getWidth(TextRenderer textRenderer) {
		return textRenderer.getStringWidth(this.text);
	}

	@Override
	public int getHeight() {
		return 10;
	}

	@Override
	public void drawText(TextRenderer textRenderer, int x, int y) {
		textRenderer.method_956(this.text, x, y, -1);
		GL11.glColor4f(1, 1, 1, 1);
	}
}
