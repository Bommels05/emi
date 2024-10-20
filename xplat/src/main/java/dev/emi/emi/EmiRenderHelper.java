package dev.emi.emi;

import java.text.DecimalFormat;
import java.util.List;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.recipe.handler.StandardRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.config.EmiConfig;
import dev.emi.emi.mixin.accessor.DrawableHelperAccessor;
import dev.emi.emi.registry.EmiRecipeFiller;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import dev.emi.emi.screen.widget.config.ListWidget;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.tooltip.TextTooltipComponent;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraftforge.fluids.Fluid;
import org.lwjgl.opengl.GL11;

public class EmiRenderHelper {
	public static final DecimalFormat TEXT_FORMAT = new DecimalFormat("0.##");
	public static final Text EMPTY_TEXT = EmiPort.literal("");
	public static final MinecraftClient CLIENT = MinecraftClient.getInstance();
	public static final Identifier WIDGETS = EmiPort.id("emi", "textures/gui/widgets.png");
	public static final Identifier BUTTONS = EmiPort.id("emi", "textures/gui/buttons.png");
	public static final Identifier BACKGROUND = EmiPort.id("emi", "textures/gui/background.png");
	public static final Identifier GRID = EmiPort.id("emi", "textures/gui/grid.png");
	public static final Identifier DASH = EmiPort.id("emi", "textures/gui/dash.png");
	public static final Identifier CONFIG = EmiPort.id("emi", "textures/gui/config.png");
	public static final Identifier PIECES = EmiPort.id("emi", "textures/gui/pieces.png");
	public static final ItemRenderer ITEM_RENDERER = new ItemRenderer();

	public static void drawNinePatch(EmiDrawContext context, Identifier texture, int x, int y, int w, int h, int u, int v, int cornerLength, int centerLength) {
		int cor = cornerLength;
		int cen = centerLength;
		int corcen = cor + cen;
		int innerWidth = w - cornerLength * 2;
		int innerHeight = h - cornerLength * 2;
		int coriw = cor + innerWidth;
		int corih = cor + innerHeight;
		// TL
		context.drawTexture(texture, x,         y,         cor,        cor,         u,          v,          cor, cor, 256, 256);
		// T
		context.drawTexture(texture, x + cor,   y,         innerWidth, cor,         u + cor,    v,          cen, cor, 256, 256);
		// TR
		context.drawTexture(texture, x + coriw, y,         cor,        cor,         u + corcen, v,          cor, cor, 256, 256);
		// L
		context.drawTexture(texture, x,         y + cor,   cor,        innerHeight, u,          v + cor,    cor, cen, 256, 256);
		// C
		context.drawTexture(texture, x + cor,   y + cor,   innerWidth, innerHeight, u + cor,    v + cor,    cen, cen, 256, 256);
		// R
		context.drawTexture(texture, x + coriw, y + cor,   cor,        innerHeight, u + corcen, v + cor,    cor, cen, 256, 256);
		// BL
		context.drawTexture(texture, x,         y + corih, cor,        cor,         u,          v + corcen, cor, cor, 256, 256);
		// B
		context.drawTexture(texture, x + cor,   y + corih, innerWidth, cor,         u + cor,    v + corcen, cen, cor, 256, 256);
		// BR
		context.drawTexture(texture, x + coriw, y + corih, cor,        cor,         u + corcen, v + corcen, cor, cor, 256, 256);
	}

	/*public static void drawTintedSprite(MatrixStack matrices, Sprite sprite, int color, int x, int y, int xOff, int yOff, int width, int height) {
		if (sprite == null) {
			return;
		}
		EmiPort.setPositionColorTexShader();
		RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
		RenderSystem.setShaderTexture(0, sprite.getAtlas().getId());
		RenderSystem.enableBlend();
		
		float r = ((color >> 16) & 255) / 256f;
		float g = ((color >> 8) & 255) / 256f;
		float b = (color & 255) / 256f;
		
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuffer();
		bufferBuilder.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE);
		float xMin = (float) x;
		float yMin = (float) y;
		float xMax = xMin + width;
		float yMax = yMin + height;
		float uSpan = sprite.getMaxU() - sprite.getMinU();
		float vSpan = sprite.getMaxV() - sprite.getMinV();
		float uMin = sprite.getMinU() + uSpan / 16 * xOff;
		float vMin = sprite.getMinV() + vSpan / 16 * yOff;
		float uMax = sprite.getMaxU() - uSpan / 16 * (16 - (width + xOff));
		float vMax = sprite.getMaxV() - vSpan / 16 * (16 - (height + yOff));
		Matrix4f model = matrices.peek().getPositionMatrix();
		bufferBuilder.vertex(model, xMin, yMax, 1).color(r, g, b, 1).texture(uMin, vMax).next();
		bufferBuilder.vertex(model, xMax, yMax, 1).color(r, g, b, 1).texture(uMax, vMax).next();
		bufferBuilder.vertex(model, xMax, yMin, 1).color(r, g, b, 1).texture(uMax, vMin).next();
		bufferBuilder.vertex(model, xMin, yMin, 1).color(r, g, b, 1).texture(uMin, vMin).next();
		EmiPort.draw(bufferBuilder);
	}*/

