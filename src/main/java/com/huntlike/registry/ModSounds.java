package com.huntlike.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import com.huntlike.HuntLike;

/** HuntLike's own sound events. They play vanilla sound files (see assets/huntlike/sounds.json). */
public final class ModSounds {
	private ModSounds() {
	}

	/** Aurochs calling its herd: a cow's moo. */
	public static final SoundEvent BOSS_MOO = register("boss.moo");
	/** Epicyon Haydeni calling its pack: a wolf's howl. */
	public static final SoundEvent BOSS_HOWL = register("boss.howl");

	private static SoundEvent register(String name) {
		Identifier id = HuntLike.id(name);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void initialize() {
	}
}
