package dev.emi.emi.network;

import dev.emi.emi.platform.EmiClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public class PingS2CPacket implements EmiPacket {

	public PingS2CPacket() {
	}

	public void read(PacketByteBuf buf) {
	}

	@Override
	public void write(PacketByteBuf buf) {
	}

	@Override
	public void apply(PlayerEntity player) {
		EmiClient.onServer = true;
	}

	@Override
	public Identifier getId() {
		return EmiNetwork.PING;
	}
}
