package dev.emi.emi.screen.widget.config;

import java.util.List;
import java.util.function.Consumer;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.backport.ButtonManager;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.widget.SizedButtonWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

public class ConfigJumpButton extends SizedButtonWidget {

	public ConfigJumpButton(int x, int y, int u, int v, Consumer<ButtonWidget> action, ButtonManager manager, List<Text> text) {
		super(x, y, 16, 16, u, v, () -> true, action, manager, text);
		this.texture = EmiRenderHelper.CONFIG;
	}

	@Override
	protected int getV(int mouseX, int mouseY) {
		return this.v;
	}

	@Override
	public void render(MinecraftClient client, int mouseX, int mouseY) {
		EmiDrawContext context = EmiDrawContext.wrap(MatrixStack.INSTANCE);
		if (this.isMouseOver(client, mouseX, mouseY)) {
			context.setColor(0.5f, 0.6f, 1f);
		}
		super.render(client, mouseX, mouseY);
		context.resetColor();
	}
}
