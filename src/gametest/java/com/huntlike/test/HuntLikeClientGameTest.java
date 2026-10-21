package com.huntlike.test;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.chicken.Chicken;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import com.huntlike.HuntLike;
import com.huntlike.client.screen.HuntMenuScreen;
import com.huntlike.hunt.BossController;
import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.QuestManager;

/** Plays a Tier I Animal quest end to end and screenshots every menu screen. */
@SuppressWarnings("UnstableApiUsage")
public class HuntLikeClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			singleplayer.getServer().runCommand("time set day");
			singleplayer.getServer().runCommand("gamerule doDaylightCycle false");
			addProgress(singleplayer, 0, 64);
			singleplayer.getServer().runOnServer(server -> QuestManager.giveStack(player(server),
					new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG, 32)));
			context.waitTicks(10);

			// Open the menu the way the altar does: from the server.
			singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
			context.waitForScreen(HuntMenuScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("01-quests-rail");
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.takeScreenshot("02-boss-card-tier1");
			context.getInput().pressKey(InputConstants.KEY_D);
			context.waitTicks(3);
			context.takeScreenshot("03-boss-card-tier2-locked");
			context.getInput().pressKey(InputConstants.KEY_A);

			context.setScreen(() -> new HuntMenuScreen(2));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.takeScreenshot("04-ranks-current");
			context.getInput().pressKey(InputConstants.KEY_D);
			context.waitTicks(3);
			context.takeScreenshot("05-ranks-next-snapshot");

			context.setScreen(() -> new HuntMenuScreen(1));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.takeScreenshot("06-gear");
			context.getInput().pressKey(InputConstants.KEY_A);
			context.waitTicks(3);
			context.takeScreenshot("06b-gear-armor");
			context.getInput().pressKey(InputConstants.KEY_A);
			context.waitTicks(3);
			context.takeScreenshot("06c-gear-weapons");
			double[] center = context.computeOnClient(client -> new double[] {
					client.getWindow().getScreenWidth() * 0.62, client.getWindow().getScreenHeight() * 0.6});
			context.getInput().setCursorPos(center[0], center[1]);
			context.getInput().scroll(-10);
			context.waitTicks(3);
			context.takeScreenshot("06d-gear-weapons-scrolled");

			// Start a Tier I quest with Enter on the boss card.
			context.setScreen(() -> new HuntMenuScreen(0));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(20);
			check(singleplayer, "quest started", data -> data.quest != null && data.quest.tier == 1);
			context.takeScreenshot("07-active-quest");
			context.setScreen(() -> null);

			// Fill the quota: the player kills four chickens.
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				ServerLevel level = player.level();
				for (int i = 0; i < 4; i++) {
					Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
					chicken.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
					level.addFreshEntity(chicken);
					chicken.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
				}
			});
			context.waitTicks(80);
			context.getInput().lookAt(new net.minecraft.core.BlockPos(3, -60, 0));
			context.waitTicks(5);
			check(singleplayer, "boss spawned", data -> data.quest != null && data.quest.phase == HuntData.PHASE_BOSS);
			context.takeScreenshot("08-boss-spawned");
			boolean glowing = context.computeOnClient(client -> {
				for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
					if (entity.getId() == com.huntlike.client.ClientHuntState.bossEntityId) {
						return client.shouldEntityAppearGlowing(entity);
					}
				}
				return false;
			});
			HuntLike.LOGGER.info("[test] boss glows for owner: {} (boss id {})", glowing, com.huntlike.client.ClientHuntState.bossEntityId);

			singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
			context.waitForScreen(HuntMenuScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("09-active-quest-boss");
			hover(context, "Drops");
			context.takeScreenshot("09b-drops-tooltip");
			context.setScreen(() -> null);

			// Let the boss fight for a few seconds, then kill it as the player.
			context.waitTicks(60);
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
				if (boss != null) {
					HuntLike.LOGGER.info("[test] Boss at {} health {}, player health {}", boss.blockPosition(), boss.getHealth(), player.getHealth());
				}
			});
			context.takeScreenshot("10-boss-fight");
			singleplayer.getServer().runOnServer(server -> {
				ServerPlayer player = player(server);
				Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
				if (boss == null) {
					throw new AssertionError("Boss not found");
				}
				HuntLike.LOGGER.info("[test] Boss {} health {}/{}", boss.getName().getString(), boss.getHealth(), boss.getMaxHealth());
				boss.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 1000.0F);
			});
			context.waitTicks(20);
			check(singleplayer, "quest complete", data -> data.quest == null && data.progress("animal").xp == 5
					&& data.progress("animal").bossKills[0] == 1);
			context.takeScreenshot("11-quest-complete");

			addProgress(singleplayer, 200, 0);
			context.waitTicks(10);
			context.setScreen(() -> new HuntMenuScreen(2));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.takeScreenshot("12-ranks-after");
			context.setScreen(() -> new HuntMenuScreen(0));
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_RETURN);
			context.waitTicks(3);
			context.getInput().pressKey(InputConstants.KEY_A);
			context.waitTicks(3);
			context.takeScreenshot("13-boss-card-defeated");
			hover(context, "Lore");
			context.takeScreenshot("14-lore-tooltip");
			context.setScreen(() -> null);
			tierFive(context, singleplayer);
			gear(context, singleplayer);
			HuntLike.LOGGER.info("[test] HuntLike client game test passed");
		}
	}

	/** Tier V: Herd Call, its immunity, Soul Strike, Apex Instinct and the Lunge. */
	private static void tierFive(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		addProgress(singleplayer, 10500, 32);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			HuntData data = QuestManager.data(player);
			for (int i = 0; i < 4; i++) {
				data.progress("animal").bossKills[i] = Math.max(1, data.progress("animal").bossKills[i]);
			}
			player.setAttached(QuestManager.DATA, data);
			player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
			player.setHealth(100.0F);
		});
		context.waitTicks(10);
		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("start", "animal", 5)));
		context.waitTicks(10);
		check(singleplayer, "tier V started", data -> data.quest != null && data.quest.tier == 5);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			for (int i = 0; i < 12; i++) {
				Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
				chicken.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
				level.addFreshEntity(chicken);
				chicken.hurtServer(level, level.damageSources().playerAttack(player), 100.0F);
			}
		});
		context.waitTicks(80);
		check(singleplayer, "tier V boss spawned", data -> data.quest != null && data.quest.phase == HuntData.PHASE_BOSS);

		// 240 -> below 180: Herd Call clamps at 180 and calls 6 wolves.
		hitBoss(singleplayer, 70.0F);
		context.waitTicks(5);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
			int herd = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), com.huntlike.hunt.BossAbilities::isHerd).size();
			HuntLike.LOGGER.info("[test] after herd call: boss health {} herd {}", boss.getHealth(), herd);
			if (herd != 6 || boss.getHealth() < 175) {
				throw new AssertionError("Herd Call failed");
			}
			float before = boss.getHealth();
			boss.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 50.0F);
			HuntLike.LOGGER.info("[test] immune while herd lives: {}", boss.getHealth() >= before);
			if (boss.getHealth() < before) {
				throw new AssertionError("Boss not immune during Herd Call");
			}
		});
		context.getInput().lookAt(new net.minecraft.core.BlockPos(3, -60, 0));
		context.waitTicks(10);
		context.takeScreenshot("15-herd-call");
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (Mob mob : player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(64), com.huntlike.hunt.BossAbilities::isHerd)) {
				mob.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 100.0F);
			}
		});
		context.waitTicks(10);

		// Below 120: Soul Strike for 18 true damage.
		float healthBefore = singleplayer.getServer().computeOnServer(server -> player(server).getHealth());
		hitBoss(singleplayer, 65.0F);
		context.waitTicks(15);
		float healthAfter = singleplayer.getServer().computeOnServer(server -> player(server).getHealth());
		HuntLike.LOGGER.info("[test] Soul Strike: player health {} -> {}", healthBefore, healthAfter);
		if (healthBefore - healthAfter < 17.0F) {
			throw new AssertionError("Soul Strike did not land");
		}

		// Below 96: Apex Instinct heals to 108.
		hitBoss(singleplayer, 30.0F);
		context.waitTicks(15);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
			HuntLike.LOGGER.info("[test] Apex Instinct: boss health {}", boss.getHealth());
			if (boss.getHealth() < 107.0F) {
				throw new AssertionError("Apex Instinct did not heal");
			}
		});
		context.waitTicks(20);
		context.takeScreenshot("16-apex-instinct");

		// Let a Lunge happen, then finish the fight.
		float beforeLunge = singleplayer.getServer().computeOnServer(server -> player(server).getHealth());
		context.waitTicks(220);
		float afterLunge = singleplayer.getServer().computeOnServer(server -> player(server).getHealth());
		HuntLike.LOGGER.info("[test] health over 11 s of Tier V fighting: {} -> {}", beforeLunge, afterLunge);
		context.takeScreenshot("17-tier5-fight");
		hitBoss(singleplayer, 1000.0F);
		context.waitTicks(20);
		check(singleplayer, "tier V complete", data -> data.quest == null && data.progress("animal").bossKills[4] == 1);
		context.takeScreenshot("18-tier5-complete");
	}

	/** v0.4: buy every weapon and armor stage, then check their effects. */
	private static void gear(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		addProgress(singleplayer, 26500, 0);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			var sharpness = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
					.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
			var sword = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_SWORD);
			sword.enchant(sharpness, 5);
			QuestManager.giveStack(player, sword);
			give(player, com.huntlike.registry.ModItems.LOW_QUALITY_MYSTERY_MEAT, 5);
			give(player, com.huntlike.registry.ModItems.DECENT_QUALITY_MYSTERY_MEAT, 10);
			give(player, com.huntlike.registry.ModItems.HIGH_QUALITY_MYSTERY_MEAT, 3);
			give(player, com.huntlike.registry.ModItems.LARGE_FEMUR, 2);
			give(player, com.huntlike.registry.ModItems.THICK_HIDE, 4);
			give(player, net.minecraft.world.item.Items.IRON_INGOT, 5);
			give(player, net.minecraft.world.item.Items.DIAMOND, 6);
			give(player, net.minecraft.world.item.Items.SOUL_SAND, 8);
			give(player, net.minecraft.world.item.Items.BONE_MEAL, 12);
			give(player, net.minecraft.world.item.Items.OBSIDIAN, 8);
			give(player, net.minecraft.world.item.Items.OAK_LOG, 64);
			give(player, net.minecraft.world.item.Items.OAK_LEAVES, 32);
			give(player, net.minecraft.world.item.Items.LEATHER, 16);
			give(player, net.minecraft.world.item.Items.BONE, 16);
			for (var piece : new net.minecraft.world.item.Item[] {net.minecraft.world.item.Items.LEATHER_HELMET,
					net.minecraft.world.item.Items.LEATHER_CHESTPLATE, net.minecraft.world.item.Items.LEATHER_LEGGINGS,
					net.minecraft.world.item.Items.LEATHER_BOOTS}) {
				give(player, piece, 1);
			}
		});
		context.waitTicks(10);
		context.setScreen(() -> new HuntMenuScreen(1));
		context.waitTicks(3);
		context.getInput().pressKey(InputConstants.KEY_RETURN);
		context.getInput().pressKey(InputConstants.KEY_D);
		context.waitTicks(5);
		context.takeScreenshot("19-gear-weapons-affordable");
		for (int value : new int[] {200, 201, 202, 203, 204, 0, 1, 2}) {
			context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new com.huntlike.network.HuntPayloads.Action("buy_gear", "animal", value)));
			context.waitTicks(5);
		}
		context.setScreen(() -> null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var inventory = player.getInventory();
			net.minecraft.world.item.ItemStack weapon = net.minecraft.world.item.ItemStack.EMPTY;
			int ecocide = 0;
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				var stack = inventory.getItem(i);
				if (stack.is(com.huntlike.gear.ModGear.BONE_PULVERIZER)) {
					weapon = stack;
				}
				if (com.huntlike.gear.ModGear.armorSetOf(stack.getItem()) == com.huntlike.gear.ModGear.ECOCIDE) {
					ecocide++;
				}
			}
			var sharpness = player.level().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
					.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS);
			int level = weapon.isEmpty() ? -1 : weapon.getEnchantments().getLevel(sharpness);
			HuntLike.LOGGER.info("[test] gear bought: pulverizer {} (Sharpness {}), ecocide pieces {}", !weapon.isEmpty(), level, ecocide);
			if (weapon.isEmpty() || level != 5 || ecocide != 4) {
				throw new AssertionError("Gear purchase failed");
			}
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, weapon.copy());
			weapon.setCount(0);
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				var stack = inventory.getItem(i);
				var set = com.huntlike.gear.ModGear.armorSetOf(stack.getItem());
				if (set == com.huntlike.gear.ModGear.ECOCIDE) {
					var slot = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).slot();
					player.setItemSlot(slot, stack.copy());
					stack.setCount(0);
				}
			}
		});
		context.waitTicks(10);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			HuntLike.LOGGER.info("[test] Ecocide worn: armor {} toughness {} max health {}", player.getArmorValue(),
					player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS), player.getMaxHealth());
			var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
			cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(100.0);
			cow.setHealth(100.0F);
			cow.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 0.0F, 0.0F);
			level.addFreshEntity(cow);
			player.attack(cow);
			boolean slowed = cow.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
			HuntLike.LOGGER.info("[test] Bone Pulverizer hit on a cow: {} damage, Fracture {}", 100.0F - cow.getHealth(), slowed);
			if (100.0F - cow.getHealth() < 13.0F || !slowed) {
				throw new AssertionError("Biohazard or Fracture failed");
			}
			var boss = EntityTypes.CHICKEN.create(level, EntitySpawnReason.COMMAND);
			boss.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0.0F, 0.0F);
			var egg = new com.huntlike.entity.BossEgg(level, boss, 2.0F);
			boolean allowed = com.huntlike.gear.GearEffects.allowDamage(player, level.damageSources().thrown(egg, boss), 2.0F);
			HuntLike.LOGGER.info("[test] Egg Shell blocks eggs: {}", !allowed);
			if (allowed) {
				throw new AssertionError("Egg Shell failed");
			}
		});
		context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(
				net.minecraft.client.Minecraft.getInstance().player));
		context.waitTicks(10);
		context.takeScreenshot("20-ecocide-inventory");
		double[] helmet = context.computeOnClient(client -> {
			double scale = client.getWindow().getGuiScale();
			int left = (client.getWindow().getGuiScaledWidth() - 176) / 2;
			int top = (client.getWindow().getGuiScaledHeight() - 166) / 2;
			return new double[] {(left + 16) * scale, (top + 16) * scale};
		});
		context.getInput().setCursorPos(helmet[0], helmet[1]);
		context.waitTicks(5);
		context.takeScreenshot("20b-helmet-tooltip");
		context.runOnClient(client -> {
			var stack = new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModGear.BONE_PULVERIZER);
			client.player.getInventory().setItem(9, stack);
		});
		context.setScreen(() -> null);
		context.getInput().pressKey(options -> options.keyTogglePerspective);
		context.getInput().pressKey(options -> options.keyTogglePerspective);
		context.waitTicks(10);
		context.takeScreenshot("21-ecocide-worn");
	}

	private static void give(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		QuestManager.giveStack(player, new net.minecraft.world.item.ItemStack(item, count));
	}

	private static void hitBoss(TestSingleplayerContext singleplayer, float amount) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
			if (boss == null) {
				throw new AssertionError("Boss not found");
			}
			boss.hurtServer(player.level(), player.level().damageSources().playerAttack(player), amount);
		});
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	private static void hover(ClientGameTestContext context, String button) {
		double[] pos = context.computeOnClient(client -> {
			if (client.gui.screen() instanceof HuntMenuScreen screen && screen.buttonCenter(button) != null) {
				int[] center = screen.buttonCenter(button);
				double scale = client.getWindow().getGuiScale();
				return new double[] {center[0] * scale, center[1] * scale};
			}
			return null;
		});
		if (pos == null) {
			throw new AssertionError("Button not found: " + button);
		}
		context.getInput().setCursorPos(pos[0], pos[1]);
		context.waitTicks(3);
	}

	private static void addProgress(TestSingleplayerContext singleplayer, int xp, int tokens) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			HuntData data = QuestManager.data(player);
			data.progress("animal").xp += xp;
			data.progress("animal").tokens += tokens;
			player.setAttached(QuestManager.DATA, data);
			QuestManager.sync(player);
		});
	}

	private static void check(TestSingleplayerContext singleplayer, String what, java.util.function.Predicate<HuntData> test) {
		boolean ok = singleplayer.getServer().computeOnServer(server -> test.test(QuestManager.data(player(server))));
		String state = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).toTag().toString());
		HuntLike.LOGGER.info("[test] {}: {} | {}", what, ok ? "OK" : "FAILED", state);
		if (!ok) {
			throw new AssertionError("Check failed: " + what + " | " + state);
		}
	}
}
