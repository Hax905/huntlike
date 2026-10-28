package com.huntlike.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import com.huntlike.hunt.HuntData;

/** The latest Hunt data the server sent this client. */
public final class ClientHuntState {
	private ClientHuntState() {
	}

	public static HuntData data = new HuntData();
	public static boolean peaceful;
	/** Towernaar's Final Chapter: 0 none, 1 waiting for an enderman kill, 2 fighting. */
	public static int towernaarPhase;
	/** The Interlude stage being fought (0 = none), its time left, and its living bodies (they glow for the owner). */
	public static int interludeStage;
	public static int interludeTicksLeft;
	public static int[] interludeIds = new int[0];
	public static int bossTicksLeft;
	public static int bossEntityId = -1;
	/** Every living body of a Cluster boss (empty for Animal bosses). */
	public static int[] bossEntityIds = new int[0];
	/** Spiritual Catalyst highlight: entity id and the client tick it ends at. */
	/** Entities highlighted by the Spiritual Catalyst or Compass: id -> game time the glow ends. */
	public static final java.util.Map<Integer, Long> highlights = new java.util.HashMap<>();
	public static List<String> huntable = new ArrayList<>();

	public static void update(CompoundTag tag) {
		data = HuntData.fromTag(tag);
		peaceful = tag.getBooleanOr("peaceful", false);
		towernaarPhase = tag.getIntOr("towernaarPhase", 0);
		interludeStage = tag.getIntOr("interludeStage", 0);
		interludeTicksLeft = tag.getIntOr("interludeTicksLeft", 0);
		interludeIds = tag.getIntArray("interludeEntities").orElse(new int[0]);
		bossTicksLeft = tag.getIntOr("bossTicksLeft", 0);
		bossEntityId = tag.getIntOr("bossEntity", -1);
		bossEntityIds = tag.getIntArray("bossEntities").orElse(new int[0]);
		List<String> names = new ArrayList<>();
		ListTag list = tag.getListOrEmpty("huntable");
		for (int i = 0; i < list.size(); i++) {
			names.add(list.getStringOr(i, ""));
		}
		huntable = names;
	}
}
