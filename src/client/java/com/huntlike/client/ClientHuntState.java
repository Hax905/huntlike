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
	public static int bossTicksLeft;
	public static int bossEntityId = -1;
	public static List<String> huntable = new ArrayList<>();

	public static void update(CompoundTag tag) {
		data = HuntData.fromTag(tag);
		peaceful = tag.getBooleanOr("peaceful", false);
		bossTicksLeft = tag.getIntOr("bossTicksLeft", 0);
		bossEntityId = tag.getIntOr("bossEntity", -1);
		List<String> names = new ArrayList<>();
		ListTag list = tag.getListOrEmpty("huntable");
		for (int i = 0; i < list.size(); i++) {
			names.add(list.getStringOr(i, ""));
		}
		huntable = names;
	}
}
