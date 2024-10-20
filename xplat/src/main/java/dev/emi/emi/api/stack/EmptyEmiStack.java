package dev.emi.emi.api.stack;

import java.util.List;

import dev.emi.emi.backport.ItemKey;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import org.jetbrains.annotations.ApiStatus;

import dev.emi.emi.EmiPort;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@ApiStatus.Internal
public class EmptyEmiStack extends EmiStack {
	private static final Identifier ID = EmiPort.id("emi", "empty");
	private static final ItemKey EMPTY_ITEM = new ItemKey(null, 0);

	@Override
	public EmiStack getRemainder() {
		return EMPTY;
	}

	@Override
	public List<EmiStack> getEmiStacks() {
		return List.of(EMPTY);
	}

	@Override
	public EmiStack setRemainder(EmiStack stack) {
		throw new UnsupportedOperationException("Cannot mutate an empty stack");
	}

	@Override
	public EmiStack copy() {
		return EMPTY;
	}
	
	public EmiStack setAmount(long amount) {
		return this;
	}
	
	public EmiStack setChance(float chance) {
		return this;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public NbtCompound getNbt() {
		return null;
	}

	@Override
	public Object getKey() {
		return EMPTY_ITEM;
	}

	@Override
	public ItemStack getItemStack() {
		return null;
	}

	@Override
	public Identifier getId() {
		return ID;
	}

	@Override
	public boolean isEqual(EmiStack stack) {
		return stack == EMPTY;
	}

	@Override
	public void render(MatrixStack matrices, int x, int y, float delta, int flags) {
	}

	@Override
	public List<Text> getTooltipText() {
		return List.of();
	}

	@Override
	public List<TooltipComponent> getTooltip() {
		return List.of();
	}

	@Override
	public Text getName() {
		return EmiPort.literal("");
	}

	static class EmptyEntry {
	}
}