package dev.emi.emi.network;

import dev.emi.emi.EmiUtil;
import dev.emi.emi.runtime.EmiLog;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class CreateItemC2SPacket implements EmiPacket {
	private int mode;
	private ItemStack stack;

	public CreateItemC2SPacket() {
		//for netty reading
	}

	public CreateItemC2SPacket(int mode, ItemStack stack) {
		this.mode = mode;
		this.stack = stack;
	}

	public void read(PacketByteBuf buf) {
		this.mode = buf.readByte();
		this.stack = buf.readItemStack();
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeByte(mode);
		buf.writeItemStack(stack);
	}

	@Override
	public void apply(PlayerEntity player) {
		if ((player.canUseCommand(2, "give") || player.abilities.creativeMode) && player.openScreenHandler != null) {
			if (stack == null) {
				if (mode == 1 && player.inventory.getCursorStack() != null) {
					EmiLog.info(player.getTranslationKey() + " deleted " + player.inventory.getCursorStack());
					player.inventory.setCursorStack(stack);
				}
			} else {
				EmiLog.info(player.getTranslationKey() + " cheated in " + stack);
				if (mode == 0) {
					EmiUtil.offerOrDrop(player, stack);
				} else if (mode == 1) {
					player.inventory.setCursorStack(stack);
				}
			}
		}
	}

	@Override
	public Identifier getId() {
		return EmiNetwork.CREATE_ITEM;
	}
}
