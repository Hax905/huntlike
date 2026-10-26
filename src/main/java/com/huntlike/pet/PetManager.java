package com.huntlike.pet;

import java.util.Comparator;
import java.util.List;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import com.huntlike.HuntLike;
import com.huntlike.entity.ModEntities;
import com.huntlike.hunt.ClusterFight;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;
import com.huntlike.hunt.SpiritLure;
import com.huntlike.registry.ModItems;

/**
 * The Cluster Hunt's slime pet: taming a wild small slime with Thermal Slimeballs, its upgrades (Ionic Liquid, growing big,
 * Corroded Palladium and heat into a Magma Cube), splitting when killed, regrowing, the Slimy Core and reviving.
 */
public final class PetManager {
	private PetManager() {
	}

	/** Every pet the player has tamed. */
	public static final AttachmentType<PetRoster> ROSTER = AttachmentRegistry.create(HuntLike.id("slime_pets"),
			builder -> builder.initializer(PetRoster::new).persistent(PetRoster.CODEC).copyOnDeath());
	/** v0.11.0 kept a single pet here; it moves into the roster the first time it's read. */
	private static final AttachmentType<PetState> LEGACY = AttachmentRegistry.create(HuntLike.id("slime_pet"),
			builder -> builder.persistent(PetState.CODEC).copyOnDeath());

	public static final double TAME_CHANCE = 0.05;
	public static final int TAME_GUARANTEED = 20;
	public static final double GROW_CHANCE = 0.04;
	public static final int GROW_GUARANTEED = 25;
	public static final int HEAT_GOAL = 100;
	/** A following pet that hasn't got closer to its owner for this long (while more than 6 blocks away) teleports. */
	public static final int STUCK_TICKS = 5 * 20;
	/** Health a slime ball heals (any piece, any form). */
	public static final float SLIME_BALL_HEAL = 4.0F;
	/** Quiet time before split pieces grow back: a minute for a medium pet, 45 seconds for a big one. */
	public static final int REGROW_MEDIUM = 60 * 20;
	public static final int REGROW_BIG = 45 * 20;
	private static final String FEEDS_PREFIX = "huntlike_feeds:";
	private static final int GRAY = 0xAAAAAA;
	private static final int HEAT = 0xFF8833;

	public static final List<String> ADJECTIVES = List.of("Funny", "Angry", "Wild", "Friendly", "Aggressive", "Slimy", "Slippery",
			"Warm", "Cold", "Green", "Cubical", "Square", "Lime", "Sweet", "Salty", "Wet", "Handsome", "Pretty", "Happy", "Good");

	public static void initialize() {
		UseEntityCallback.EVENT.register(PetManager::onUseEntity);
	}

	public static PetRoster roster(ServerPlayer player) {
		PetRoster roster = player.getAttachedOrCreate(ROSTER);
		PetState legacy = player.getAttached(LEGACY);
		if (legacy != null) {
			if (legacy.status != PetState.NONE) {
				legacy.id = LEGACY_ID;
				roster.pets.add(legacy);
			}
			player.removeAttached(LEGACY);
		}
		return roster;
	}

	/** Entities of a pet from v0.11.0 have no pet id tag: they belong to the migrated pet. */
	private static final java.util.UUID LEGACY_ID = new java.util.UUID(0x48756e744c696b65L, 0x50657431L);

	public static PetState pet(ServerPlayer player, java.util.UUID id) {
		return roster(player).get(id);
	}

	/** The pet tamed most recently (for tests and commands). */
	public static PetState latest(ServerPlayer player) {
		return roster(player).latest();
	}

	/** The state of the pet an entity belongs to, or null if that pet no longer exists. */
	public static PetState stateOf(ServerPlayer owner, PetCube cube) {
		java.util.UUID id = cube.petId();
		if (id == null) {
			id = LEGACY_ID;
			cube.addTag(PetCube.ID_PREFIX + id);
		}
		PetState state = pet(owner, id);
		return state == null || !state.alive() || cube.generation() != state.generation ? null : state;
	}

	/** This pet follows; every other pet of the owner that was following sits down (only one follows at a time). */
	public static void follow(ServerPlayer owner, PetState state) {
		state.sitting = false;
		int sat = 0;
		for (PetState other : roster(owner).pets) {
			if (other != state && other.alive() && !other.sitting) {
				other.sitting = true;
				sat++;
			}
		}
		if (sat > 0) {
			owner.sendSystemMessage(Messages.c(sat == 1 ? "Your other slime sits down to wait." : "Your other slimes sit down to wait.",
					GRAY));
		}
	}

