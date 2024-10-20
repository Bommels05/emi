package dev.emi.emi.screen.tooltip;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

public interface EmiTooltipComponent extends TooltipComponent {

	default void drawTooltip(EmiDrawContext context, TooltipRenderData tooltip) {
	}

	default void drawTooltipText(TextRenderData text) {
	}

	@Override
	default void drawItems(TextRenderer textRenderer, int x, int y) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		context.push();
		context.matrices().translate(x, y, EmiRenderHelper.ITEM_RENDERER.zOffset);
		drawTooltip(context, new TooltipRenderData(textRenderer, EmiRenderHelper.ITEM_RENDERER, x, y));
		context.pop();
	}

	@Override
	default void drawText(TextRenderer textRenderer, int x, int y) {
		drawTooltipText(new TextRenderData(textRenderer, x, y));
	}

	public static class TextRenderData {
		public final TextRenderer renderer;
		public final int x, y;
		
		public TextRenderData(TextRenderer renderer, int x, int y) {
			this.renderer = renderer;
			this.x = x;
			this.y = y;
		}

		public void draw(String text, int x, int y, int color, boolean shadow) {
			draw(EmiPort.literal(text), x, y, color, shadow);
		}

		public void draw(Text text, int x, int y, int color, boolean shadow) {
			renderer.draw(text.asFormattedString(), x + this.x, y + this.y, color, shadow);
			GL11.glColor4f(1, 1, 1, 1);
		}
	}

	public static class TooltipRenderData {
		public final TextRenderer text;
		public final ItemRenderer item;
		public final int x, y;

		public TooltipRenderData(TextRenderer text, ItemRenderer item, int x, int y) {
			this.text = text;
			this.item = item;
			this.x = x;
			this.y = y;
		}
	}
}
