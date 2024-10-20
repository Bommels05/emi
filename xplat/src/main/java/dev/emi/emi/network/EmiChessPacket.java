package dev.emi.emi.network;

import java.util.UUID;

import dev.emi.emi.chess.EmiChess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.PacketByteBuf;

public abstract class EmiChessPacket implements EmiPacket {
	protected UUID uuid;
	protected byte type, start, end;

	public EmiChessPacket() {
		//for netty reading
	}

	public EmiChessPacket(UUID uuid, byte type, byte start, byte end) {
		this.uuid = uuid;
		this.type = type;
		this.start = start;
		this.end = end;
	}

	public void read(PacketByteBuf buf) {
		this.uuid = new UUID(buf.readLong(), buf.readLong());
		this.type = buf.readByte();
		this.start = buf.readByte();
		this.end = buf.readByte();
	}

	@Override
	public void write(PacketByteBuf buf) {
		buf.writeLong(uuid.getMostSignificantBits());
		buf.writeLong(uuid.getLeastSignificantBits());
		buf.writeByte(type);
		buf.writeByte(start);
		buf.writeByte(end);
	}

	@Override
	public Identifier getId() {
		return EmiNetwork.CHESS;
	}

	public static class S2C extends EmiChessPacket {

		public S2C() {
			//for netty reading
		}

		public S2C(UUID uuid, byte type, byte start, byte end) {
			super(uuid, type, start, end);
		}

		@Override
		public void apply(PlayerEntity player) {
			EmiChess.receiveNetwork(uuid, type, start, end);
		}
	}

	public static class C2S extends EmiChessPacket {

		public C2S() {
			//for netty reading
		}

		public C2S(UUID uuid, byte type, byte start, byte end) {
			super(uuid, type, start, end);
		}

		@Override
		public void apply(PlayerEntity player) {
			PlayerEntity opponent = player.getWorld().getPlayerByUuid(uuid);
			if (opponent instanceof ServerPlayerEntity spe) {
				EmiNetwork.sendToClient(spe, new EmiChessPacket.S2C(player.getUuid(), type, start, end));
			}
		}
	}
}