	// ---------------------------------------------------------------- taming

	private static InteractionResult onUseEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hit) {
		if (player.isSpectator() || entity.getType() != EntityTypes.SLIME || !(entity instanceof Slime slime) || slime.getSize() != 1
				|| !player.getItemInHand(hand).is(ModItems.THERMAL_SLIMEBALL)) {
			return InteractionResult.PASS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			feedWild(serverPlayer, slime, player.getItemInHand(hand));
		}
		return InteractionResult.SUCCESS;
	}

	/** One Thermal Slimeball fed to a wild small slime: 5% to befriend it, guaranteed on the 20th. */
	public static boolean feedWild(ServerPlayer player, Slime slime, ItemStack stack) {
		if (ClusterFight.isBody(slime) || SpiritLure.isLured(slime)) {
			player.sendSystemMessage(Messages.c("This slime doesn't seem friendly. Find a wild one!", GRAY));
			return false;
		}
		stack.consume(1, player);
		ServerLevel level = (ServerLevel) slime.level();
		int feeds = feeds(slime) + 1;
		setFeeds(slime, feeds);
		if (feeds < TAME_GUARANTEED && player.getRandom().nextDouble() >= TAME_CHANCE) {
			particles(level, slime, ParticleTypes.SMOKE, 6);
			slime.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 1.2F);
			return false;
		}
		Vec3 pos = slime.position();
		float yaw = slime.getYRot();
		slime.discard();
		PetState pet = new PetState();
		pet.name = randomName(player.getRandom());
		pet.status = PetState.WHOLE;
		roster(player).pets.add(pet);
		follow(player, pet);
		PetCube cube = spawnPiece(level, player, pet, 2, pos, yaw);
		particles(level, cube, ParticleTypes.HEART, 7);
		level.playSound(null, cube.blockPosition(), SoundEvents.SLIME_JUMP, SoundSource.NEUTRAL, 1.0F, 1.4F);
		QuestManager.award(player, "cluster/pet_tame");
		return true;
	}

	public static String randomName(RandomSource random) {
		return ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " Cluster";
	}

	private static int feeds(Entity entity) {
		for (String tag : entity.entityTags()) {
			if (tag.startsWith(FEEDS_PREFIX)) {
				try {
					return Integer.parseInt(tag.substring(FEEDS_PREFIX.length()));
				} catch (NumberFormatException e) {
					return 0;
				}
			}
		}
		return 0;
	}

	private static void setFeeds(Entity entity, int feeds) {
		for (String tag : List.copyOf(entity.entityTags())) {
			if (tag.startsWith(FEEDS_PREFIX)) {
				entity.removeTag(tag);
			}
		}
		entity.addTag(FEEDS_PREFIX + feeds);
	}

	// ---------------------------------------------------------------- pieces

	/** Spawns one pet entity (the whole pet or a piece) of a vanilla size: 1 small, 2 medium, 4 big. */
	public static PetCube spawnPiece(ServerLevel level, ServerPlayer owner, PetState state, int size, Vec3 pos, float yaw) {
		EntityType<? extends PetCube> type = state.magma ? ModEntities.PET_MAGMA : ModEntities.PET_SLIME;
		PetCube pet = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		pet.addTag(PetCube.OWNER_PREFIX + owner.getUUID());
		pet.addTag(PetCube.GEN_PREFIX + state.generation);
		pet.addTag(PetCube.ID_PREFIX + state.id);
		pet.setSize(size, false);
		setHealth(pet, state.health(size));
		pet.setCustomName(Component.literal(state.name));
		pet.snapTo(pos.x, pos.y, pos.z, yaw, 0.0F);
		level.addFreshEntity(pet);
		return pet;
	}

	private static void setHealth(PetCube pet, float health) {
		pet.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
		pet.setHealth(health);
	}

	/** Living pieces of this owner's current pet near an entity. */
	public static List<PetCube> pieces(ServerLevel level, ServerPlayer owner, PetState state, Entity near) {
		AABB box = near.getBoundingBox().inflate(96.0);
		return level.getEntitiesOfClass(PetCube.class, box, p -> !p.isRemoved() && p.isOwnedBy(owner)
				&& state.id.equals(p.petId()) && p.generation() == state.generation);
	}

	public static void tickPiece(PetCube pet, ServerLevel level) {
		ServerPlayer owner = pet.owner();
		if (owner == null) {
			pet.setTarget(null);
			return;
		}
		PetState state = stateOf(owner, pet);
		if (state == null) {
			pet.discard();
			return;
		}
		// The whole pet carries the name (a name tag renames it); split pieces show it.
		if (state.status == PetState.WHOLE && pet.hasCustomName() && !pet.getCustomName().getString().equals(state.name)) {
			state.name = pet.getCustomName().getString();
		} else if (!pet.hasCustomName() || !pet.getCustomName().getString().equals(state.name)) {
			pet.setCustomName(Component.literal(state.name));
		}
		LivingEntity target = pet.getTarget();
		if (!PetCube.validTarget(target, pet) || state.sitting) {
			target = null;
		}
		if (pet.tickCount % 10 == 0 && !state.sitting) {
			LivingEntity hurtBy = owner.getLastHurtByMob();
			LivingEntity hurt = owner.getLastHurtMob();
			if (hurtBy != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100 && PetCube.validTarget(hurtBy, pet)) {
				target = hurtBy;
			} else if (hurt != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && PetCube.validTarget(hurt, pet)) {
				target = hurt;
			}
		}
		pet.setTarget(target);
		pet.setChest(state.chest && state.status == PetState.WHOLE && pet.getSize() >= 4);
		if (!pet.isVehicle()) {
			pet.serverMove(level, owner, state, target);
			// Following pets (and their pieces) come to the owner when far away, or when stuck for 5 seconds.
			if (!state.sitting && owner.level() == level) {
				double distance = pet.distanceTo(owner);
				int stuck = pet.stuckTicks(distance);
				if ((distance > 24.0 || stuck >= STUCK_TICKS) && (owner.onGround() || owner.isInWater())) {
					pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
					pet.resetStuck();
				}
			}
		}
		if (state.status == PetState.SPLIT && pet.tickCount % 20 == 0) {
			regrow(pet, level, owner, state);
		}
	}

	/** Split pieces grow back into the whole pet after enough quiet (a minute for a medium pet, 45 seconds for a big one). */
	static void regrow(PetCube pet, ServerLevel level, ServerPlayer owner, PetState state) {
		List<PetCube> pieces = pieces(level, owner, state, pet).stream().filter(LivingEntity::isAlive).toList();
		PetCube leader = pieces.stream().min(Comparator.comparing(Entity::getUUID)).orElse(null);
		if (leader != pet) {
			return;
		}
		long now = level.getGameTime();
		int wait = state.fullSize() >= 4 ? REGROW_BIG : REGROW_MEDIUM;
		if (now - state.lastDamage < wait) {
			return;
		}
		Vec3 pos = pieces.stream().filter(p -> p.getSize() >= 2).findFirst().orElse(pet).position();
		for (PetCube piece : pieces) {
			particles(level, piece, pieceParticle(state), 10);
			piece.discard();
		}
		spawnPiece(level, owner, state, state.fullSize(), pos, pet.getYRot());
		state.status = PetState.WHOLE;
		level.playSound(null, pet.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.2F, 0.7F);
	}

	private static ParticleOptions pieceParticle(PetState state) {
		return state.magma ? ParticleTypes.FLAME : ParticleTypes.ITEM_SLIME;
	}

	// ---------------------------------------------------------------- damage, splitting, the core

	/** The owner (and the owner's other pets) never hurt the pet; it can't be hurt while its owner is offline. */
	public static boolean allowDamage(PetCube pet, DamageSource source) {
		ServerPlayer owner = pet.owner();
		if (owner == null) {
			return false;
		}
		Entity attacker = source.getEntity();
		return !pet.isOwnedBy(attacker) && !(attacker instanceof PetCube other && other.isOwnedBy(owner));
	}

	public static void onPieceDamaged(PetCube pet, ServerLevel level) {
		ServerPlayer owner = pet.owner();
		PetState state = owner == null ? null : stateOf(owner, pet);
		if (state != null) {
			state.lastDamage = level.getGameTime();
		}
	}

	/** Killed: a medium piece splits into two smalls, a big one into two mediums; the last small drops the Slimy Core. */
	public static void onPieceKilled(PetCube pet, ServerLevel level) {
		ServerPlayer owner = pet.owner();
		if (owner == null) {
			return;
		}
		PetState state = stateOf(owner, pet);
		if (state == null) {
			return;
		}
		if (owner.containerMenu instanceof PetMenu) {
			owner.closeContainer();
		}
		int size = pet.getSize();
		if (size >= 2) {
			int child = size / 2;
			Vec3 pos = pet.position();
			spawnPiece(level, owner, state, child, pos.add(0.3, 0.2, 0.0), pet.getYRot());
			spawnPiece(level, owner, state, child, pos.add(-0.3, 0.2, 0.0), pet.getYRot());
			state.status = PetState.SPLIT;
			state.lastDamage = level.getGameTime();
			return;
		}
		boolean others = pieces(level, owner, state, pet).stream().anyMatch(p -> p != pet);
		if (others) {
			return;
		}
		state.status = PetState.CORE;
		state.sitting = false;
		ItemStack core = new ItemStack(ModItems.SLIME_PET_CORE);
		core.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
				Component.literal(owner.getName().getString() + "'s " + state.name + " Slimy Core")
						.withStyle(style -> style.withItalic(false)));
		writeCore(core, owner, state);
		ItemEntity item = new ItemEntity(level, pet.getX(), pet.getY() + 0.3, pet.getZ(), core);
		item.setUnlimitedLifetime();
		level.addFreshEntity(item);
		owner.sendSystemMessage(Messages.c(state.name + " fell apart! Its Slimy Core dropped where it died.", GRAY));
	}

	// ---------------------------------------------------------------- feeding, sitting, riding

	public static InteractionResult interact(PetCube pet, ServerPlayer player, InteractionHand hand) {
		if (!pet.isOwnedBy(player) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		PetState state = stateOf(player, pet);
		if (state == null) {
			return InteractionResult.PASS;
		}
		ServerLevel level = (ServerLevel) pet.level();
		ItemStack stack = player.getItemInHand(hand);
		boolean whole = state.status == PetState.WHOLE;
		if (stack.is(Items.SLIME_BALL)) {
			if (pet.getHealth() >= pet.getMaxHealth()) {
				return InteractionResult.PASS;
			}
			stack.consume(1, player);
			pet.heal(SLIME_BALL_HEAL);
			particles(level, pet, ParticleTypes.HEART, 3);
			pet.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 1.0F);
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.THERMAL_SLIMEBALL)) {
			if (!whole || !state.ionic || state.big || state.magma) {
				return InteractionResult.PASS;
			}
			stack.consume(1, player);
			state.growFeeds++;
			if (state.growFeeds >= GROW_GUARANTEED || player.getRandom().nextDouble() < GROW_CHANCE) {
				state.big = true;
				pet.setSize(4, false);
				setHealth(pet, state.fullHealth());
				particles(level, pet, ParticleTypes.HEART, 10);
				level.playSound(null, pet.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.5F, 0.6F);
			} else {
				particles(level, pet, ParticleTypes.ITEM_SLIME, 6);
				pet.playSound(SoundEvents.SLIME_SQUISH_SMALL, 1.0F, 1.2F);
			}
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.IONIC_LIQUID)) {
			if (!whole || state.ionic || state.big || state.magma) {
				return InteractionResult.PASS;
			}
			stack.consume(1, player);
			state.ionic = true;
			setHealth(pet, state.fullHealth());
			player.sendSystemMessage(Messages.c("He seems to be able to grow a bit more now!", GRAY));
			particles(level, pet, ParticleTypes.HAPPY_VILLAGER, 10);
			return InteractionResult.SUCCESS;
		}
		if (stack.is(ModItems.CORRODED_PALLADIUM)) {
			if (!whole || !state.big || state.magma || state.palladium) {
				return InteractionResult.PASS;
			}
			stack.consume(1, player);
			state.palladium = true;
			player.sendSystemMessage(Messages.c("This slime now craves for HEAT!", 0xFFAA00));
			particles(level, pet, ParticleTypes.SMOKE, 12);
			return InteractionResult.SUCCESS;
		}
		int heat = stack.is(Items.MAGMA_CREAM) ? 5 : stack.is(Items.MAGMA_BLOCK) ? 10 : stack.is(Items.LAVA_BUCKET) ? 20 : 0;
		if (heat > 0) {
			if (!whole || !state.palladium || state.magma) {
				return InteractionResult.PASS;
			}
			if (stack.is(Items.LAVA_BUCKET)) {
				player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
			} else {
				stack.consume(1, player);
			}
			state.heat = Math.min(HEAT_GOAL, state.heat + heat);
			particles(level, pet, ParticleTypes.FLAME, 8);
			if (state.heat >= HEAT_GOAL) {
				becomeMagma(pet, level, player, state);
			} else {
				String name = state.name;
				player.sendSystemMessage(Messages.c(heat == 5 ? name + " is now a bit warmer. Keep going..."
						: heat == 10 ? name + " is now boiling a bit more! Keep going..."
								: name + "'s insides are scorching more! Keep going...", HEAT));
			}
			return InteractionResult.SUCCESS;
		}
		if (!stack.isEmpty()) {
			return InteractionResult.PASS;
		}
		if (player.isShiftKeyDown()) {
			if (state.sitting) {
				follow(player, state);
			} else {
				state.sitting = true;
			}
			player.sendSystemMessage(Component.literal(state.name + (state.sitting ? " is sitting." : " is following you.")), true);
			return InteractionResult.SUCCESS;
		}
		if (whole && pet.getSize() >= 4 && !pet.isVehicle()) {
			player.startRiding(pet, false, true);
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** Heat 100: the big slime becomes a big Magma Cube (80 health, fire immune). */
	static void becomeMagma(PetCube pet, ServerLevel level, ServerPlayer owner, PetState state) {
		state.magma = true;
		Vec3 pos = pet.position();
		float yaw = pet.getYRot();
		pet.ejectPassengers();
		pet.discard();
		PetCube cube = spawnPiece(level, owner, state, 4, pos, yaw);
		particles(level, cube, ParticleTypes.LAVA, 20);
		particles(level, cube, ParticleTypes.FLAME, 30);
		level.playSound(null, cube.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 1.0F, 0.7F);
		QuestManager.award(owner, "cluster/pet_magma");
	}

	public static void openMenu(PetCube pet, ServerPlayer player) {
		PetState state = pet.isOwnedBy(player) ? stateOf(player, pet) : null;
		if (state == null || state.status != PetState.WHOLE || pet.getSize() < 4) {
			return;
		}
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PetMenu(id, inventory, state, pet),
				Component.literal(state.name)));
	}

	// ---------------------------------------------------------------- revive

	/** A revived core used on the ground: the pet comes back as it was (medium, big or a big Magma Cube). */
	public static boolean revive(ServerPlayer player, Vec3 pos, java.util.UUID petId) {
		PetState state = pet(player, petId);
		if (state == null) {
			player.sendSystemMessage(Messages.c("This core belongs to someone else's slime.", GRAY));
			return false;
		}
		if (state.status != PetState.CORE) {
			player.sendSystemMessage(Messages.c(state.name + " is already alive.", GRAY));
			return false;
		}
		state.generation++;
		state.status = PetState.WHOLE;
		follow(player, state);
		ServerLevel level = player.level();
		PetCube pet = spawnPiece(level, player, state, state.fullSize(), pos, player.getYRot() + 180.0F);
		particles(level, pet, ParticleTypes.HEART, 10);
		particles(level, pet, pieceParticle(state), 20);
		level.playSound(null, pet.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.5F, 1.0F);
		return true;
	}

	// ---------------------------------------------------------------- the core's data

	private static final String CORE_PET = "huntlike_pet";
	private static final String CORE_OWNER = "huntlike_owner";

	/** The core (and the revived core) remember which pet they are. */
	public static void writeCore(ItemStack core, ServerPlayer owner, PetState state) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putString(CORE_PET, state.id.toString());
		tag.putString(CORE_OWNER, owner.getUUID().toString());
		core.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
	}

	public static java.util.UUID corePet(ItemStack core) {
		var data = core.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		try {
			return java.util.UUID.fromString(data.copyTag().getStringOr(CORE_PET, ""));
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static void particles(ServerLevel level, Entity entity, ParticleOptions particle, int count) {
		level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), count,
				entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.02);
	}
}
