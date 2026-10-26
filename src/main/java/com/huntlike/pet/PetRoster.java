package com.huntlike.pet;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.serialization.Codec;

/** Every slime pet a player has tamed. Any number can be tamed; only one follows at a time, the others sit. */
public class PetRoster {
	public static final Codec<PetRoster> CODEC = PetState.CODEC.listOf().xmap(PetRoster::new, roster -> roster.pets);

	public final List<PetState> pets;

	public PetRoster() {
		this(List.of());
	}

	public PetRoster(List<PetState> pets) {
		this.pets = new ArrayList<>(pets);
	}

	public PetState get(UUID id) {
		if (id == null) {
			return null;
		}
		for (PetState pet : pets) {
			if (pet.id.equals(id)) {
				return pet;
			}
		}
		return null;
	}

	/** The pet tamed most recently, or null. */
	public PetState latest() {
		return pets.isEmpty() ? null : pets.get(pets.size() - 1);
	}
}
