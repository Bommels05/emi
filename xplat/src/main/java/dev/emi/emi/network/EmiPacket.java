package dev.emi.emi.network;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public interface EmiPacket extends IMessage {
	
	void write(PacketByteBuf buf);

	void read(PacketByteBuf buf);
	
	void apply(PlayerEntity player);

	Identifier getId();

	@Override
	default void fromBytes(ByteBuf byteBuf) {
		read(new PacketByteBuf(byteBuf));
	}

	@Override
	default void toBytes(ByteBuf byteBuf) {
		write(new PacketByteBuf(byteBuf));
	}
}
