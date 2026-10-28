package com.huntlike.gear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.Messages;
import com.huntlike.hunt.QuestManager;
import com.huntlike.registry.Lore;
import com.huntlike.registry.ModItems;

/** Towernaar's shields, the Magnetic Wand and its PRD. */
public final class ModShields {
	private ModShields() {
	}

	/** Rogue Hunter kills with an Unbreakable Shield in the offhand that add Towernaar's sigil (after Chapter III). */
	public static final int SIGIL_KILLS = 10;
	private static final String SIGIL_KEY = "huntlike_sigil";

	// Enchantability 60: a level-30 table always rolls a cost between 26 and 70, the range where Unbreaking III is
	// the only Unbreaking level (any higher and Unbreaking would drop out completely).
	public static final Item OBSIDIAN_SHIELD = shield("obsidian_shield", new Item.Properties().durability(2500).enchantable(60)
			.repairable(Items.OBSIDIAN).rarity(Rarity.RARE), "An exaggeratedly resistant shield.");
	public static final Item UNBREAKABLE_SHIELD = shield("unbreakable_shield", new Item.Properties().stacksTo(1)
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.EPIC),
			"A shield that carries the honor of Towernaar's family.");
	public static final Item UNBREAKABLE_SHIELD_STAR = shield("unbreakable_shield_star", new Item.Properties().stacksTo(1)
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).component(DataComponents.USE_EFFECTS, new UseEffects(true, true, 1.0F))
			.rarity(Rarity.EPIC), "A small upgrade to the unbreakable shield. It adds the signature mark of Towernaar's family, "
					+ "making the shield lighter.", "Blocking doesn't slow you down.");

	public static final Item MAGNETIC_WAND = ModItems.register("magnetic_wand", MagneticWandItem::new, new Item.Properties().stacksTo(1)
			.component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.EPIC)
			.component(DataComponents.LORE, new ItemLore(lore("A magical wand that uses the magnetic moment of holmium to pull things "
					+ "together.", "Right-click to pull creatures to where you look. 10 s cooldown."))));
	public static final Item MAGNETIC_WAND_PRD = ModItems.register("magnetic_wand_prd", PrdItem::new, new Item.Properties().stacksTo(1)
			.rarity(Rarity.EPIC).component(DataComponents.LORE, new ItemLore(lore("Displays the product requirements for a magnetic "
					+ "wand as well as the template to create it. In small print a text says \"Thank you adventurer.\"...",
					"Use it to learn the Magnetic Wand trade [Gear]."))));

	public static final List<Item> SHIELDS = List.of(OBSIDIAN_SHIELD, UNBREAKABLE_SHIELD, UNBREAKABLE_SHIELD_STAR);

	private static Item shield(String name, Item.Properties properties, String description, String... extra) {
		// Blocking like the vanilla shield (its values: 90° arc, 0.25 s delay, 3+ durability per block, its sounds).
		properties.delayedComponent(DataComponents.BLOCKS_ATTACKS, registries -> new net.minecraft.world.item.component.BlocksAttacks(0.25F,
				1.0F, List.of(new net.minecraft.world.item.component.BlocksAttacks.DamageReduction(90.0F, java.util.Optional.empty(), 0.0F, 1.0F)),
				new net.minecraft.world.item.component.BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
				java.util.Optional.of(registries.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
						.getOrThrow(net.minecraft.tags.DamageTypeTags.BYPASSES_SHIELD)),
				java.util.Optional.of(SoundEvents.SHIELD_BLOCK), java.util.Optional.of(SoundEvents.SHIELD_BREAK)));
		properties.component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
		properties.equippableUnswappable(EquipmentSlot.OFFHAND);
		properties.component(DataComponents.LORE, new ItemLore(lore(description, extra)));
		return ModItems.register(name, Item::new, properties);
	}

	private static List<Component> lore(String description, String... extra) {
		List<Component> lore = new ArrayList<>();
		Lore.gray(lore, description);
		for (String line : extra) {
			lore.add(Component.empty());
			Lore.gray(lore, line);
		}
		return lore;
	}

	// ---------------------------------------------------------------- Towernaar's sigil (after Chapter III)

	public static int sigilKills(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? -1 : data.copyTag().getIntOr(SIGIL_KEY, -1);
	}

	/** Shows the sigil counter on an Unbreakable Shield. */
	public static void setSigilKills(ItemStack stack, int kills) {
		CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putInt(SIGIL_KEY, kills));
		List<Component> lore = new ArrayList<>(lore("A shield that carries the honor of Towernaar's family."));
		lore.add(Component.empty());
		lore.add(Component.literal("Towernaar's sigil: " + kills + "/" + SIGIL_KILLS + " Rogue Hunters")
				.withStyle(style -> style.withColor(ChatFormatting.DARK_PURPLE).withItalic(false)));
		stack.set(DataComponents.LORE, new ItemLore(lore));
	}

	/** After Chapter III, every Unbreakable Shield the player carries shows its sigil counter. */
	public static void stampSigil(ServerPlayer player, HuntData data) {
		if (data.towernaarDone < 3) {
			return;
		}
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(UNBREAKABLE_SHIELD) && sigilKills(stack) < 0) {
				setSigilKills(stack, 0);
			}
		}
		ItemStack offhand = player.getOffhandItem();
		if (offhand.is(UNBREAKABLE_SHIELD) && sigilKills(offhand) < 0) {
			setSigilKills(offhand, 0);
		}
	}

	/** A Rogue Hunter killed by this player: an Unbreakable Shield in the offhand earns a sigil kill (after Chapter III). */
	public static void onRogueKill(ServerPlayer player, HuntData data, DamageSource source) {
		ItemStack offhand = player.getOffhandItem();
		if (data.towernaarDone < 3 || source.getEntity() != player || !offhand.is(UNBREAKABLE_SHIELD)) {
			return;
		}
		int kills = Math.max(0, sigilKills(offhand)) + 1;
		if (kills < SIGIL_KILLS) {
			setSigilKills(offhand, kills);
			return;
		}
		ItemStack star = new ItemStack(UNBREAKABLE_SHIELD_STAR);
		var enchantments = offhand.get(DataComponents.ENCHANTMENTS);
		if (enchantments != null) {
			star.set(DataComponents.ENCHANTMENTS, enchantments);
		}
		var name = offhand.get(DataComponents.CUSTOM_NAME);
		if (name != null) {
			star.set(DataComponents.CUSTOM_NAME, name);
		}
		player.setItemSlot(EquipmentSlot.OFFHAND, star);
		player.sendSystemMessage(Messages.c("Your shield now carries Towernaar's family sigil!", 0xAA00AA));
		player.level().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
	}

	// ---------------------------------------------------------------- Magnetic Wand

	public static final int WAND_COOLDOWN = 200;
	public static final double WAND_REACH = 20.0;
	public static final double WAND_RADIUS = 8.0;

	public static class MagneticWandItem extends Item {
		public MagneticWandItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (player instanceof ServerPlayer serverPlayer) {
				pull(serverPlayer);
				serverPlayer.getCooldowns().addCooldown(player.getItemInHand(hand), WAND_COOLDOWN);
			}
			return InteractionResult.SUCCESS;
		}
	}

	/** How long a cast holds its creatures at the spot: 1.5 seconds. */
	public static final int WAND_HOLD_TICKS = 30;

	/** A cast's magnetic field: the spot, the creatures it caught, and the ticks it has left. */
	private static final class Field {
		final ServerLevel level;
		final Vec3 point;
		final List<java.util.UUID> caught;
		int ticksLeft = WAND_HOLD_TICKS;

		Field(ServerLevel level, Vec3 point, List<java.util.UUID> caught) {
			this.level = level;
			this.point = point;
			this.caught = caught;
		}
	}

	private static final List<Field> FIELDS = new ArrayList<>();

	/**
	 * Catches every creature within 8 blocks of the spot you look at (up to 20 blocks away), then for 1.5 seconds draws them
	 * to that spot and holds them there. Returns how many it caught.
	 */
	public static int pull(ServerPlayer player) {
		ServerLevel level = player.level();
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(WAND_REACH));
		HitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
		Vec3 point = hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
		List<java.util.UUID> caught = new ArrayList<>();
		for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(WAND_RADIUS),
				e -> e.isAlive() && !(e instanceof Player) && !(e instanceof ArmorStand)
						&& !(e instanceof com.huntlike.pet.PetCube pet && pet.isOwnedBy(player))
						&& !(e instanceof TamableAnimal pet && pet.isOwnedBy(player)) && e.position().distanceTo(point) <= WAND_RADIUS)) {
			caught.add(entity.getUUID());
			attract(entity, point);
		}
		if (!caught.isEmpty()) {
			FIELDS.add(new Field(level, point, caught));
		}
		Vec3 step = point.subtract(eye).normalize();
		for (double d = 1.0; d < point.distanceTo(eye); d += 0.6) {
			Vec3 at = eye.add(step.scale(d));
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
		level.sendParticles(ParticleTypes.REVERSE_PORTAL, point.x, point.y + 0.5, point.z, 40, WAND_RADIUS / 3, 0.6, WAND_RADIUS / 3, 0.05);
		level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.6F);
		return caught.size();
	}

	/**
	 * One tick of the field on a creature: a firm pull toward the spot that eases off as it gets close, and once there it
	 * stays put (no launching, no overshooting). Its pathfinding is stopped so it can't walk away.
	 */
	private static void attract(LivingEntity entity, Vec3 point) {
		Vec3 to = point.subtract(entity.position());
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3 motion = entity.getDeltaMovement();
		double speed = Math.min(0.9, 0.08 + flat * 0.3);
		double vx = flat > 0.3 ? to.x / flat * speed : 0.0;
		double vz = flat > 0.3 ? to.z / flat * speed : 0.0;
		// Climb toward a spot above (a little more than gravity), otherwise let it fall onto the spot.
		double vy = to.y > 0.5 ? Math.min(0.5, 0.1 + to.y * 0.25) : Math.min(motion.y, 0.0);
		entity.setDeltaMovement(vx, vy, vz);
		entity.resetFallDistance();
		if (entity instanceof net.minecraft.world.entity.Mob mob) {
			mob.getNavigation().stop();
		}
	}

	private static void tickFields() {
		for (java.util.Iterator<Field> it = FIELDS.iterator(); it.hasNext();) {
			Field field = it.next();
			if (--field.ticksLeft < 0) {
				it.remove();
				continue;
			}
			for (java.util.UUID id : field.caught) {
				if (field.level.getEntity(id) instanceof LivingEntity entity && entity.isAlive() && entity.level() == field.level) {
					attract(entity, field.point);
				}
			}
			if (field.ticksLeft % 4 == 0) {
				field.level.sendParticles(ParticleTypes.REVERSE_PORTAL, field.point.x, field.point.y + 0.4, field.point.z, 8, 0.4, 0.3,
						0.4, 0.02);
			}
		}
	}

	// ---------------------------------------------------------------- Magnetic Wand PRD

	public static class PrdItem extends Item {
		public PrdItem(Properties properties) {
			super(properties);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			if (player instanceof ServerPlayer serverPlayer) {
				HuntData data = QuestManager.data(serverPlayer);
				if (data.wandUnlocked) {
					serverPlayer.sendSystemMessage(Messages.c("You already know how to make the Magnetic Wand.", 0xAAAAAA));
					return InteractionResult.FAIL;
				}
				data.wandUnlocked = true;
				serverPlayer.setAttached(QuestManager.DATA, data);
				player.getItemInHand(hand).consume(1, player);
				serverPlayer.sendSystemMessage(Messages.c("You can now make the Magnetic Wand! [Gear]", 0xFFAA00));
				level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
				QuestManager.sync(serverPlayer);
			}
			return InteractionResult.SUCCESS;
		}
	}

	public static void initialize() {
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> tickFields());
		// Shields picked up or traded after Chapter III get their sigil counter.
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 40 != 0) {
				return;
			}
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				HuntData data = QuestManager.data(player);
				if (data.towernaarDone >= 3) {
					stampSigil(player, data);
				}
			}
		});
	}
}
