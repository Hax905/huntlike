package com.huntlike.test;

import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.core.BlockPos;
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
		});
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
			context.waitTicks(10);
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
			check(singleplayer, "quest complete (Rank 0: no Daily Quest charge used)", data -> data.quest == null
					&& data.progress("animal").xp == 5 && data.progress("animal").bossKills[0] == 1
					&& data.dailyCharges == HuntData.DAILY_CHARGES);
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
			context.getInput().pressKey(InputConstants.KEY_A);
			context.waitTicks(3);
			context.takeScreenshot("13-boss-card-defeated");
			hover(context, "Lore");
			context.takeScreenshot("14-lore-tooltip");
			context.setScreen(() -> null);
			tierFive(context, singleplayer);
			gear(context, singleplayer);
			koro(context, singleplayer);
			extras(context, singleplayer);
			multiplayer(context, singleplayer);
			trophy(context, singleplayer);
			catalystAndPulverizer(context, singleplayer);
			dailyRefill(context, singleplayer);
			clusterHunt(context, singleplayer);
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
		context.setScreen(() -> new HuntMenuScreen(0));
		context.waitTicks(3);
		for (int i = 0; i < 4; i++) {
			context.getInput().pressKey(InputConstants.KEY_D);
		}
		context.waitTicks(10);
		context.takeScreenshot("13c-tier5-card");
		context.setScreen(() -> null);
		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("start", "animal", 5)));
		context.waitTicks(10);
		check(singleplayer, "tier V started as a Daily Quest (Rank I+)", data -> data.quest != null && data.quest.tier == 5
				&& data.quest.daily && data.dailyCharges == HuntData.DAILY_CHARGES - 1);
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
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			HuntData data = QuestManager.data(player);
			data.concoction = true;
			data.progress("animal").dropsSeen.add("thick_hide");
			data.progress("animal").dropsSeen.add("large_femur");
			player.setAttached(QuestManager.DATA, data);
			QuestManager.sync(player);
		});
		singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
		context.waitForScreen(HuntMenuScreen.class);
		context.waitTicks(5);
		hover(context, "Drops");
		context.takeScreenshot("14a-drops-concoction");
		context.setScreen(() -> null);

		// 240 -> below 180: Herd Call clamps at 180 and calls 6 wolves.
		hitBoss(singleplayer, 70.0F);
		context.waitTicks(8);
		context.takeScreenshot("14b-herd-call-warning");
		context.waitTicks(25);
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
		context.waitTicks(6);
		context.takeScreenshot("15b-soul-strike-warning");
		context.waitTicks(25);
		float healthAfter = singleplayer.getServer().computeOnServer(server -> player(server).getHealth());
		HuntLike.LOGGER.info("[test] Soul Strike: player health {} -> {}", healthBefore, healthAfter);
		if (healthBefore - healthAfter < 17.0F) {
			throw new AssertionError("Soul Strike did not land");
		}

		// Below 96: Apex Instinct heals to 108.
		hitBossDownTo(singleplayer, 90.0F);
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
		int xpBefore = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).progress("animal").xp);
		hitBoss(singleplayer, 1000.0F);
		context.waitTicks(20);
		check(singleplayer, "tier V complete with Daily Quest x2 XP (+1600)", data -> data.quest == null
				&& data.progress("animal").bossKills[4] == 1 && data.progress("animal").xp == xpBefore + 1600);
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
			if (value == 200) {
				context.waitTicks(10);
				context.takeScreenshot("19b-gear-toast");
			}
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

	/** v0.5: Koro's four chapters, the bells, Koro Whakamate and the Soul Monolith. */
	private static void koro(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		addProgress(singleplayer, 50500, 0);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			give(player, net.minecraft.world.item.Items.MUSHROOM_STEW, 1);
			give(player, net.minecraft.world.item.Items.OAK_LOG, 16);
			give(player, com.huntlike.registry.ModItems.THICK_HIDE, 3);
			give(player, com.huntlike.registry.ModItems.LOW_QUALITY_MYSTERY_MEAT, 5);
			give(player, net.minecraft.world.item.Items.IRON_INGOT, 5);
			give(player, net.minecraft.world.item.Items.SAND, 10);
			give(player, net.minecraft.world.item.Items.GOLD_INGOT, 10);
			give(player, com.huntlike.registry.ModItems.DECENT_QUALITY_MYSTERY_MEAT, 3);
			give(player, com.huntlike.registry.ModItems.GOLDEN_MOSS, 1);
			give(player, com.huntlike.registry.ModItems.HIGH_QUALITY_MYSTERY_MEAT, 1);
			give(player, com.huntlike.registry.ModItems.SPIRITUAL_CATALYST, 1);
		});
		context.waitTicks(5);
		context.setScreen(() -> new HuntMenuScreen(1));
		context.waitTicks(5);
		context.takeScreenshot("22-misc-koro");
		context.setScreen(() -> new com.huntlike.client.screen.KoroScreen(null));
		context.waitTicks(5);
		context.takeScreenshot("23-koro-first-visit");
		for (int i = 0; i < 400; i++) {
			int done = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).koroDone);
			if (done >= 4) {
				break;
			}
			context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new com.huntlike.network.HuntPayloads.Action("koro_click", "animal", 0)));
			context.waitTicks(1);
			if (i == 8) {
				context.waitTicks(3);
				context.takeScreenshot("24-koro-chapter1");
			}
			int stepNow = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).koroStep);
			int doneNow = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).koroDone);
			if (doneNow == 3 && stepNow == 20) {
				context.waitTicks(3);
				context.takeScreenshot("25-koro-final-quest");
			}
		}
		context.waitTicks(5);
		context.takeScreenshot("26-koro-finished");
		check(singleplayer, "Koro questline finished", data -> data.koroDone == 4);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			ServerLevel level = player.level();
			var inventory = player.getInventory();
			boolean bell = false;
			boolean dagger = false;
			for (int i = 0; i < inventory.getContainerSize(); i++) {
				bell |= inventory.getItem(i).is(com.huntlike.gear.ModKoro.MOSSY_BELL);
				dagger |= inventory.getItem(i).is(com.huntlike.gear.ModKoro.KORO_WHAKAMATE);
			}
			HuntLike.LOGGER.info("[test] Koro rewards: Mossy Bell {} Koro Whakamate {}", bell, dagger);
			if (!bell || !dagger) {
				throw new AssertionError("Koro rewards missing");
			}
			// Mossy Bell: heal and Absorption II.
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModKoro.MOSSY_BELL));
			player.getMainHandItem().use(level, player, net.minecraft.world.InteractionHand.MAIN_HAND);
			var absorption = player.getEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
			HuntLike.LOGGER.info("[test] Mossy Bell absorption level {}", absorption == null ? -1 : absorption.getAmplifier());
			if (absorption == null || absorption.getAmplifier() != 1) {
				throw new AssertionError("Bell failed");
			}
			// Koro Whakamate on a cow (poison) and a zombie (wither).
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModKoro.KORO_WHAKAMATE));
			var cow = EntityTypes.COW.create(level, EntitySpawnReason.COMMAND);
			cow.snapTo(player.getX() + 1.5, player.getY(), player.getZ(), 0.0F, 0.0F);
			level.addFreshEntity(cow);
			player.attack(cow);
			var zombie = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
			zombie.snapTo(player.getX() - 1.5, player.getY(), player.getZ(), 0.0F, 0.0F);
			level.addFreshEntity(zombie);
			player.attack(zombie);
			HuntLike.LOGGER.info("[test] Whakamate: cow poisoned {} burning {}, zombie withered {}",
					cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON), cow.isOnFire(),
					zombie.hasEffect(net.minecraft.world.effect.MobEffects.WITHER));
			if (!cow.hasEffect(net.minecraft.world.effect.MobEffects.POISON)
					|| !zombie.hasEffect(net.minecraft.world.effect.MobEffects.WITHER)) {
				throw new AssertionError("Koro Whakamate effects failed");
			}
			zombie.discard();
			// Soul Monolith next to the player.
			level.setBlockAndUpdate(player.blockPosition().offset(2, 0, 2), com.huntlike.registry.ModBlocks.SOUL_MONOLITH.defaultBlockState());
		});
		context.waitTicks(20);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			boolean resistance = player.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
			HuntLike.LOGGER.info("[test] Soul Monolith resistance {}", resistance);
			if (!resistance) {
				throw new AssertionError("Soul Monolith failed");
			}
		});
		context.waitTicks(80);
		check(singleplayer, "cow killed by Fatal Touch", data -> true);
	}

	/** Golden Meat, the wolf treats, the Hide Boots line and the FeatherChute. */
	private static void extras(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		assertServer(singleplayer, "recipes loaded", server -> {
			for (String id : new String[] {"golden_meat", "thick_hide_boots", "feather_chute", "winged_hide_boots_smithing",
					"hardened_hide_boots_smithing", "speedy_hide_boots_smithing", "hermes_boots_smithing"}) {
				var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, HuntLike.id(id));
				if (server.getRecipeManager().byKey(key).isEmpty()) {
					HuntLike.LOGGER.error("[test] missing recipe {}", id);
					return false;
				}
			}
			return true;
		});
		assertServer(singleplayer, "golden meat eaten instantly", server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			player.getFoodData().setFoodLevel(4);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.GOLDEN_MEAT, 2));
			player.getMainHandItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			return player.getFoodData().getFoodLevel() == 12 && player.getMainHandItem().getCount() == 1 && !player.isUsingItem();
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var wolf = EntityTypes.WOLF.create(player.level(), EntitySpawnReason.COMMAND);
			wolf.setPos(player.getX() + 2, player.getY(), player.getZ());
			wolf.addTag("huntlike_test_wolf");
			player.level().addFreshEntity(wolf);
		});
		context.waitTicks(2);
		assertServer(singleplayer, "low quality meat tames a wolf", server -> {
			ServerPlayer player = player(server);
			var wolf = testWolf(player);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.LOW_QUALITY_MYSTERY_MEAT, 1));
			net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player, player.level(),
					net.minecraft.world.InteractionHand.MAIN_HAND, wolf, null);
			return wolf.isTame() && wolf.isOwnedBy(player);
		});
		assertServer(singleplayer, "large femur doubles wolf health and damage", server -> {
			ServerPlayer player = player(server);
			var wolf = testWolf(player);
			double health = wolf.getMaxHealth();
			double damage = wolf.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.LARGE_FEMUR, 2));
			net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player, player.level(),
					net.minecraft.world.InteractionHand.MAIN_HAND, wolf, null);
			net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.invoker().interact(player, player.level(),
					net.minecraft.world.InteractionHand.MAIN_HAND, wolf, null);
			HuntLike.LOGGER.info("[test] wolf health {} -> {}, damage {} -> {}", health, wolf.getMaxHealth(), damage,
					wolf.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
			return wolf.getMaxHealth() == health * 2 && wolf.getHealth() == wolf.getMaxHealth()
					&& Math.abs(wolf.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.SCALE) - 1.25) < 0.001
					&& wolf.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == damage * 2
					&& player.getMainHandItem().getCount() == 1 && hasAdvancement(player, "animal/happier_dog");
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			testWolf(player).discard();
			player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET,
					new net.minecraft.world.item.ItemStack(com.huntlike.gear.HideBoots.WINGED_HIDE_BOOTS));
			player.teleportTo(player.getX(), player.getY() + 30, player.getZ());
		});
		context.waitTicks(25);
		assertServer(singleplayer, "winged boots glide", server -> {
			ServerPlayer player = player(server);
			return player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)
					&& net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(player);
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.item.ItemStack.EMPTY);
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.gear.HideBoots.FEATHER_CHUTE));
			player.teleportTo(player.getX(), player.getY() + 40, player.getZ());
		});
		context.waitTicks(70);
		assertServer(singleplayer, "feather chute glides and wears", server -> {
			ServerPlayer player = player(server);
			var chute = player.getOffhandItem();
			HuntLike.LOGGER.info("[test] chute damage {}", chute.getDamageValue());
			return player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING) && chute.getDamageValue() >= 1
					&& !net.minecraft.world.level.block.PowderSnowBlock.canEntityWalkOnPowderSnow(player);
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, net.minecraft.world.item.ItemStack.EMPTY);
		});
	}

	/** A second (fake) player on the server: admin commands, two quests at once, a helper finishing someone else's boss. */
	private static void multiplayer(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		assertServer(singleplayer, "creative tabs Drops, Gear, Blocks", server -> {
			var registry = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB;
			return registry.getValue(HuntLike.id("drops")) != null && registry.getValue(HuntLike.id("gear")) != null
					&& registry.getValue(HuntLike.id("blocks")) != null && registry.getValue(HuntLike.id("huntlike")) == null;
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
			player.teleportTo(player.getX(), player.level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
					player.getBlockX(), player.getBlockZ()), player.getZ());
			var helper = net.fabricmc.fabric.api.entity.FakePlayer.get(player.level(),
					new com.mojang.authlib.GameProfile(java.util.UUID.fromString("00000000-0000-4000-8000-00000000beef"), "Helper"));
			helper.snapTo(player.getX() + 2, player.getY(), player.getZ(), 0, 0);
			addToPlayerList(server, helper);
		});
		context.waitTicks(2);
		singleplayer.getServer().runCommand("huntlike quest start Helper animal 2");
		singleplayer.getServer().runCommand("huntlike stats Helper");
		context.waitTicks(2);
		assertServer(singleplayer, "command starts a quest for another player", server -> {
			var quest = QuestManager.data(helper(server)).quest;
			return quest != null && quest.tier == 2 && quest.phase == HuntData.PHASE_KILLING
					&& QuestManager.data(player(server)).quest == null;
		});
		String name = singleplayer.getServer().computeOnServer(server -> player(server).getScoreboardName());
		singleplayer.getServer().runCommand("huntlike boss animal 1 " + name);
		context.waitTicks(60);
		assertServer(singleplayer, "command summons a boss; both quests run side by side", server -> {
			var mine = QuestManager.data(player(server)).quest;
			var theirs = QuestManager.data(helper(server)).quest;
			return mine != null && mine.phase == HuntData.PHASE_BOSS && BossController.findBoss(player(server), mine) != null
					&& theirs != null && theirs.phase == HuntData.PHASE_KILLING && theirs.tier == 2;
		});
		int killsBefore = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).progress("animal").bossKills[0]);
		assertServer(singleplayer, "helper's killing blow completes the owner's quest", server -> {
			ServerPlayer owner = player(server);
			ServerPlayer helper = helper(server);
			Mob boss = BossController.findBoss(owner, QuestManager.data(owner).quest);
			boss.hurtServer(owner.level(), owner.level().damageSources().playerAttack(owner), 2.0F);
			boss.hurtServer(owner.level(), owner.level().damageSources().playerAttack(helper), 1000.0F);
			return !boss.isAlive() && QuestManager.data(owner).quest == null
					&& QuestManager.data(owner).progress("animal").bossKills[0] == killsBefore + 1
					&& QuestManager.data(helper).quest != null && QuestManager.data(helper).progress("animal").bossKills[0] == 0;
		});
		assertServer(singleplayer, "helper's quota kill counts only for the helper", server -> {
			ServerPlayer helper = helper(server);
			var cow = EntityTypes.COW.create(helper.level(), EntitySpawnReason.COMMAND);
			cow.snapTo(helper.getX() + 1, helper.getY(), helper.getZ(), 0, 0);
			helper.level().addFreshEntity(cow);
			int before = QuestManager.data(helper).quest.kills;
			cow.hurtServer(helper.level(), helper.level().damageSources().playerAttack(helper), 1000.0F);
			return QuestManager.data(helper).quest.kills == before + 1 && QuestManager.data(player(server)).quest == null;
		});
		singleplayer.getServer().runCommand("huntlike quest start @a animal 1");
		context.waitTicks(2);
		singleplayer.getServer().runCommand("huntlike quest cancel @a");
		context.waitTicks(2);
		assertServer(singleplayer, "cancel command clears every quest", server ->
				QuestManager.data(player(server)).quest == null && QuestManager.data(helper(server)).quest == null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer helper = helper(server);
			removeFromPlayerList(server, helper);
			helper.discard();
		});
	}

	private static net.minecraft.world.item.ItemStack trophyStack = net.minecraft.world.item.ItemStack.EMPTY;

	private static void killAll(TestSingleplayerContext singleplayer, java.util.function.Function<ServerPlayer, List<? extends net.minecraft.world.entity.LivingEntity>> which) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (var mob : which.apply(player)) {
				mob.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 100000.0F);
			}
		});
	}

	/** The Cluster Hunt: Soul Sand tokens, whole-cluster quota, Mitosis, Baller, Yummy!, Watch out! and Thermodynamic. */
	private static void clusterHunt(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		String name = singleplayer.getServer().computeOnServer(server -> player(server).getScoreboardName());
		// Locked: below Animal Rank III the rail shows a padlock, and a selected Cluster Hunt reopens on Animal.
		int savedXp = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).progress("animal").xp);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			HuntData data = QuestManager.data(player);
			data.progress("animal").xp = 0;
			data.selectedHunt = "cluster";
			player.setAttached(QuestManager.DATA, data);
			QuestManager.sync(player);
		});
		context.waitTicks(3);
		singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
		context.waitForScreen(HuntMenuScreen.class);
		context.waitTicks(10);
		context.getInput().pressKey(InputConstants.KEY_S);
		context.waitTicks(5);
		context.takeScreenshot("39-cluster-locked");
		context.setScreen(() -> null);
		context.waitTicks(3);
		singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
		context.waitForScreen(HuntMenuScreen.class);
		context.waitTicks(5);
		check(singleplayer, "a locked Hunt reopens on Animal", data -> data.selectedHunt.equals("animal"));
		context.setScreen(() -> null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			HuntData data = QuestManager.data(player);
			data.progress("animal").xp = savedXp;
			player.setAttached(QuestManager.DATA, data);
			QuestManager.sync(player);
		});
		singleplayer.getServer().runCommand("execute as " + name + " run huntlike xp add 60 cluster");
		context.waitTicks(2);
		check(singleplayer, "/huntlike xp add <amount> cluster", data -> data.progress("cluster").xp == 60);
		singleplayer.getServer().runOnServer(server -> {
			HuntData data = QuestManager.data(player(server));
			data.progress("cluster").xp = 0;
			player(server).setAttached(QuestManager.DATA, data);
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var icy = com.huntlike.gear.ModGear.ICY;
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(icy.helmet()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(icy.chestplate()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(icy.leggings()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(icy.boots()));
		});
		context.waitTicks(3);
		assertServer(singleplayer, "Icy Armor full set is Slippery with iron armor's 15 points", server -> {
			ServerPlayer player = player(server);
			boolean slippery = com.huntlike.gear.GearEffects.slippery(player) && com.huntlike.gear.GearEffects.armorPieces(player) == 0;
			HuntLike.LOGGER.info("[test] Icy armor value {}", player.getArmorValue());
			return slippery && player.getArmorValue() == 15;
		});
		assertServer(singleplayer, "cluster hunt unlocked at Animal Rank III; Spirit Lure brings slimes here", server ->
				com.huntlike.hunt.HuntDefs.huntUnlocked(QuestManager.data(player(server)), com.huntlike.hunt.HuntDefs.CLUSTER)
						&& com.huntlike.hunt.SpiritLure.huntableHere(player(server), com.huntlike.hunt.HuntDefs.CLUSTER)
								.equals(List.of(EntityTypes.SLIME)));
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			give(player, net.minecraft.world.item.Items.SOUL_SAND, 5);
		});
		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("buy_tokens", "cluster", 0)));
		context.waitTicks(5);
		check(singleplayer, "5 Soul Sand -> 64 Cluster tokens", data -> data.progress("cluster").tokens == 64);

		// Menu: the Cluster Hunt's boss cards.
		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("select", "cluster", 0)));
		context.waitTicks(3);
		singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
		context.waitForScreen(HuntMenuScreen.class);
		context.waitTicks(10);
		context.takeScreenshot("40-cluster-card");
		for (int i = 0; i < 4; i++) {
			context.getInput().pressKey(InputConstants.KEY_D);
		}
		context.waitTicks(5);
		context.takeScreenshot("41-cluster-tier5-card");
		for (int i = 0; i < 4; i++) {
			context.getInput().pressKey(InputConstants.KEY_A);
		}
		context.setScreen(() -> new HuntMenuScreen(1));
		context.waitTicks(5);
		context.takeScreenshot("42-cluster-gear");
		context.setScreen(() -> null);

		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("start", "cluster", 1)));
		context.waitTicks(5);
		check(singleplayer, "cluster tier I started", data -> data.quest != null && data.quest.hunt.equals("cluster")
				&& data.quest.tier == 1 && data.quest.quota == 2);

		// A whole cluster counts once: a medium slime and both of its pieces = 1 point.
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var slime = EntityTypes.SLIME.create(player.level(), EntitySpawnReason.COMMAND);
			slime.setSize(2, true);
			slime.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			slime.setNoAi(true);
			player.level().addFreshEntity(slime);
		});
		context.waitTicks(3);
		killAll(singleplayer, player -> player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.cubemob.Slime.class,
				player.getBoundingBox().inflate(8), m -> m.getType() == EntityTypes.SLIME && m.isAlive()));
		context.waitTicks(30);
		int pieces = singleplayer.getServer().computeOnServer(server -> player(server).level().getEntitiesOfClass(
				net.minecraft.world.entity.monster.cubemob.Slime.class, player(server).getBoundingBox().inflate(8),
				m -> m.getType() == EntityTypes.SLIME && m.isAlive()).size());
		HuntLike.LOGGER.info("[test] medium slime split into {} pieces", pieces);
		check(singleplayer, "no quota point until the whole cluster is dead", data -> data.quest.kills == 0);
		killAll(singleplayer, player -> player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.cubemob.Slime.class,
				player.getBoundingBox().inflate(8), m -> m.getType() == EntityTypes.SLIME && m.isAlive()));
		context.waitTicks(3);
		check(singleplayer, "whole slime cluster = 1 point", data -> data.quest.kills == 1);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var magma = EntityTypes.MAGMA_CUBE.create(player.level(), EntitySpawnReason.COMMAND);
			magma.setSize(1, true);
			magma.snapTo(player.getX() + 3, player.getY(), player.getZ(), 0, 0);
			player.level().addFreshEntity(magma);
		});
		context.waitTicks(3);
		killAll(singleplayer, player -> player.level().getEntitiesOfClass(net.minecraft.world.entity.monster.cubemob.MagmaCube.class,
				player.getBoundingBox().inflate(8), m -> m.getType() == EntityTypes.MAGMA_CUBE && m.isAlive()));
		context.waitTicks(3);
		check(singleplayer, "magma cube cluster = 2 points, quota full", data -> data.quest.kills == 2
				&& data.quest.phase != HuntData.PHASE_KILLING);
		context.waitTicks(60);
		assertServer(singleplayer, "Cubert spawned as one main body", server -> {
			var bodies = com.huntlike.hunt.ClusterFight.bodies(player(server));
			return bodies.size() == 1 && bodies.get(0).getType() == com.huntlike.entity.ModEntities.CUBERT;
		});
		context.takeScreenshot("43-cubert");
		killAll(singleplayer, player -> com.huntlike.hunt.ClusterFight.bodies(player));
		context.waitTicks(2);
		assertServer(singleplayer, "Mitosis: Cubert split into 2", server -> com.huntlike.hunt.ClusterFight.bodies(player(server)).size() == 2);
		context.waitTicks(20);
		context.takeScreenshot("44-cubert-split");
		killAll(singleplayer, player -> com.huntlike.hunt.ClusterFight.bodies(player));
		context.waitTicks(5);
		check(singleplayer, "cluster tier I complete with Thermal Slimeballs", data -> data.quest == null
				&& data.progress("cluster").bossKills[0] == 1 && data.progress("cluster").dropCounts.getOrDefault("thermal_slimeball", 0) >= 1);

		// Tier V: The Vile.
		int ballsBefore = com.huntlike.hunt.ClusterFight.ballsThrown;
		singleplayer.getServer().runCommand("huntlike boss cluster 5 " + name);
		context.waitTicks(45);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var main = com.huntlike.hunt.ClusterFight.bodies(player).get(0);
			main.setNoAi(true);
			main.teleportTo(player.getX(), player.getY(), player.getZ() + 6);
		});
		context.runOnClient(client -> {
			client.player.setYRot(0.0F);
			client.player.setXRot(15.0F);
		});
		context.waitTicks(5);
		context.takeScreenshot("45a-the-vile-close");
		boolean core = singleplayer.getServer().computeOnServer(server ->
				((com.huntlike.entity.cluster.TheVile) com.huntlike.hunt.ClusterFight.bodies(player(server)).get(0)).isMagmaCore());
		context.waitTicks(110);
		context.takeScreenshot("45b-the-vile-switched");
		assertServer(singleplayer, "Thermodynamic switched the core; Baller threw a ball", server -> {
			var vile = (com.huntlike.entity.cluster.TheVile) com.huntlike.hunt.ClusterFight.bodies(player(server)).get(0);
			HuntLike.LOGGER.info("[test] core {} -> {}, balls {}", core, vile.isMagmaCore(), com.huntlike.hunt.ClusterFight.ballsThrown - ballsBefore);
			return vile.isMagmaCore() != core && com.huntlike.hunt.ClusterFight.ballsThrown > ballsBefore;
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var main = com.huntlike.hunt.ClusterFight.bodies(player).get(0);
			main.hurtServer(player.level(), player.level().damageSources().playerAttack(player), main.getMaxHealth() * 0.52F);
		});
		context.waitTicks(5);
		assertServer(singleplayer, "Yummy!: the owner is consumed", server -> com.huntlike.hunt.ClusterFight.isConsumed(player(server))
				&& com.huntlike.hunt.ClusterFight.yummyKeys(player(server)).length == 15);
		context.waitTicks(10);
		context.takeScreenshot("46-yummy");
		int[] keys = context.computeOnClient(client -> com.huntlike.client.YummyClient.keys.clone());
		HuntLike.LOGGER.info("[test] Yummy! keys {}", java.util.Arrays.toString(keys));
		singleplayer.getServer().runOnServer(server -> com.huntlike.hunt.ClusterFight.onYummyKey(player(server), (keys[0] + 1) % 7));
		context.waitTicks(2);
		assertServer(singleplayer, "a wrong key does nothing", server -> com.huntlike.hunt.ClusterFight.yummyProgress(player(server)) == 0);
		for (int key : keys) {
			context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
					new com.huntlike.network.HuntPayloads.Action("yummy_key", "cluster", key)));
		}
		context.waitTicks(5);
		assertServer(singleplayer, "escaped after the whole sequence", server -> !com.huntlike.hunt.ClusterFight.isConsumed(player(server)));
		context.waitTicks(30);
		assertServer(singleplayer, "Watch out! started (below 75%)", server -> com.huntlike.hunt.ClusterFight.isWatching(player(server)));
		context.takeScreenshot("47-watch-out");
		context.waitTicks(250);
		killAll(singleplayer, player -> com.huntlike.hunt.ClusterFight.bodies(player));
		context.waitTicks(2);
		assertServer(singleplayer, "Mitosis: The Vile split into 4", server -> com.huntlike.hunt.ClusterFight.bodies(player(server)).size() == 4);
		killAll(singleplayer, player -> com.huntlike.hunt.ClusterFight.bodies(player));
		context.waitTicks(2);
		assertServer(singleplayer, "...and does it keep going? 16", server -> com.huntlike.hunt.ClusterFight.bodies(player(server)).size() == 16);
		context.waitTicks(20);
		context.takeScreenshot("48-vile-sixteen");
		killAll(singleplayer, player -> com.huntlike.hunt.ClusterFight.bodies(player));
		context.waitTicks(5);
		check(singleplayer, "cluster tier V complete", data -> data.quest == null && data.progress("cluster").bossKills[4] == 1);
	}

	/** Daily Quest charges: commands don't use them; a new world day refills all five. */
	private static void dailyRefill(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		int before = singleplayer.getServer().computeOnServer(server -> QuestManager.data(player(server)).dailyCharges);
		HuntLike.LOGGER.info("[test] daily charges before refill: {}", before);
		singleplayer.getServer().runCommand("huntlike quest start @a animal 1");
		context.waitTicks(2);
		check(singleplayer, "command quests use no charge", data -> data.quest != null && !data.quest.daily
				&& data.dailyCharges == before);
		singleplayer.getServer().runCommand("huntlike quest cancel @a");
		singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
		context.waitForScreen(HuntMenuScreen.class);
		context.waitTicks(25);
		context.takeScreenshot("31-daily-charges-used");
		hover(context, "Daily Quest Charges");
		context.takeScreenshot("31b-daily-charges-tooltip");
		singleplayer.getServer().runCommand("time add 24000");
		context.waitTicks(25);
		check(singleplayer, "a new day refills the charges", data -> data.dailyCharges == HuntData.DAILY_CHARGES);
		context.takeScreenshot("32-daily-charges-refilled");
		addProgress(singleplayer, 0, 64);
		context.runOnClient(client -> net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
				new com.huntlike.network.HuntPayloads.Action("start", "animal", 1)));
		context.waitTicks(25);
		check(singleplayer, "menu quest uses a charge", data -> data.quest != null && data.quest.daily
				&& data.dailyCharges == HuntData.DAILY_CHARGES - 1);
		context.takeScreenshot("33-daily-active-quest");
		hover(context, "Drops");
		context.takeScreenshot("34-daily-drops");
		singleplayer.getServer().runCommand("huntlike quest cancel @a");
		context.setScreen(() -> null);
		context.waitTicks(5);
		context.takeScreenshot("35-daily-refill-message");
	}

	/** Spiritual Catalyst highlights the nearest living entity (client side); the Bone Pulverizer in hand. */
	private static void catalystAndPulverizer(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		int cowId = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					player.getBoundingBox().inflate(40), e -> e != player)) {
				e.discard();
			}
			var cow = EntityTypes.COW.create(player.level(), EntitySpawnReason.COMMAND);
			cow.snapTo(player.getX() - 4, player.getY(), player.getZ() + 1, 0, 0);
			cow.setNoAi(true);
			player.level().addFreshEntity(cow);
			var far = EntityTypes.PIG.create(player.level(), EntitySpawnReason.COMMAND);
			far.snapTo(player.getX() - 9, player.getY(), player.getZ() - 2, 0, 0);
			far.setNoAi(true);
			player.level().addFreshEntity(far);
			return cow.getId();
		});
		context.waitTicks(3);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var stack = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.SPIRITUAL_CATALYST);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			stack.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
		boolean glowing = context.computeOnClient(client -> {
			var cow = client.level.getEntity(cowId);
			return cow != null && client.shouldEntityAppearGlowing(cow);
		});
		HuntLike.LOGGER.info("[test] Spiritual Catalyst highlights the nearest entity: {}", glowing ? "OK" : "FAILED");
		if (!glowing) {
			throw new AssertionError("Spiritual Catalyst highlight");
		}
		context.takeScreenshot("28-catalyst-highlight");
		context.waitTicks(70);
		boolean faded = context.computeOnClient(client -> !client.shouldEntityAppearGlowing(client.level.getEntity(cowId)));
		HuntLike.LOGGER.info("[test] highlight fades after 3 seconds: {}", faded ? "OK" : "FAILED");
		if (!faded) {
			throw new AssertionError("Spiritual Catalyst highlight did not fade");
		}
		singleplayer.getServer().runOnServer(server -> player(server).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModGear.BONE_BREAKER)));
		context.waitTicks(10);
		context.takeScreenshot("29a-breaker-first-person");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("29b-breaker-third-person");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		singleplayer.getServer().runOnServer(server -> player(server).setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModGear.BONE_PULVERIZER)));
		context.waitTicks(10);
		context.takeScreenshot("29-pulverizer-first-person");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));
		context.waitTicks(10);
		context.takeScreenshot("30-pulverizer-third-person");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
	}

	/** The 100th Tier V boss gives the Animal Hunting Trophy, which keeps its lore when placed and broken. */
	private static void trophy(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		context.setScreen(() -> null);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.getInventory().clearContent();
			HuntData data = QuestManager.data(player);
			data.progress("animal").bossKills[4] = 99;
			player.setAttached(QuestManager.DATA, data);
		});
		String name = singleplayer.getServer().computeOnServer(server -> player(server).getScoreboardName());
		singleplayer.getServer().runCommand("huntlike boss animal 5 " + name);
		context.waitTicks(60);
		assertServer(singleplayer, "100th Tier V boss gives the trophy", server -> {
			ServerPlayer player = player(server);
			Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
			boss.hurtServer(player.level(), player.level().damageSources().playerAttack(player), 100000.0F);
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				var stack = player.getInventory().getItem(i);
				if (stack.is(com.huntlike.registry.ModBlocks.ANIMAL_TROPHY.asItem())) {
					String lore = stack.get(net.minecraft.core.component.DataComponents.LORE).lines().stream()
							.map(net.minecraft.network.chat.Component::getString).reduce("", (a, b) -> a + b + " ");
					HuntLike.LOGGER.info("[test] trophy lore: {}", lore);
					trophyStack = stack.copy();
					return lore.contains(player.getScoreboardName()) && lore.contains("mastery over animal hunting");
				}
			}
			return false;
		});
		assertServer(singleplayer, "placed trophy keeps its lore when broken", server -> {
			ServerPlayer player = player(server);
			player.connection.teleport(player.getX(), player.getY(), player.getZ(), 90, 10);
			var stack = trophyStack;
			var level = player.level();
			BlockPos display = player.blockPosition().offset(-2, 1, 0);
			BlockPos broken = player.blockPosition().offset(3, 1, 3);
			for (BlockPos pos : new BlockPos[] {display, broken}) {
				var facing = pos == display ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.NORTH;
				level.setBlockAndUpdate(pos.relative(facing.getOpposite()), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
				level.setBlockAndUpdate(pos, com.huntlike.registry.ModBlocks.ANIMAL_TROPHY.defaultBlockState()
						.setValue(com.huntlike.block.TrophyBlock.FACING, facing));
				level.getBlockEntity(pos).applyComponentsFromItemStack(stack);
			}
			HuntLike.LOGGER.info("[test] trophy stack {} lore {}, block entity lore {}", stack,
					stack.get(net.minecraft.core.component.DataComponents.LORE),
					((com.huntlike.block.TrophyBlockEntity) level.getBlockEntity(broken)).lore());
			level.destroyBlock(broken, true, player);
			for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(broken).inflate(2), e -> true)) {
				HuntLike.LOGGER.info("[test] dropped {} lore {}", e.getItem(), e.getItem().get(net.minecraft.core.component.DataComponents.LORE));
			}
			return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(broken).inflate(2), e -> e.getItem().is(stack.getItem())
							&& e.getItem().get(net.minecraft.core.component.DataComponents.LORE) != null
							&& e.getItem().get(net.minecraft.core.component.DataComponents.LORE).equals(
									stack.get(net.minecraft.core.component.DataComponents.LORE))).size() == 1;
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (int slot = 0; slot < 9; slot++) {
				player.getInventory().setItem(slot, trophyStack.copy());
			}
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (var e : player.level().getEntities(player, player.getBoundingBox().inflate(6))) {
				HuntLike.LOGGER.info("[test] near trophy screenshot: {} {} alive={} age={} tags={} uuid={}", e.getType(), e.position(),
						e.isAlive(), e.tickCount, e.entityTags(), e.getUUID());
			}
		});
		context.runOnClient(client -> {
			client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
			client.player.setYRot(90.0F);
			client.player.setXRot(10.0F);
		});
		context.waitTicks(60);
		context.takeScreenshot("27-trophy-message");
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.waitTicks(3);
		context.takeScreenshot("27b-trophy-placed");
	}

	private static ServerPlayer helper(MinecraftServer server) {
		return server.getPlayerList().getPlayer(java.util.UUID.fromString("00000000-0000-4000-8000-00000000beef"));
	}

	@SuppressWarnings("unchecked")
	private static void addToPlayerList(MinecraftServer server, ServerPlayer player) {
		try {
			var list = server.getPlayerList();
			var players = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
			var byUuid = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
			players.setAccessible(true);
			byUuid.setAccessible(true);
			((java.util.List<ServerPlayer>) players.get(list)).add(player);
			((java.util.Map<java.util.UUID, ServerPlayer>) byUuid.get(list)).put(player.getUUID(), player);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Could not add a fake player", e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void removeFromPlayerList(MinecraftServer server, ServerPlayer player) {
		try {
			var list = server.getPlayerList();
			var players = net.minecraft.server.players.PlayerList.class.getDeclaredField("players");
			var byUuid = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
			players.setAccessible(true);
			byUuid.setAccessible(true);
			((java.util.List<ServerPlayer>) players.get(list)).remove(player);
			((java.util.Map<java.util.UUID, ServerPlayer>) byUuid.get(list)).remove(player.getUUID());
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Could not remove a fake player", e);
		}
	}

	private static net.minecraft.world.entity.animal.wolf.Wolf testWolf(ServerPlayer player) {
		return player.level().getEntitiesOfClass(net.minecraft.world.entity.animal.wolf.Wolf.class,
				player.getBoundingBox().inflate(32), w -> w.entityTags().contains("huntlike_test_wolf")).getFirst();
	}

	private static boolean hasAdvancement(ServerPlayer player, String path) {
		var holder = player.level().getServer().getAdvancements().get(HuntLike.id(path));
		return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
	}

	private static void assertServer(TestSingleplayerContext singleplayer, String what,
			java.util.function.Predicate<MinecraftServer> test) {
		boolean ok = singleplayer.getServer().computeOnServer(test::test);
		HuntLike.LOGGER.info("[test] {}: {}", what, ok ? "OK" : "FAILED");
		if (!ok) {
			throw new AssertionError("Check failed: " + what);
		}
	}

	private static void give(ServerPlayer player, net.minecraft.world.item.Item item, int count) {
		QuestManager.giveStack(player, new net.minecraft.world.item.ItemStack(item, count));
	}

	/** Damages the boss so its health drops to about the given value. */
	private static void hitBossDownTo(TestSingleplayerContext singleplayer, float health) {
		float current = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			Mob boss = BossController.findBoss(player, QuestManager.data(player).quest);
			return boss == null ? 0.0F : boss.getHealth();
		});
		hitBoss(singleplayer, Math.max(1.0F, current - health));
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
