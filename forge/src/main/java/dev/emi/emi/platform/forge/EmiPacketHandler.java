package dev.emi.emi.platform.forge;

import java.util.function.BiConsumer;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.CreateItemC2SPacket;
import dev.emi.emi.network.EmiChessPacket;
import dev.emi.emi.network.FillRecipeC2SPacket;
import dev.emi.emi.network.PingS2CPacket;
import dev.emi.emi.platform.forge.handler.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;

public class EmiPacketHandler {
	public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel("emi");
	
	public static void init() {
		//Handlers all need to be separate classes so they don't have the same name
		int i = 0;
		CHANNEL.registerMessage(new FillRecipeC2SPacketHandler(), FillRecipeC2SPacket.class,
			i++, Side.SERVER);
		CHANNEL.registerMessage(new CreateItemC2SPacketHandler(), CreateItemC2SPacket.class,
			i++, Side.SERVER);
		CHANNEL.registerMessage(new EmiChessC2SPacketHandler(), EmiChessPacket.C2S.class,
			i++, Side.SERVER);

		CHANNEL.registerMessage(new PingS2CPacketHandler(), PingS2CPacket.class,
			i++, Side.CLIENT);
		CHANNEL.registerMessage(new CommandS2CPacketHandler(), CommandS2CPacket.class,
			i++, Side.CLIENT);
		CHANNEL.registerMessage(new EmiChessS2CPacketHandler(), EmiChessPacket.S2C.class,
			i++, Side.CLIENT);
	}
}