	public static void drawScroll(EmiDrawContext context, int x, int y, int width, int height, int progress, int total, int color) {
		if (total <= 1) {
			return;
		}
		int start = x + width * progress / total;
		int end = start + Math.max(width / total, 1);
		if (progress == total - 1) {
			end = x + width;
			start = end - Math.max(width / total, 1);
		}
		context.fill(start, y, end - start, height, color);
	}

	public static Text getEmiText() {
		return
			EmiPort.append(
				EmiPort.append(
					EmiPort.literal("E", Formatting.LIGHT_PURPLE),
					EmiPort.literal("M", Formatting.GREEN)),
				EmiPort.literal("I", Formatting.AQUA));
	}

	public static Text getPageText(int page, int total, int maxWidth) {
		Text text = EmiPort.translatable("emi.page", page, total);
		if (CLIENT.textRenderer.getStringWidth(text.asUnformattedString()) > maxWidth) {
			text = EmiPort.translatable("emi.page.short", page, total);
			if (CLIENT.textRenderer.getStringWidth(text.asUnformattedString()) > maxWidth) {
				text = EmiPort.literal("" + page);
				if (CLIENT.textRenderer.getStringWidth(text.asUnformattedString()) > maxWidth) {
					text = EmiPort.literal("");
				}
			}
		}
		return text;
	}

	public static void drawLeftTooltip(Screen screen, EmiDrawContext context, List<TooltipComponent> components, int x, int y) {
		int original = screen.width;
		try {
			screen.width = x;
			drawTooltip(screen, context, components, x, y, original / 2 - 16);
		} finally {
			screen.width = original;
		}
	}

	public static void drawTooltip(Screen screen, EmiDrawContext context, List<TooltipComponent> components, int x, int y) {
		drawTooltip(screen, context, components, x, y, screen.width / 2 - 16);
	}

	public static void drawTooltip(Screen screen, EmiDrawContext context, List<TooltipComponent> components, int x, int y, int maxWidth) {
		y = Math.max(16, y);
		// Some mods assume this list will be mutable, oblige them
		List<TooltipComponent> mutable = Lists.newArrayList();
		int wrapWidth = Math.max(components.stream()
			.map(c -> c instanceof TextTooltipComponent ? 0 : c.getWidth(CLIENT.textRenderer))
			.max(Integer::compare).orElse(0), maxWidth);
		for (TooltipComponent comp : components) {
			if (comp instanceof TextTooltipComponent ottc && ottc.getWidth(CLIENT.textRenderer) > wrapWidth) {
				try {
					String text =  ottc.getText();
					for (String o : (List<String>) CLIENT.textRenderer.wrapLines(text, wrapWidth)) {
						mutable.add(new TextTooltipComponent(o));
					}
				} catch (Exception e) {
					e.printStackTrace();
					mutable.add(comp);
				}
			} else {
				mutable.add(comp);
			}
		}
		if (!mutable.isEmpty()) {
			drawTooltipInternal(CLIENT.textRenderer, screen, mutable, x, y);
		}
	}

