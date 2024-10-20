package dev.emi.emi.platform.forge.handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.emi.emi.network.CreateItemC2SPacket;
import dev.emi.emi.network.EmiChessPacket;

public class EmiChessC2SPacketHandler implements IMessageHandler<EmiChessPacket.C2S, IMessage> {

    @Override
    public IMessage onMessage(EmiChessPacket.C2S packet, MessageContext context) {
        packet.apply(context.getServerHandler().player);
        return null;
    }
}
