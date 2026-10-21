package com.huntlike.hunt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import com.huntlike.HuntLike;
import com.huntlike.hunt.HuntData.Progress;
import com.huntlike.hunt.HuntData.Quest;
import com.huntlike.hunt.HuntDefs.Drop;
import com.huntlike.hunt.HuntDefs.Hunt;
import com.huntlike.hunt.HuntDefs.Tier;
import com.huntlike.network.HuntPayloads;
import com.huntlike.registry.ModItems;

/** Server side of Hunting Quests: starting, counting kills, completing and failing. */
public final class QuestManager {
	private QuestManager() {
	}

	public static final AttachmentType<HuntData> DATA = AttachmentRegistry.create(HuntLike.id("hunt_data"),
			builder -> builder.initializer(HuntData::new).persistent(HuntData.CODEC).copyOnDeath());

	public static final TagKey<EntityType<?>> QUOTA_ANIMALS = TagKey.create(Registries.ENTITY_TYPE, HuntLike.id("animal_quota"));

	public static final int BOSS_TIME_LIMIT = 20 * 60 * 5;
	public static final int SUMMON_TICKS = 40;
	public static final double FLEE_DISTANCE = 64.0;

	public static void initialize() {
		ServerPlayNetworking.registerGlobalReceiver(HuntPayloads.Action.TYPE,
				(payload, context) -> handleAction(context.player(), payload));
		ServerTickEvents.END_SERVER_TICK.register(QuestManager::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(QuestManager::onDeath);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register(BossController::allowDamage);
		ServerLivingEntityEvents.AFTER_DAMAGE.register(BossController::afterDamage);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> onLogout(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> onChangeLevel(player));
		ServerEntityEvents.ENTITY_LOAD.register(QuestManager::onEntityLoad);
	}

	public static HuntData data(ServerPlayer player) {
		return player.getAttachedOrCreate(DATA);
	}

	private static void save(ServerPlayer player, HuntData data) {
		player.setAttached(DATA, data);
	}

	public static boolean isPeaceful(ServerPlayer player) {
		return player.level().getDifficulty() == Difficulty.PEACEFUL;
	}

	// ---------------------------------------------------------------- menu

	public static void openMenu(ServerPlayer player) {
		sync(player);
		ServerPlayNetworking.send(player, new HuntPayloads.OpenMenu(0));
	}

	private static void handleAction(ServerPlayer player, HuntPayloads.Action action) {
		HuntData data = data(player);
		Hunt hunt = HuntDefs.hunt(action.hunt());
		switch (action.action()) {
			case "select" -> {
				data.selectedHunt = hunt.id();
				save(player, data);
			}
			case "start" -> startQuest(player, data, hunt, action.value());
			case "cancel" -> {
				if (data.quest != null) {
					fail(player, data);
				}
			}
			case "buy_tokens" -> buyTokens(player, data, hunt, action.value() == 1);
			default -> {
			}
		}
		sync(player);
	}

	private static void buyTokens(ServerPlayer player, HuntData data, Hunt hunt, boolean bulk) {
		int logs = bulk ? 100 : 10;
		int tokens = bulk ? 640 : 64;
		Inventory inventory = player.getInventory();
		int have = 0;
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.OAK_LOG)) {
				have += stack.getCount();
			}
		}
		if (have < logs) {
			player.sendSystemMessage(Messages.error("You need " + logs + " Oak Logs."));
			return;
		}
		int left = logs;
		for (int i = 0; i < inventory.getContainerSize() && left > 0; i++) {
			ItemStack stack = inventory.getItem(i);
			if (stack.is(Items.OAK_LOG)) {
				int take = Math.min(left, stack.getCount());
				stack.shrink(take);
				left -= take;
			}
		}
		inventory.setChanged();
		data.progress(hunt.id()).tokens += tokens;
		save(player, data);
		playSound(player, SoundEvents.WOOD_PLACE, 1.0F);
	}

	// ---------------------------------------------------------------- quest lifecycle

	private static void startQuest(ServerPlayer player, HuntData data, Hunt hunt, int tierNumber) {
		if (data.quest != null) {
			player.sendSystemMessage(Messages.error("You already have an active quest."));
			return;
		}
		if (isPeaceful(player)) {
			player.sendSystemMessage(Messages.peacefulStart());
			return;
		}
		if (tierNumber < 1 || tierNumber > hunt.tiers().size()) {
			return;
		}
		Tier tier = hunt.tier(tierNumber);
		Progress progress = data.progress(hunt.id());
		if (progress.rank() < tier.unlockRank()) {
			player.sendSystemMessage(Messages.error("This tier unlocks at " + hunt.rankName(tier.unlockRank()) + "."));
			return;
		}
		if (tierNumber > 1 && progress.bossKills[tierNumber - 2] == 0) {
			player.sendSystemMessage(Messages.error("Defeat " + hunt.tier(tierNumber - 1).bossName() + " first."));
			return;
		}
		if (progress.tokens < tier.tokenCost()) {
			player.sendSystemMessage(Messages.error("Not enough " + hunt.name() + " Ritual Tokens."));
			return;
		}
		progress.tokens -= tier.tokenCost();
		Quest quest = new Quest();
		quest.hunt = hunt.id();
		quest.tier = tierNumber;
		quest.quota = tier.quota();
		data.quest = quest;
		save(player, data);
		SpiritLure.reset(player);
		player.sendSystemMessage(Messages.started(hunt, tierNumber));
		playSound(player, SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F);
		playSound(player, SoundEvents.BEACON_ACTIVATE, 1.2F);
		award(player, hunt.id() + "/conquest");
	}

	/** Fails or cancels the quest: no refund. */
	public static void fail(ServerPlayer player, HuntData data) {
		Quest quest = data.quest;
		if (quest == null) {
			return;
		}
		BossController.discardBoss(player, quest);
		SpiritLure.cleanup(player);
		data.quest = null;
		save(player, data);
		player.sendSystemMessage(Messages.failed(HuntDefs.hunt(quest.hunt), quest.tier));
		playSound(player, SoundEvents.BEACON_DEACTIVATE, 0.8F);
		sync(player);
	}

	/** One kill short of the boss: used when the boss can't spawn or disappears without being killed. */
	public static void resetToQuotaMinusOne(ServerPlayer player, HuntData data) {
		Quest quest = data.quest;
		if (quest == null) {
			return;
		}
		BossController.discardBoss(player, quest);
		quest.phase = HuntData.PHASE_KILLING;
		quest.kills = Math.max(0, quest.quota - 1);
		quest.bossUuid = "";
		save(player, data);
		sync(player);
	}

	public static void complete(ServerPlayer owner, HuntData data, Collection<UUID> helpers) {
		Quest quest = data.quest;
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		Tier tier = hunt.tier(quest.tier);
		Progress progress = data.progress(hunt.id());
		RandomSource random = owner.getRandom();

		for (Drop drop : HuntDefs.drops(tier)) {
			if (drop.guaranteed()) {
				int count = drop.minCount() + random.nextInt(drop.maxCount() - drop.minCount() + 1);
				give(owner, data, hunt, drop, count);
			} else if (random.nextInt(drop.chanceDen()) < drop.chanceNum()) {
				give(owner, data, hunt, drop, drop.minCount());
				owner.sendSystemMessage(Messages.rareDrop(drop.dropClass(), new ItemStack(drop.item()).getStyledHoverName()));
				playSound(owner, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F);
			}
		}
		for (UUID helperId : helpers) {
			if (helperId.equals(owner.getUUID())) {
				continue;
			}
			ServerPlayer helper = owner.level().getServer().getPlayerList().getPlayer(helperId);
			if (helper == null) {
				continue;
			}
			HuntData helperData = data(helper);
			for (Drop drop : HuntDefs.drops(tier)) {
				if (!drop.guaranteed() && random.nextInt(drop.chanceDen() * 2) < drop.chanceNum()) {
					give(helper, helperData, hunt, drop, drop.minCount());
					helper.sendSystemMessage(Messages.rareDrop(drop.dropClass(), new ItemStack(drop.item()).getStyledHoverName()));
					playSound(helper, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5F);
				}
			}
			save(helper, helperData);
			sync(helper);
		}

		int oldRank = progress.rank();
		progress.bossKills[tier.number() - 1]++;
		progress.xp += tier.xp();
		int newRank = progress.rank();
		SpiritLure.cleanup(owner);
		data.quest = null;
		save(owner, data);

		owner.sendSystemMessage(Messages.complete(hunt, tier.number()));
		playSound(owner, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F);
		award(owner, hunt.id() + "/tier" + tier.number());
		for (int level = oldRank + 1; level <= newRank; level++) {
			owner.sendSystemMessage(Messages.rankUp(hunt, level));
			playSound(owner, SoundEvents.PLAYER_LEVELUP, 1.0F);
			playSound(owner, SoundEvents.BELL_BLOCK, 1.0F);
			if (level == HuntDefs.MAX_RANK) {
				award(owner, hunt.id() + "/slayer");
			}
		}
		sync(owner);
	}

	/** Delivers a drop like /give: into the inventory, anything left over drops for that player only. */
	private static void give(ServerPlayer player, HuntData data, Hunt hunt, Drop drop, int count) {
		Progress progress = data.progress(hunt.id());
		progress.dropsSeen.add(drop.id());
		progress.dropCounts.merge(drop.id(), count, Integer::sum);
		giveStack(player, new ItemStack(drop.item(), count));
	}

	public static void giveStack(ServerPlayer player, ItemStack stack) {
		player.getInventory().add(stack);
		if (!stack.isEmpty()) {
			ItemEntity item = new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack);
			item.setNoPickUpDelay();
			item.setTarget(player.getUUID());
			player.level().addFreshEntity(item);
		}
		player.inventoryMenu.broadcastChanges();
	}

	// ---------------------------------------------------------------- events

	private static void tick(MinecraftServer server) {
		boolean syncNow = server.getTickCount() % 20 == 0;
		for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
			HuntData data = data(player);
			Quest quest = data.quest;
			if (quest == null) {
				continue;
			}
			switch (quest.phase) {
				case HuntData.PHASE_KILLING -> {
					if (!isPeaceful(player)) {
						SpiritLure.tick(player, data, quest);
					}
				}
				case HuntData.PHASE_SUMMONING -> BossController.tickSummon(player, data, quest);
				case HuntData.PHASE_BOSS -> BossController.tickBoss(player, data, quest);
				default -> {
				}
			}
			if (syncNow && data.quest != null) {
				sync(player);
			}
		}
	}

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (entity instanceof ServerPlayer player) {
			HuntData data = data(player);
			if (data.quest != null && data.quest.phase != HuntData.PHASE_KILLING) {
				fail(player, data);
			}
			return;
		}
		if (BossController.isBoss(entity)) {
			BossController.onBossDeath(entity);
			return;
		}
		if (!entity.getType().builtInRegistryHolder().is(QUOTA_ANIMALS)) {
			return;
		}
		if (entity instanceof Mob mob && mob.isBaby()) {
			return;
		}
		if (!(source.getEntity() instanceof ServerPlayer killer)) {
			return;
		}
		HuntData data = data(killer);
		if (SpiritLure.isLuredBy(entity, killer) && killer.getRandom().nextInt(100) == 0) {
			HuntData.Progress progress = data.progress("animal");
			progress.dropsSeen.add("soul_stone");
			progress.dropCounts.merge("soul_stone", 1, Integer::sum);
			save(killer, data);
			giveStack(killer, new ItemStack(ModItems.SOUL_STONE));
		}
		Quest quest = data.quest;
		if (quest == null || quest.phase != HuntData.PHASE_KILLING || !quest.hunt.equals("animal") || isPeaceful(killer)) {
			return;
		}
		quest.kills = Math.min(quest.quota, quest.kills + 1);
		SpiritLure.onQuotaKill(killer);
		Hunt hunt = HuntDefs.hunt(quest.hunt);
		killer.sendSystemMessage(Messages.quota(hunt, quest.kills, quest.quota), true);
		if (quest.kills >= quest.quota) {
			beginSummon(killer, data, quest, entity.blockPosition());
		}
		save(killer, data);
		sync(killer);
	}

	private static void beginSummon(ServerPlayer player, HuntData data, Quest quest, BlockPos killPos) {
		BlockPos spot = findGround(player.level(), killPos);
		if (spot == null) {
			player.sendSystemMessage(Messages.badTerrain());
			quest.kills = quest.quota - 1;
			return;
		}
		quest.phase = HuntData.PHASE_SUMMONING;
		quest.summonX = spot.getX();
		quest.summonY = spot.getY();
		quest.summonZ = spot.getZ();
		quest.summonTicks = SUMMON_TICKS;
		playSound(player, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.0F);
	}

	/** A standing spot with solid ground within 5 blocks of the last quota kill, or null. */
	private static BlockPos findGround(ServerLevel level, BlockPos origin) {
		for (int radius = 0; radius <= 5; radius++) {
			for (int dy = 0; dy <= 5; dy++) {
				for (int sign : new int[] {1, -1}) {
					for (int dx = -radius; dx <= radius; dx++) {
						for (int dz = -radius; dz <= radius; dz++) {
							if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
								continue;
							}
							BlockPos pos = origin.offset(dx, dy * sign, dz);
							BlockState below = level.getBlockState(pos.below());
							if (below.isFaceSturdy(level, pos.below(), Direction.UP)
									&& level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
									&& level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
									&& level.getFluidState(pos).isEmpty()) {
								return pos;
							}
						}
					}
				}
			}
		}
		return null;
	}

	private static void onLogout(ServerPlayer player) {
		HuntData data = data(player);
		if (data.quest != null && data.quest.phase != HuntData.PHASE_KILLING) {
			resetToQuotaMinusOne(player, data);
		}
		SpiritLure.cleanup(player);
		SpiritLure.forget(player);
	}

	private static void onChangeLevel(ServerPlayer player) {
		HuntData data = data(player);
		if (data.quest != null && data.quest.phase != HuntData.PHASE_KILLING) {
			fail(player, data);
		}
		sync(player);
	}

	private static void onEntityLoad(Entity entity, ServerLevel level) {
		if (entity instanceof LivingEntity living && BossController.isBoss(living)) {
			UUID owner = BossController.ownerOf(entity);
			ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
			Quest quest = player == null ? null : data(player).quest;
			if (quest == null || !quest.bossUuid.equals(entity.getUUID().toString())) {
				entity.discard();
			}
		} else if (BossAbilities.isHerd(entity) && !BossAbilities.isTrackedHerd(entity)) {
			// Herd animals only live during their boss fight.
			entity.discard();
		} else if (SpiritLure.isLured(entity)) {
			UUID owner = SpiritLure.ownerOf(entity);
			ServerPlayer player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
			if (player == null || data(player).quest == null) {
				entity.discard();
			}
		}
	}

	// ---------------------------------------------------------------- helpers

	public static void playSound(ServerPlayer player, SoundEvent sound, float pitch) {
		player.level().playSound(null, player.blockPosition(), sound, SoundSource.PLAYERS, 1.0F, pitch);
	}

	public static void award(ServerPlayer player, String path) {
		var advancement = player.level().getServer().getAdvancements().get(HuntLike.id(path));
		if (advancement != null) {
			player.getAdvancements().award(advancement, "done");
		}
	}

	/** Sends the player's Hunt data and live quest values to their client. */
	public static void sync(ServerPlayer player) {
		HuntData data = data(player);
		CompoundTag tag = data.toTag();
		tag.putBoolean("peaceful", isPeaceful(player));
		Quest quest = data.quest;
		if (quest != null && quest.phase == HuntData.PHASE_BOSS) {
			long elapsed = player.level().getGameTime() - quest.bossSpawnTime;
			tag.putInt("bossTicksLeft", (int) Math.max(0, BOSS_TIME_LIMIT - elapsed));
			Entity boss = BossController.findBoss(player, quest);
			tag.putInt("bossEntity", boss == null ? -1 : boss.getId());
		} else {
			tag.putInt("bossEntity", -1);
		}
		ListTag huntable = new ListTag();
		List<String> names = new ArrayList<>();
		for (EntityType<?> type : SpiritLure.huntableHere(player)) {
			String name = type.getDescription().getString();
			if (!names.contains(name)) {
				names.add(name);
				huntable.add(StringTag.valueOf(name));
			}
		}
		tag.put("huntable", huntable);
		ServerPlayNetworking.send(player, new HuntPayloads.Sync(tag));
	}
}