	private static void drawTooltipInternal(TextRenderer textRenderer, Screen screen, List<TooltipComponent> tooltips, int x, int y) {
		DrawableHelperAccessor helper = (DrawableHelperAccessor) screen;
		//Copied from vanilla
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		GL11.glPushMatrix();
		GL11.glTranslatef(0, 0, 300);
		int maxWidth = 0;

		for (TooltipComponent tooltip : tooltips) {
			int width = tooltip.getWidth(textRenderer);
			if (width > maxWidth) {
				maxWidth = width;
			}
		}

		int tooltipX = x + 12;
		int tooltipY = y - 12;
		int maxHeight = 8;
		if (tooltips.size() > 1) {
			maxHeight = 0;
			for (TooltipComponent tooltip : tooltips) {
				maxHeight += tooltip.getHeight();
			}
		}

		if (tooltipX + maxWidth > screen.width) {
			tooltipX -= 28 + maxWidth;
		}

		if (tooltipY + maxHeight + 6 > screen.height) {
			tooltipY = screen.height - maxHeight - 6;
		}

		int backgroundColor = -267386864;
		//Background
		helper.fillGradient(tooltipX - 3, tooltipY - 4, tooltipX + maxWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
		helper.fillGradient(tooltipX - 3, tooltipY + maxHeight + 3, tooltipX + maxWidth + 3, tooltipY + maxHeight + 4, backgroundColor, backgroundColor);
		helper.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + maxWidth + 3, tooltipY + maxHeight + 3, backgroundColor, backgroundColor);
		helper.fillGradient(tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + maxHeight + 3, backgroundColor, backgroundColor);
		helper.fillGradient(tooltipX + maxWidth + 3, tooltipY - 3, tooltipX + maxWidth + 4, tooltipY + maxHeight + 3, backgroundColor, backgroundColor);
		//Borders
		int borderColor = 1347420415;
		int borderColor2 = (borderColor & 16711422) >> 1 | borderColor & 0xFF000000;
		helper.fillGradient(tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + maxHeight + 3 - 1, borderColor, borderColor2);
		helper.fillGradient(tooltipX + maxWidth + 2, tooltipY - 3 + 1, tooltipX + maxWidth + 3, tooltipY + maxHeight + 3 - 1, borderColor, borderColor2);
		helper.fillGradient(tooltipX - 3, tooltipY - 3, tooltipX + maxWidth + 3, tooltipY - 3 + 1, borderColor, borderColor);
		helper.fillGradient(tooltipX - 3, tooltipY + maxHeight + 2, tooltipX + maxWidth + 3, tooltipY + maxHeight + 3, borderColor2, borderColor2);

		for (int i = 0; i < tooltips.size(); i++) {
			TooltipComponent tooltip = tooltips.get(i);
			tooltip.drawText(textRenderer, tooltipX, tooltipY);
			tooltip.drawItems(textRenderer, tooltipX, tooltipY);
			if (i == 0) {
				tooltipY += 2;
			}

			tooltipY += tooltip.getHeight();
		}

		GL11.glPopMatrix();
	}

	public static void drawSlotHightlight(EmiDrawContext context, int x, int y, int w, int h, int z) {
		context.push();
		context.matrices().translate(0, 0, z);
		RenderSystem.colorMask(true, true, true, false);
		context.fill(x, y, w, h, -2130706433);
		RenderSystem.colorMask(true, true, true, true);
		context.pop();
	}

	public static Text getAmountText(EmiIngredient stack) {
		return getAmountText(stack, stack.getAmount());
	}

	public static Text getAmountText(EmiIngredient stack, long amount) {
		if (stack.isEmpty() || amount == 0) {
			return EMPTY_TEXT;
		}
		if (stack.getEmiStacks().get(0).getKey() instanceof Fluid) {
			return getFluidAmount(amount);
		}
		return EmiPort.literal("" + amount);
	}

	public static Text getAmountText(EmiIngredient stack, double amount) {
		if (stack.isEmpty() || amount == 0) {
			return EMPTY_TEXT;
		}
		if (stack.getEmiStacks().get(0).getKey() instanceof Fluid) {
			return EmiConfig.fluidUnit.translate(amount);
		}
		return EmiPort.literal(TEXT_FORMAT.format(amount));
	}

	public static Text getFluidAmount(long amount) {
		return EmiConfig.fluidUnit.translate(amount);
	}

	public static int getAmountOverflow(Text amount) {
		int width = CLIENT.textRenderer.getStringWidth(amount.asUnformattedString());
		if (width > 14) {
			return width - 14;
		} else {
			return 0;
		}
	}

	public static void renderAmount(EmiDrawContext context, int x, int y, Text amount) {
		context.push();
		context.matrices().translate(0, 0, 200);
		int tx = x + 17 - Math.min(14, CLIENT.textRenderer.getStringWidth(amount.asUnformattedString()));
		context.drawTextWithShadow(amount, tx, y + 9, -1);
		context.pop();
	}

	public static void renderIngredient(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		RenderSystem.enableDepthTest();
		context.push();
		context.matrices().translate(0, 0, 200);
		RenderSystem.setShaderTexture(0, EmiRenderHelper.WIDGETS);
		context.drawTexture(WIDGETS, x, y, 8, 252, 4, 4);
		context.pop();
		RenderSystem.disableDepthTest();
	}

	public static void renderTag(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		if (ingredient.getEmiStacks().size() > 1) {
			RenderSystem.enableDepthTest();
			context.push();
			context.matrices().translate(0, 0, 200);
			context.drawTexture(WIDGETS, x, y + 12, 0, 252, 4, 4);
			context.pop();
		}
	}

	public static void renderRemainder(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		for (EmiStack stack : ingredient.getEmiStacks()) {
			EmiStack remainder = stack.getRemainder();
			if (!remainder.isEmpty()) {
				if (remainder.equals(ingredient)) {
					renderCatalyst(ingredient, context, x, y);
				} else {
					context.push();
					context.matrices().translate(0, 0, 200);
					RenderSystem.enableDepthTest();
					context.drawTexture(WIDGETS, x + 12, y, 4, 252, 4, 4);
					context.pop();
				}
				return;
			}
		}
	}

