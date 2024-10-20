package dev.emi.emi.network;

import java.util.List;
import java.util.function.Consumer;

import dev.emi.emi.EmiUtil;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.util.PacketByteBuf;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.Lists;

import dev.emi.emi.runtime.EmiLog;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Identifier;

public class FillRecipeC2SPacket implements EmiPacket {
	private int syncId;
	private int action;
	private List<Integer> slots, crafting;
	private int output;
	private List<ItemStack> stacks;

	public FillRecipeC2SPacket() {
		//for netty reading
	}

	public FillRecipeC2SPacket(ScreenHandler handler, int action, List<Slot> slots, List<Slot> crafting, @Nullable Slot output, List<ItemStack> stacks) {
		this.syncId = handler.syncId;
		this.action = action;
		this.slots = slots.stream().map(s -> s == null ? -1 : s.id).toList();
		this.crafting = crafting.stream().map(s -> s == null ? -1 : s.id).toList();
		this.output = output == null ? -1 : output.id;
		this.stacks = stacks;
	}

	public void read(PacketByteBuf buf) {
		syncId = buf.readInt();
		action = buf.readByte();
		slots = parseCompressedSlots(buf);
		crafting = Lists.newArrayList();
		int craftingSize = buf.readVarInt();
		for (int i = 0; i < craftingSize; i++) {
			int s = buf.readVarInt();
			crafting.add(s);
		}
		if (buf.readBoolean()) {
			output = buf.readVarInt();
		} else {
			output = -1;
		}
		int size = buf.readVarInt();
		stacks = Lists.newArrayList();
		for (int i = 0; i < size; i++) {
			stacks.add(buf.readItemStack());
		}
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeInt(syncId);
		buf.writeByte(action);
		writeCompressedSlots(slots, buf);
		buf.writeVarInt(crafting.size());
		for (Integer s : crafting) {
			buf.writeVarInt(s);
		}
		if (output != -1) {
			buf.writeBoolean(true);
			buf.writeVarInt(output);
		} else {
			buf.writeBoolean(false);
		}
		buf.writeVarInt(stacks.size());
		for (ItemStack stack : stacks) {
			buf.writeItemStack(stack);
		}
	}

	@Override
	public void apply(PlayerEntity player) {
		if (slots == null || crafting == null) {
			EmiLog.error("Client requested fill but passed input and crafting slot information was invalid, aborting");
			return;
		}
		ScreenHandler handler = player.openScreenHandler;
		if (handler == null || handler.syncId != syncId) {
			EmiLog.warn("Client requested fill but screen handler has changed, aborting");
			return;
		}
		List<Slot> slots = Lists.newArrayList();
		List<Slot> crafting = Lists.newArrayList();
		Slot output = null;
		for (int i : this.slots) {
			if (i < 0 || i >= handler.slots.size()) {
				EmiLog.error("Client requested fill but passed input slots don't exist, aborting");
				return;
			}
			slots.add(handler.getSlot(i));
		}
		for (int i : this.crafting) {
			if (i >= 0 && i < handler.slots.size()) {
				crafting.add(handler.getSlot(i));
			} else {
				crafting.add(null);
			}
		}
		if (this.output != -1) {
			if (this.output >= 0 && this.output < handler.slots.size()) {
				output = handler.getSlot(this.output);
			}
		}
		if (crafting.size() >= stacks.size()) {
			List<ItemStack> rubble = Lists.newArrayList();
			for (int i = 0; i < crafting.size(); i++) {
				Slot s = crafting.get(i);
				if (s != null && s.canTakeItems(player) && s.getStack() != null) {
					rubble.add(s.getStack().copy());
					s.setStack(null);
				}
			}
			try {	
				for (int i = 0; i < stacks.size(); i++) {
					ItemStack stack = stacks.get(i);
					if (stack == null) {
						continue;
					}
					int gotten = grabMatching(player, slots, rubble, crafting, stack);
					if (gotten != stack.count) {
						if (gotten > 0) {
							stack.count = gotten;
							EmiUtil.offerOrDrop(player, stack);
						}
						return;
					} else {
						Slot s = crafting.get(i);
						if (s != null && s.canInsert(stack) && stack.count <= s.getMaxStackAmount()) {
							s.setStack(stack);
						} else {
							EmiUtil.offerOrDrop(player, stack);
						}
					}
				}
				if (output != null) {
					if (action == 1) {
						handler.onSlotClick(output.id, 0, 0, player);
					} else if (action == 2) {
						handler.onSlotClick(output.id, 0, 1, player);
					}
				}
			} finally {
				for (ItemStack stack : rubble) {
					EmiUtil.offerOrDrop(player, stack);
				}
			}
		}
	}

	private static List<Integer> parseCompressedSlots(PacketByteBuf buf) {
		List<Integer> list = Lists.newArrayList();
		int amount = buf.readVarInt();
		for (int i = 0; i < amount; i++) {
			int low = buf.readVarInt();
			int high = buf.readVarInt();
			if (low < 0) {
				return null;
			}
			for (int j = low; j <= high; j++) {
				list.add(j);
			}
		}
		return list;
	}
	
	private static void writeCompressedSlots(List<Integer> list, PacketByteBuf buf) {
		List<Consumer<PacketByteBuf>> postWrite = Lists.newArrayList();
		int groups = 0;
		int i = 0;
		while (i < list.size()) {
			groups++;
			int start = i;
			int startValue = list.get(start);
			while (i < list.size() && i - start == list.get(i) - startValue) {
				i++;
			}
			int end = i - 1;
			postWrite.add(b -> {
				b.writeVarInt(startValue);
				b.writeVarInt(list.get(end));
			});
		}
		buf.writeVarInt(groups);
		for (Consumer<PacketByteBuf> consumer : postWrite) {
			consumer.accept(buf);
		}
	}

	private static int grabMatching(PlayerEntity player, List<Slot> slots, List<ItemStack> rubble, List<Slot> crafting, ItemStack stack) {
		int amount = stack.count;
		int grabbed = 0;
		for (int i = 0; i < rubble.size(); i++) {
			if (grabbed >= amount) {
				return grabbed;
			}
			ItemStack r = rubble.get(i);
			if (EmiUtil.canCombineIgnoreCount(stack, r)) {
				int wanted = amount - grabbed;
				if (r.count <= wanted) {
					grabbed += r.count;
					rubble.remove(i);
					i--;
				} else {
					grabbed = amount;
					r.count = r.count - wanted;
				}
			}
		}
		for (Slot s : slots) {
			if (grabbed >= amount) {
				return grabbed;
			}
			if (crafting.contains(s) || !s.canTakeItems(player)) {
				continue;
			}
			ItemStack st = s.getStack();
			if (EmiUtil.canCombineIgnoreCount(stack, st)) {
				int wanted = amount - grabbed;
				if (st.count <= wanted) {
					grabbed += st.count;
					s.setStack(null);
				} else {
					grabbed = amount;
					st.count = st.count - wanted;
				}
			}
		}
		return grabbed;
	}

	@Override
	public Identifier getId() {
		return EmiNetwork.FILL_RECIPE;
	}
}
