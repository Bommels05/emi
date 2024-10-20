package net.minecraft.client.gui.tooltip;

import net.minecraft.client.font.TextRenderer;
import dev.emi.emi.backport.OrderedText;
import net.minecraft.text.Text;

public interface TooltipComponent {
	static TooltipComponent of(Text text) {
		return new TextTooltipComponent(text.asFormattedString());
	}
	static TooltipComponent of(OrderedText text) {
		return new TextTooltipComponent(text.asString());
	}

	int getHeight();
	int getWidth(TextRenderer textRenderer);

	default void drawText(TextRenderer textRenderer, int x, int y) {}
	default void drawItems(TextRenderer textRenderer, int x, int y) {}
}
