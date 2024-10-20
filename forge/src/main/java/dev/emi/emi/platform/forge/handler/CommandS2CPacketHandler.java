package dev.emi.emi.platform.forge.handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.emi.emi.network.CommandS2CPacket;
import dev.emi.emi.network.PingS2CPacket;
import net.minecraft.client.MinecraftClient;

public class CommandS2CPacketHandler implements IMessageHandler<CommandS2CPacket, IMessage> {

    @Override
    public IMessage onMessage(CommandS2CPacket packet, MessageContext context) {
        packet.apply(MinecraftClient.getInstance().field_3805);
        return null;
    }
}