	public static void renderCatalyst(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		RenderSystem.enableDepthTest();
		context.push();
		context.matrices().translate(0, 0, 200);
		context.drawTexture(WIDGETS, x + 12, y, 12, 252, 4, 4);
		context.pop();
		return;
	}

	public static void renderRecipeFavorite(EmiIngredient ingredient, EmiDrawContext context, int x, int y) {
		context.push();
		context.matrices().translate(0, 0, 200);
		RenderSystem.enableDepthTest();
		context.drawTexture(WIDGETS, x + 12, y, 16, 252, 4, 4);
		context.pop();
		return;
	}

	public static void renderRecipeBackground(EmiRecipe recipe, EmiDrawContext context, int x, int y) {
		context.resetColor();
		EmiRenderHelper.drawNinePatch(context, BACKGROUND, x, y, recipe.getDisplayWidth() + 8, recipe.getDisplayHeight() + 8, 27, 0, 4, 1);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static void renderRecipe(EmiRecipe recipe, EmiDrawContext context, int x, int y, boolean showMissing, int overlayColor) {
		try {
			renderRecipeBackground(recipe, context, x, y);

			List<Widget> widgets = Lists.newArrayList();
			WidgetHolder holder = new WidgetHolder() {

				public int getWidth() {
					return recipe.getDisplayWidth();
				}

				public int getHeight() {
					return recipe.getDisplayHeight();
				}

				public <T extends Widget> T add(T widget) {
					widgets.add(widget);
					return widget;
				}
			};

			MatrixStack view = RenderSystem.getModelViewStack();
			view.push();
			view.translate(x + 4, y + 4, 0);
			RenderSystem.applyModelViewMatrix();

			recipe.addWidgets(holder);
			float delta = MinecraftClient.getInstance().ticker.tickDelta;
			for (Widget widget : widgets) {
				widget.render(context.raw(), -1000, -1000, delta);
			}
			if (overlayColor != -1) {
				context.fill(-1, -1, recipe.getDisplayWidth() + 2, recipe.getDisplayHeight() + 2, overlayColor);
			}

			if (showMissing) {
				HandledScreen hs = EmiApi.getHandledScreen();
				EmiRecipeHandler handler = EmiRecipeFiller.getFirstValidHandler(recipe, hs);
				if (handler != null) {
					handler.render(recipe, new EmiCraftContext(hs, handler.getInventory(hs), EmiCraftContext.Type.FILL_BUTTON), widgets, context.raw());
				} else if (EmiScreenManager.lastPlayerInventory != null) {
					StandardRecipeHandler.renderMissing(recipe, EmiScreenManager.lastPlayerInventory, widgets, context.raw());
				}
			}

			view.pop();
			RenderSystem.applyModelViewMatrix();

			// Force translucency to match that of the recipe background
			RenderSystem.disableBlend();
			RenderSystem.colorMask(false, false, false, true);
			RenderSystem.disableDepthTest();
			renderRecipeBackground(recipe, context, x, y);
			RenderSystem.enableDepthTest();
			RenderSystem.colorMask(true, true, true, true);
			// Blend should be off by default
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	public static void renderSplitBackground(Screen screen, ListWidget list) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, DrawableHelper.OPTIONS_BACKGROUND_TEXTURE);
		Tessellator tessellator = Tessellator.INSTANCE;
		tessellator.begin();
		tessellator.color(4210752);
		tessellator.vertex(0, list.getTop(), 0.0, 0.0, ((float) list.getTop() / 32F - 100F));
		tessellator.vertex(screen.width, list.getTop(), 0.0, ((float) screen.width / 32F), ((float) list.getTop() / 32F - 100F));
		tessellator.vertex(screen.width, 0.0, 0.0, ((float) screen.width / 32F), -100F);
		tessellator.vertex(0.0, 0.0, 0.0, 0.0, -100F);
		tessellator.end();
		tessellator.begin();
		tessellator.color(4210752);
		tessellator.vertex(0, screen.height, 0.0, 0.0, ((float) (screen.height - list.getBottom()) / 32F - 100F));
		tessellator.vertex(screen.width, screen.height, 0.0, ((float) screen.width / 32F), ((float) (screen.height - list.getBottom()) / 32F - 100F));
		tessellator.vertex(screen.width, list.getBottom(), 0.0, ((float) screen.width / 32F), -100F);
		tessellator.vertex(0.0, list.getBottom(), 0.0, 0.0, -100F);
		tessellator.end();
	}
}
