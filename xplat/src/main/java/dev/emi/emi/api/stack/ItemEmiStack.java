package dev.emi.emi.api.stack;

import java.util.List;

import dev.emi.emi.backport.ItemKey;
import net.minecraft.item.Item;
import net.minecraftforge.oredict.OreDictionary;
import org.jetbrains.annotations.ApiStatus;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiRenderHelper;
import dev.emi.emi.api.render.EmiRender;
import dev.emi.emi.platform.EmiAgnos;
import dev.emi.emi.runtime.EmiDrawContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

@ApiStatus.Internal
public class ItemEmiStack extends EmiStack {
	private static final MinecraftClient client = MinecraftClient.getInstance();

	private final Item item;
	private final NbtCompound nbt;
	private final int meta;

	private boolean unbatchable;

	public ItemEmiStack(ItemStack stack) {
		this(stack, stack.count, stack.getData());
	}

	public ItemEmiStack(ItemStack stack, long amount) {
		this(stack.getItem(), stack.getNbt(), amount, stack.getData());
	}

	public ItemEmiStack(ItemStack stack, long amount, int meta) {
		this(stack.getItem(), stack.getNbt(), amount, meta);
	}

	public ItemEmiStack(Item item, NbtCompound nbt, long amount, int meta) {
		this.item = item;
		this.nbt = nbt != null ? (NbtCompound) nbt.copy() : null;
		this.amount = amount;
		this.meta = meta;
		if (meta == OreDictionary.WILDCARD_VALUE) {
			throw new IllegalArgumentException("This EmiStack(" + this + ") should be a Tag-Ingredient");
		}
	}

	@Override
	public ItemStack getItemStack() {
		ItemStack stack = new ItemStack(this.item, (int) this.amount, this.meta);
		if (this.nbt != null) {
			stack.setNbt(this.nbt);
		}
		return stack;
	}

	@Override
	public EmiStack copy() {
		EmiStack e = new ItemEmiStack(item, nbt, amount, meta);
		e.setChance(chance);
		e.setRemainder(getRemainder().copy());
		e.comparison = comparison;
		return e;
	}

	@Override
	public boolean isEmpty() {
		return amount == 0 || item == null;
	}

	@Override
	public NbtCompound getNbt() {
		return nbt;
	}

	@Override
	public Object getKey() {
		return new ItemKey(item, meta);
	}

	@Override
	public Identifier getId() {
		return new Identifier(EmiPort.getItemRegistry().getId((Object) item) + (meta == 0 ? "" : "_" + meta));
	}

	@Override
	public boolean isEqual(EmiStack stack) {
		if (stack instanceof ItemEmiStack i && this.meta != i.meta) {
			return false;
		}
		return super.isEqual(stack);
	}

	@Override
	public boolean isEqual(EmiStack stack, Comparison comparison) {
		if (stack instanceof ItemEmiStack i && this.meta != i.meta) {
			return false;
		}
		return super.isEqual(stack, comparison);
	}

	@Override
	public void render(MatrixStack matrices, int x, int y, float delta, int flags) {
		EmiDrawContext context = EmiDrawContext.wrap(matrices);
		ItemStack stack = getItemStack();
		if ((flags & RENDER_ICON) != 0) {
			DiffuseLighting.enable();
			GL11.glEnable(GL11.GL_LIGHTING);
			GL11.glEnable(GL11.GL_DEPTH_TEST);
			MatrixStack view = RenderSystem.getModelViewStack();
			view.push();
			RenderSystem.applyModelViewMatrix();
			ItemRenderer itemRenderer = EmiRenderHelper.ITEM_RENDERER;
			float zOffset = itemRenderer.zOffset;
			itemRenderer.zOffset = 0;
			itemRenderer.method_6920(client.textRenderer, client.getTextureManager(), stack, x, y);
			itemRenderer.method_1549(client.textRenderer, client.getTextureManager(), stack, x, y);
			itemRenderer.zOffset = zOffset;
			view.pop();
			RenderSystem.applyModelViewMatrix();
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
			GL11.glDisable(GL11.GL_LIGHTING);
			DiffuseLighting.disable();
		}
		if ((flags & RENDER_AMOUNT) != 0) {
			String count = "";
			if (amount != 1) {
				count += amount;
			}
			EmiRenderHelper.renderAmount(context, x, y, EmiPort.literal(count));
		}
		if ((flags & RENDER_REMAINDER) != 0) {
			EmiRender.renderRemainderIcon(this, context.raw(), x, y);
		}
	}
	
	/*@Override
	public boolean isSideLit() {
		return client.getItemRenderer().getModel(getItemStack(), null, null, 0).isSideLit();
	}
	
	@Override
	public boolean isUnbatchable() {
		ItemStack stack = getItemStack();
		return unbatchable || stack.hasGlint() || stack.isDamaged() || !EmiAgnos.canBatch(stack)
			|| client.getItemRenderer().getModel(getItemStack(), null, null, 0).isBuiltin();
	}
	
	@Override
	public void setUnbatchable() {
		this.unbatchable = true;
	}
	
	@Override
	public void renderForBatch(VertexConsumerProvider vcp, MatrixStack matrices, int x, int y, int z, float delta) {
		EmiDrawContext context = EmiDrawContext.wrap(matrices);
		ItemStack stack = getItemStack();
		ItemRenderer ir = client.getItemRenderer();
		BakedModel model = ir.getModel(stack, null, null, 0);
		context.push();
		try {
			context.matrices().translate(x, y, 100.0f + z + (model.hasDepth() ? 50 : 0));
			context.matrices().translate(8.0, 8.0, 0.0);
			context.matrices().scale(16.0f, 16.0f, 16.0f);
			ir.renderItem(stack, ModelTransformation.Mode.GUI, false, context.raw(), vcp, LightmapTextureManager.MAX_LIGHT_COORDINATE, OverlayTexture.DEFAULT_UV, model);
		} finally {
			context.pop();
		}
	}*/

	@Override
	public List<Text> getTooltipText() {
		return ((List<String>) getItemStack().getTooltip(client.field_3805, false)).stream().map(EmiPort::literal).toList();
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		ItemStack stack = getItemStack();
		List<TooltipComponent> list = Lists.newArrayList();
		if (!isEmpty()) {
			list.addAll(EmiAgnos.getItemTooltip(stack));
			//String namespace = EmiPort.getItemRegistry().getId(stack.getItem()).getNamespace();
			//String mod = EmiUtil.getModName(namespace);
			//list.add(TooltipComponent.of(EmiLang.literal(mod, Formatting.BLUE, Formatting.ITALIC)));
			list.addAll(super.getTooltip());
		}
		return list;
	}

	@Override
	public Text getName() {
		if (isEmpty()) {
			return EmiPort.literal("");
		}
		return EmiPort.literal(getItemStack().getCustomName());
	}

	static class ItemEntry {
	}
}