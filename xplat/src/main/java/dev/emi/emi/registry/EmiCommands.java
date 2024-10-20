package dev.emi.emi.registry;

import net.minecraft.command.AbstractCommand;
import net.minecraft.command.Command;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.EmiNetwork;
import net.minecraft.util.Identifier;

public class EmiCommands {
	public static final byte VIEW_RECIPE = 0x01;
	public static final byte VIEW_TREE = 0x02;
	public static final byte TREE_GOAL = 0x11;
	public static final byte TREE_RESOLUTION = 0x12;
	
	public static void registerCommands(MinecraftServer server) {
		((CommandRegistry) server.getCommandManager()).registerCommand(new EmiCommand());
	}

	private static class EmiCommand extends AbstractCommand {

		@Override
		public String getCommandName() {
			return "emi";
		}

		@Override
		public String getUsageTranslationKey(CommandSource source) {
			return "commands.emi.usage";
		}

		@Override
		public void execute(CommandSource source, String[] args) {
			if (source instanceof ServerPlayerEntity player) {
				if (args.length == 3 || (args.length == 2 && args[1].equals("tree"))) {
					if (args[0].equals("view")) {
						if (args[1].equals("recipe")) {
							Identifier id = new Identifier(args[2]);
							send(player, VIEW_RECIPE, id);
						} else if (args[1].equals("tree")) {
							send(player, VIEW_TREE, null);
							return;
						}
					} else if (args[0].equals("tree")) {
						if (args[1].equals("goal")) {
							Identifier id = new Identifier(args[2]);
							send(player, TREE_GOAL, id);
						} else if (args[1].equals("resolution")) {
							Identifier id = new Identifier(args[2]);
							send(player, TREE_RESOLUTION, id);
						}
					}
				}
			}
			throw new IncorrectUsageException(getUsageTranslationKey(source));
		}

		@Override
		public int compareTo(@NotNull Object command) {
			return this.getCommandName().compareTo(((Command) command).getCommandName());
		}
	}

	private static void send(ServerPlayerEntity player, byte type, @Nullable Identifier id) {
		EmiNetwork.sendToClient(player, new CommandS2CPacket(type, id));
	}
}
