package dev.emi.emi.network;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.bom.BoM;
import dev.emi.emi.registry.EmiCommands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class CommandS2CPacket implements EmiPacket {
	private byte type;
	private Identifier id;

	public CommandS2CPacket() {
		//for netty reading
	}

	public CommandS2CPacket(byte type, Identifier id) {
		this.type = type;
		this.id = id;
	}

	public void read(PacketByteBuf buf) {
		type = buf.readByte();
		if (type == EmiCommands.VIEW_RECIPE || type == EmiCommands.TREE_GOAL || type == EmiCommands.TREE_RESOLUTION) {
			id = new Identifier(buf.readString(32767));
		} else {
			id = null;
		}
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeByte(type);
		if (type == EmiCommands.VIEW_RECIPE || type == EmiCommands.TREE_GOAL || type == EmiCommands.TREE_RESOLUTION) {
			buf.method_7423(id.toString());
		}
	}

	@Override
	public void apply(PlayerEntity player) {
		if (type == EmiCommands.VIEW_RECIPE) {
			EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
			if (recipe != null) {
				EmiApi.displayRecipe(recipe);
			}
		} else if (type == EmiCommands.VIEW_TREE) {
			EmiApi.viewRecipeTree();
		} else if (type == EmiCommands.TREE_GOAL) {
			EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
			if (recipe != null) {
				BoM.setGoal(recipe);
			}
		} else if (type == EmiCommands.TREE_RESOLUTION) {
			EmiRecipe recipe = EmiApi.getRecipeManager().getRecipe(id);
			if (recipe != null && BoM.tree != null) {
				for (EmiStack stack : recipe.getOutputs()) {
					BoM.tree.addResolution(stack, recipe);
				}
			}
		}
	}

	@Override
	public Identifier getId() {
		return EmiNetwork.COMMAND;
	}
}
