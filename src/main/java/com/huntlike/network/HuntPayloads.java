package com.huntlike.network;

import io.netty.buffer.ByteBuf;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

import com.huntlike.HuntLike;

/** Packets between the server and the Hunt menu. */
public final class HuntPayloads {
	private HuntPayloads() {
	}

	/** Server to client: the player's Hunt data plus a few live values for the menu. */
	public record Sync(CompoundTag tag) implements CustomPacketPayload {
		public static final Type<Sync> TYPE = new Type<>(HuntLike.id("sync"));
		public static final StreamCodec<ByteBuf, Sync> CODEC = ByteBufCodecs.TRUSTED_COMPOUND_TAG.map(Sync::new, Sync::tag);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Server to client: open the Hunt menu. */
	public record OpenMenu(int tab) implements CustomPacketPayload {
		public static final Type<OpenMenu> TYPE = new Type<>(HuntLike.id("open_menu"));
		public static final StreamCodec<ByteBuf, OpenMenu> CODEC = ByteBufCodecs.VAR_INT.map(OpenMenu::new, OpenMenu::tab);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	/** Client to server: a menu action ("select", "start", "cancel", "buy_tokens", "refresh"). */
	public record Action(String action, String hunt, int value) implements CustomPacketPayload {
		public static final Type<Action> TYPE = new Type<>(HuntLike.id("action"));
		public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
				ByteBufCodecs.STRING_UTF8, Action::action,
				ByteBufCodecs.STRING_UTF8, Action::hunt,
				ByteBufCodecs.VAR_INT, Action::value,
				Action::new);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return TYPE;
		}
	}

	public static void initialize() {
		PayloadTypeRegistry.clientboundPlay().register(Sync.TYPE, Sync.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(OpenMenu.TYPE, OpenMenu.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(Action.TYPE, Action.CODEC);
	}
}
