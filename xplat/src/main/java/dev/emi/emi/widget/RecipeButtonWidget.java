package dev.emi.emi.widget;

import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class RecipeButtonWidget extends Widget {
	protected final EmiRecipe recipe;
	protected final int x, y, u, v;

	public RecipeButtonWidget(int x, int y, int u, int v, EmiRecipe recipe) {
		this.x = x;
		this.y = y;
		this.u = u;
		this.v = v;
		this.recipe = recipe;
	}

	public int getTextureOffset(int mouseX, int mouseY) {
		if (getBounds().contains(mouseX, mouseY)) {
			return 12;
		} else {
			return 0;
		}
	}

	public void playButtonSound() {
		MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(EmiUtil.UI_BUTTON_CLICK, 1.0f));
	}

	@Override
	public Bounds getBounds() {
		return new Bounds(x, y, 12, 12);
	}

	@Override
	public void render(MatrixStack raw, int mouseX, int mouseY, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(raw);
		context.resetColor();
		context.drawTexture(EmiRenderHelper.BUTTONS, x, y, 12, 12, u, v + getTextureOffset(mouseX, mouseY), 12, 12, 256, 256);
	}
}
