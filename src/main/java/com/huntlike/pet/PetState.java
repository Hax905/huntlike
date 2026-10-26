package com.huntlike.pet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.ItemStack;

/**
 * A player's slime pet, kept on the player (one pet per player). The entities only carry the owner tag and their size;
 * everything that must survive splitting, dying and reviving (upgrades, heat, the chest's storage) lives here.
 */
public class PetState {
	public static final int NONE = 0;
	/** One entity: the medium or big pet. */
	public static final int WHOLE = 1;
	/** Split into smaller pieces after being killed. */
	public static final int SPLIT = 2;
	/** Every piece died: only the Slimy Core is left. */
	public static final int CORE = 3;

	public static final int STORAGE = 54;
	public static final int PAGE = 27;

	/** Which pet this is (the entities and its Slimy Core carry it). */
	public UUID id = UUID.randomUUID();
	public String name = "";
	public int status = NONE;
	/** Grown into a big slime (a magma pet is always big). */
	public boolean big;
	public boolean ionic;
	public boolean palladium;
	public boolean magma;
	public int heat;
	/** Thermal Slimeballs fed since the Ionic Liquid, towards growing big. */
	public int growFeeds;
	public boolean sitting;
	public boolean chest;
	/** The storage page last opened (0 or 1). */
	public int page;
	public NonNullList<ItemStack> storage = NonNullList.withSize(STORAGE, ItemStack.EMPTY);
	public List<UUID> pieces = new ArrayList<>();
	/** Game time a piece last took damage (regrowing waits for quiet). */
	public long lastDamage;
	/** Changes on every tame and revive; pieces from an older pet remove themselves. */
	public int generation;

	public static final Codec<PetState> CODEC = RecordCodecBuilder.create(i -> i.group(
			UUIDUtil.CODEC.optionalFieldOf("id").forGetter(s -> Optional.of(s.id)),
			Codec.STRING.optionalFieldOf("name", "").forGetter(s -> s.name),
			Codec.INT.optionalFieldOf("status", NONE).forGetter(s -> s.status),
			Codec.BOOL.optionalFieldOf("big", false).forGetter(s -> s.big),
			Codec.BOOL.optionalFieldOf("ionic", false).forGetter(s -> s.ionic),
			Codec.BOOL.optionalFieldOf("palladium", false).forGetter(s -> s.palladium),
			Codec.BOOL.optionalFieldOf("magma", false).forGetter(s -> s.magma),
			Codec.INT.optionalFieldOf("heat", 0).forGetter(s -> s.heat),
			Codec.INT.optionalFieldOf("grow_feeds", 0).forGetter(s -> s.growFeeds),
			Codec.BOOL.optionalFieldOf("sitting", false).forGetter(s -> s.sitting),
			Codec.BOOL.optionalFieldOf("chest", false).forGetter(s -> s.chest),
			Codec.INT.optionalFieldOf("page", 0).forGetter(s -> s.page),
			ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("storage", List.of()).forGetter(s -> s.storage),
			UUIDUtil.CODEC.listOf().optionalFieldOf("pieces", List.of()).forGetter(s -> s.pieces),
			Codec.LONG.optionalFieldOf("last_damage", 0L).forGetter(s -> s.lastDamage),
			Codec.INT.optionalFieldOf("generation", 0).forGetter(s -> s.generation))
			.apply(i, PetState::of));

	private static PetState of(Optional<UUID> id, String name, int status, boolean big, boolean ionic, boolean palladium, boolean magma, int heat,
			int growFeeds, boolean sitting, boolean chest, int page, List<ItemStack> storage, List<UUID> pieces, long lastDamage, int generation) {
		PetState s = new PetState();
		s.id = id.orElseGet(UUID::randomUUID);
		s.name = name;
		s.status = status;
		s.big = big;
		s.ionic = ionic;
		s.palladium = palladium;
		s.magma = magma;
		s.heat = heat;
		s.growFeeds = growFeeds;
		s.sitting = sitting;
		s.chest = chest;
		s.page = page;
		for (int k = 0; k < Math.min(STORAGE, storage.size()); k++) {
			s.storage.set(k, storage.get(k));
		}
		s.pieces = new ArrayList<>(pieces);
		s.lastDamage = lastDamage;
		s.generation = generation;
		return s;
	}

	public boolean alive() {
		return status == WHOLE || status == SPLIT;
	}

	/** Health of the whole pet: medium 20 (40 with Ionic Liquid), big 60, magma cube 80. */
	public float fullHealth() {
		if (magma) {
			return 80.0F;
		}
		if (big) {
			return 60.0F;
		}
		return ionic ? 40.0F : 20.0F;
	}

	/** Vanilla size of the whole pet: 2 (medium) or 4 (big). */
	public int fullSize() {
		return big || magma ? 4 : 2;
	}

	/** Health of one piece of a given size: half per split (60 -> 30 -> 15). */
	public float health(int size) {
		float health = fullHealth();
		for (int s = fullSize(); s > size; s /= 2) {
			health /= 2.0F;
		}
		return health;
	}

	public boolean storageEmpty() {
		for (ItemStack stack : storage) {
			if (!stack.isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
