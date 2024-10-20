package dev.emi.emi.api.widget;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.FluidEmiStack;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.screen.EmiScreenBase;
import dev.emi.emi.screen.EmiScreenManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Texture;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.texture.TextureUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;
import org.lwjgl.opengl.GL11;

public class TankWidget extends SlotWidget {
	private final long capacity;

	public TankWidget(EmiIngredient stack, int x, int y, int width, int height, long capacity) {
		super(stack, x, y);
		this.bounds = new Bounds(x, y, width, height);
		this.capacity = capacity;
	}

	@Override
	public Bounds getBounds() {
		return bounds;
	}

	/**
	 * Sets the slot to use a custom texture.
	 * The size of the texture drawn is based on the size of the tank.
	 */
	public SlotWidget backgroundTexture(Identifier id, int u, int v) {
		return super.backgroundTexture(textureId, u, v);
	}

	@Override
	public void drawStack(MatrixStack matrices, int mouseX, int mouseY, float delta) {
		EmiIngredient ingredient = getStack();
		for (EmiStack stack : ingredient.getEmiStacks()) {
			if (stack.getKey() instanceof Fluid fluid) {
				FluidEmiStack fes = new FluidEmiStack(fluid, stack.getNbt(), ingredient.getAmount());
				boolean floaty = EmiAgnos.isFloatyFluid(fes);
				Bounds bounds = getBounds();
				int x = bounds.x() + 1;
				int y = bounds.y() + 1;
				int w = bounds.width() - 2;
				int h = bounds.height() - 2;
				int filledHeight = Math.max(1, (int) Math.min(h, (fes.getAmount() * h / capacity)));
				int sy = floaty ? y : y + h;
				for (int oy = 0; oy < filledHeight; oy += 16) {
					int rh = Math.min(16, filledHeight - oy);
					for (int ox = 0; ox < w; ox += 16) {
						int rw = Math.min(16, w - ox);
						Screen screen = MinecraftClient.getInstance().currentScreen;
						RenderSystem.setShaderTexture(0, SpriteAtlasTexture.BLOCK_ATLAS_TEX);
						Texture texture = fes.getKeyOfType(Fluid.class).getIcon();
						if (floaty) {
							screen.method_4944(x + ox, sy + oy, texture, rw, rh);
						} else {
							screen.method_4944( x + ox, sy + (oy + rh) * -1, texture, rw, rh);
						}
					}
				}
				return;
			}
		}
	}
}
