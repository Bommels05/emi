package dev.emi.emi.platform.forge.handler;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dev.emi.emi.network.FillRecipeC2SPacket;

public class FillRecipeC2SPacketHandler implements IMessageHandler<FillRecipeC2SPacket, IMessage> {

    @Override
    public IMessage onMessage(FillRecipeC2SPacket packet, MessageContext context) {
        packet.apply(context.getServerHandler().player);
        return null;
    }
}
