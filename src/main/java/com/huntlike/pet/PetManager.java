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

	public static final AttachmentType<PetState> STATE = AttachmentRegistry.create(HuntLike.id("slime_pet"),
			builder -> builder.initializer(PetState::new).persistent(PetState.CODEC).copyOnDeath());

	public static final double TAME_CHANCE = 0.05;
	public static final int TAME_GUARANTEED = 20;
	public static final double GROW_CHANCE = 0.04;
	public static final int GROW_GUARANTEED = 25;
	public static final int HEAT_GOAL = 100;
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

	public static PetState state(ServerPlayer player) {
		return player.getAttachedOrCreate(STATE);
	}

	public static void save(ServerPlayer player, PetState state) {
		player.setAttached(STATE, state);
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
		PetState state = state(player);
		if (state.alive()) {
			player.sendSystemMessage(Messages.c("You already have a slime companion!", GRAY));
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
		pet.generation = state.generation + 1;
		pet.name = randomName(player.getRandom());
		pet.status = PetState.WHOLE;
		save(player, pet);
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
				&& p.generation() == state.generation);
	}

	public static void tickPiece(PetCube pet, ServerLevel level) {
		ServerPlayer owner = pet.owner();
		if (owner == null) {
			pet.setTarget(null);
			return;
		}
		PetState state = state(owner);
		if (!state.alive() || pet.generation() != state.generation) {
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
		if (!PetCube.validTarget(target, pet)) {
			target = null;
		}
		if (pet.tickCount % 10 == 0) {
			LivingEntity hurtBy = owner.getLastHurtByMob();
			LivingEntity hurt = owner.getLastHurtMob();
			if (hurtBy != null && owner.tickCount - owner.getLastHurtByMobTimestamp() < 100 && PetCube.validTarget(hurtBy, pet)) {
				target = hurtBy;
			} else if (hurt != null && owner.tickCount - owner.getLastHurtMobTimestamp() < 100 && PetCube.validTarget(hurt, pet)) {
				target = hurt;
			}
		}
		pet.setTarget(target);
		if (!pet.isVehicle()) {
			pet.serverMove(level, owner, state, target);
			if (state.status == PetState.WHOLE && !state.sitting && owner.level() == level && pet.distanceToSqr(owner) > 24 * 24
					&& owner.onGround()) {
				pet.teleportTo(owner.getX(), owner.getY(), owner.getZ());
			}
		}
		if (state.status == PetState.SPLIT && pet.tickCount % 20 == 0) {
			regrow(pet, level, owner, state);
		}
	}

	/** Split pieces grow back after enough quiet: any medium piece -> the big pet; only small pieces -> a medium (or two). */
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
		boolean anyMedium = pieces.stream().anyMatch(p -> p.getSize() >= 2);
		Vec3 pos = pieces.stream().filter(p -> p.getSize() >= 2).findFirst().orElse(pet).position();
		for (PetCube piece : pieces) {
			particles(level, piece, pieceParticle(state), 10);
			piece.discard();
		}
		if (state.fullSize() < 4 || anyMedium) {
			spawnPiece(level, owner, state, state.fullSize(), pos, pet.getYRot());
			state.status = PetState.WHOLE;
		} else {
			spawnPiece(level, owner, state, 2, pos.add(0.4, 0.0, 0.0), pet.getYRot());
			spawnPiece(level, owner, state, 2, pos.add(-0.4, 0.0, 0.0), pet.getYRot());
			state.lastDamage = now;
		}
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
		if (owner != null) {
			state(owner).lastDamage = level.getGameTime();
		}
	}

	/** Killed: a medium piece splits into two smalls, a big one into two mediums; the last small drops the Slimy Core. */
	public static void onPieceKilled(PetCube pet, ServerLevel level) {
		ServerPlayer owner = pet.owner();
		if (owner == null) {
			return;
		}
		PetState state = state(owner);
		if (pet.generation() != state.generation || !state.alive()) {
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
		PetState state = state(player);
		if (pet.generation() != state.generation || !state.alive()) {
			return InteractionResult.PASS;
		}
		ServerLevel level = (ServerLevel) pet.level();
		ItemStack stack = player.getItemInHand(hand);
		boolean whole = state.status == PetState.WHOLE;
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
			if (!whole) {
				return InteractionResult.PASS;
			}
			state.sitting = !state.sitting;
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
		PetState state = state(player);
		if (!pet.isOwnedBy(player) || state.status != PetState.WHOLE || pet.getSize() < 4) {
			return;
		}
		player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PetMenu(id, inventory, state, pet),
				Component.literal(state.name)));
	}

	// ---------------------------------------------------------------- revive

	/** A revived core used on the ground: the pet comes back as it was (medium, big or a big Magma Cube). */
	public static boolean revive(ServerPlayer player, Vec3 pos) {
		PetState state = state(player);
		if (state.status != PetState.CORE) {
			player.sendSystemMessage(Messages.c("You have no slime waiting to come back.", GRAY));
			return false;
		}
		state.generation++;
		state.status = PetState.WHOLE;
		state.sitting = false;
		ServerLevel level = player.level();
		PetCube pet = spawnPiece(level, player, state, state.fullSize(), pos, player.getYRot() + 180.0F);
		particles(level, pet, ParticleTypes.HEART, 10);
		particles(level, pet, pieceParticle(state), 20);
		level.playSound(null, pet.blockPosition(), SoundEvents.SLIME_SQUISH, SoundSource.NEUTRAL, 1.5F, 1.0F);
		return true;
	}

	private static void particles(ServerLevel level, Entity entity, ParticleOptions particle, int count) {
		level.sendParticles(particle, entity.getX(), entity.getY() + entity.getBbHeight() * 0.6, entity.getZ(), count,
				entity.getBbWidth() * 0.4, entity.getBbHeight() * 0.3, entity.getBbWidth() * 0.4, 0.02);
	}
}
