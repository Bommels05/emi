package dev.emi.emi.platform.forge.handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.emi.emi.network.EmiChessPacket;
import dev.emi.emi.network.PingS2CPacket;
import net.minecraft.client.MinecraftClient;

public class EmiChessS2CPacketHandler implements IMessageHandler<EmiChessPacket.S2C, IMessage> {

    @Override
    public IMessage onMessage(EmiChessPacket.S2C packet, MessageContext context) {
        packet.apply(MinecraftClient.getInstance().field_3805);
        return null;
    }
}
