package dev.emi.emi;

import java.io.InputStream;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import cpw.mods.fml.common.registry.FMLControlledNamespacedRegistry;
import cpw.mods.fml.common.registry.GameData;
import dev.emi.emi.backport.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

import dev.emi.emi.api.stack.Comparison;
import net.minecraft.block.Block;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/**
 * Multiversion quarantine, to avoid excessive git pain
 */
public final class EmiPort {
	public static Text literal(String s) {
		return new LiteralText(s);
	}

	public static Text literal(String s, Formatting formatting) {
		return new LiteralText(s).setStyle(fromFormatting(formatting));
	}

	public static Text literal(String s, Formatting... formatting) {
		return new LiteralText(s).setStyle(fromFormatting(formatting));
	}

	private static Style fromFormatting(Formatting... formattings) {
		Style style = new Style();
		boolean colored = false;

		for (Formatting formatting : formattings) {
			if (EmiUtil.getColorValue(formatting) != -1) {
				if (colored) {
					throw new IllegalArgumentException("Only one color formatting is supported");
				}
				style.setFormatting(formatting);
				colored = true;
			}  else {
				switch (formatting) {
					case OBFUSCATED -> style.setObfuscated(true);
					case BOLD -> style.setBold(true);
					case STRIKETHROUGH -> style.setStrikethrough(true);
					case UNDERLINE -> style.setUnderline(true);
					case ITALIC -> style.setItalic(true);
				}
			}
		}
		return style;
	}

	public static Text literal(String s, Style style) {
		return new LiteralText(s).setStyle(style);
	}
	
	public static Text translatable(String s) {
		return new TranslatableText(s);
	}
	
	public static Text translatable(String s, Formatting formatting) {
		return new TranslatableText(s).setStyle(fromFormatting(formatting));
	}
	
	public static Text translatable(String s, Object... objects) {
		return new TranslatableText(s, objects);
	}

	public static Text append(Text text, Text appended) {
		return text.append(appended);
	}

	public static OrderedText ordered(Text text) {
		return OrderedText.of(text);
	}

	public static Collection<Identifier> findResources(EmiResourceManager manager, String prefix, Predicate<String> pred) {
		return manager.findResources(prefix, i -> pred.test(i.toString())).keySet();
	}

	public static InputStream getInputStream(EmiResource resource) {
		try {
			return resource.getInputStream();
		} catch (Exception e) {
			return null;
		}
	}

	/*public static BannerPattern.Patterns addRandomBanner(BannerPattern.Patterns patterns, Random random) {
		return patterns.add(Registry.BANNER_PATTERN.getEntry(random.nextInt(Registry.BANNER_PATTERN.size())).get(),
			DyeColor.values()[random.nextInt(DyeColor.values().length)]);
	}

	public static boolean canTallFlowerDuplicate(DoublePlantBlock tallFlowerBlock) {
		try {
			return tallFlowerBlock.method_6460()
		} catch(Exception e) {
			return false;
		}
	}

	public static void upload(VertexBuffer vb, BufferBuilder bldr) {
		vb.bind();
		vb.upload(bldr.end());
	}

	public static void setShader(VertexBuffer buf, Matrix4f mat) {
		buf.bind();
		buf.draw(mat, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
	}

	public static List<BakedQuad> getQuads(BakedModel model) {
		return model.getQuads(null, null, RANDOM);
	}

	public static void draw(BufferBuilder bufferBuilder) {
		BufferRenderer.drawWithShader(bufferBuilder.end());
	}*/

	public static int getGuiScale(MinecraftClient client) {
		return new Window(client, client.width, client.height).getScaleFactor();
	}

	public static void setPositionTexShader() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static void setPositionColorTexShader() {
		GL11.glEnable(GL11.GL_TEXTURE_2D);
	}

	public static FMLControlledNamespacedRegistry<Item> getItemRegistry() {
		return GameData.getItemRegistry();
	}

	public static FMLControlledNamespacedRegistry<Block> getBlockRegistry() {
		return GameData.getBlockRegistry();
	}

	public static Map<String, Fluid> getFluidRegistry() {
		return FluidRegistry.getRegisteredFluids();
	}

	public static List<Item> getAllItems() {
		List<Item> items = new ArrayList<>();
		((Iterable<Item>) getItemRegistry()).forEach(items::add);
		return items;
	}

	public static List<Fluid> getAllFluids() {
		return new ArrayList<>(getFluidRegistry().values());
	}

	public static ButtonWidget newButton(int x, int y, int w, int h, Text name, Consumer<ButtonWidget> action, ButtonManager manager) {
		return new ButtonWidget(manager.register(action), x, y, w, h, name.asFormattedString());
	}

	public static void focus(TextFieldWidget widget, boolean focused) {
		widget.setFocused(focused);
	}

	public static Stream<Item> getDisabledItems() {
		return Stream.empty();
	}

	public static Identifier getId(Object recipe) {
		return RecipeIdUtil.getId(recipe);
	}

	public static Comparison compareStrict() {
		return Comparison.compareNbt();
	}

	public static NbtCompound emptyExtraData() {
		return null;
	}

	public static Identifier id(String id) {
		return new Identifier(id);
	}

	public static Identifier id(String namespace, String path) {
		return new Identifier(namespace, path);
	}
}
