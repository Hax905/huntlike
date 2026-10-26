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
			scythes(context, singleplayer);
			slimePet(context, singleplayer);
			upgradedMonolith(context, singleplayer);
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
		wear(singleplayer, com.huntlike.gear.ModGear.GEL);
		context.waitTicks(3);
		assertServer(singleplayer, "Gel Armor: 20 armor, 6 toughness, Better Than Ever!", server -> {
			ServerPlayer player = player(server);
			double toughness = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
			HuntLike.LOGGER.info("[test] Gel armor {} toughness {}", player.getArmorValue(), toughness);
			return player.getArmorValue() == 20 && Math.abs(toughness - 6.0) < 0.01 && com.huntlike.gear.GearEffects.betterThanEver(player)
					&& !com.huntlike.gear.GearEffects.roomTemperature(player);
		});
		assertServer(singleplayer, "Thermodynamic lore: each ability once, each marked (full set)", server -> {
			var lore = new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModGear.THERMODYNAMIC.helmet())
					.get(net.minecraft.core.component.DataComponents.LORE).lines();
			List<String> lines = lore.stream().map(net.minecraft.network.chat.Component::getString).toList();
			HuntLike.LOGGER.info("[test] Thermodynamic lore {}", lines);
			return lines.stream().filter(l -> l.startsWith("Better Than Ever!")).count() == 1
					&& lines.stream().filter(l -> l.startsWith("Room Temperature")).count() == 1
					&& java.util.Collections.frequency(lines, "(full set)") == 3;
		});
		wear(singleplayer, com.huntlike.gear.ModGear.THERMODYNAMIC);
		context.waitTicks(3);
		assertServer(singleplayer, "Thermodynamic Armor: 20 armor, 12 toughness, 0.4 knockback resistance, Room Temperature", server -> {
			ServerPlayer player = player(server);
			double toughness = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
			double knockback = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);
			HuntLike.LOGGER.info("[test] Thermodynamic armor {} toughness {} knockback {}", player.getArmorValue(), toughness, knockback);
			return player.getArmorValue() == 20 && Math.abs(toughness - 12.0) < 0.01 && Math.abs(knockback - 0.4) < 0.01
					&& com.huntlike.gear.GearEffects.roomTemperature(player);
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setRemainingFireTicks(100);
			player.setTicksFrozen(100);
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 200, 0));
		});
		context.waitTicks(2);
		assertServer(singleplayer, "Room Temperature: no burning, freezing, Slowness or lava damage", server -> {
			ServerPlayer player = player(server);
			boolean lava = com.huntlike.gear.GearEffects.allowDamage(player, player.level().damageSources().lava(), 4.0F);
			boolean freeze = com.huntlike.gear.GearEffects.allowDamage(player, player.level().damageSources().freeze(), 1.0F);
			HuntLike.LOGGER.info("[test] Room Temperature fire {} frozen {} slow {} lava {} freeze {}", player.getRemainingFireTicks(),
					player.getTicksFrozen(), player.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS), lava, freeze);
			return player.getRemainingFireTicks() <= 0 && player.getTicksFrozen() == 0
					&& !player.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) && !lava && !freeze;
		});
		wear(singleplayer, com.huntlike.gear.ModGear.ICY);
		context.waitTicks(3);
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
			context.waitTicks(5);
			if (i < 3) {
				context.takeScreenshot("40" + (char) ('b' + i) + "-cluster-tier" + (i + 2) + "-card");
			}
		}
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

		// Tier V: The Vile (in Gel Armor, for Better Than Ever!).
		wear(singleplayer, com.huntlike.gear.ModGear.GEL);
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
		for (int i = 0; i < 20 && com.huntlike.hunt.ClusterFight.ballsThrown == ballsBefore; i++) {
			context.waitTicks(10);
		}
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
		assertServer(singleplayer, "escaping Yummy!: Slowness I for 5 s; Gel's Better Than Ever! makes Temperature x1.2", server -> {
			ServerPlayer player = player(server);
			var slow = player.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
			HuntLike.LOGGER.info("[test] after Yummy! slowness {} multiplier {}", slow,
					com.huntlike.hunt.ClusterFight.temperatureMultiplier(player));
			return slow != null && slow.getAmplifier() == 0 && slow.getDuration() > 80
					&& Math.abs(com.huntlike.hunt.ClusterFight.temperatureMultiplier(player) - 1.2F) < 0.001F;
		});
		singleplayer.getServer().runOnServer(server -> player(server).setHealth(player(server).getMaxHealth()));
		context.waitTicks(30);
		assertServer(singleplayer, "Watch out! started (below 75%)", server -> com.huntlike.hunt.ClusterFight.isWatching(player(server)));
		context.takeScreenshot("47-watch-out");
		// Watch out! now deals double damage: keep the test player alive through all three rounds, even when the server lags.
		singleplayer.getServer().runOnServer(server -> player(server).addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.RESISTANCE, 300, 4, false, false)));
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
		assertServer(singleplayer, "Better Than Ever! ends when the boss perishes", server ->
				!com.huntlike.hunt.ClusterFight.betterThanEver(player(server)));
	}

	private static boolean done(ServerPlayer player, String path) {
		var advancement = player.level().getServer().getAdvancements().get(HuntLike.id(path));
		return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
	}

	private static net.minecraft.world.entity.LivingEntity spawnTest(ServerPlayer player, net.minecraft.world.entity.EntityType<?> type,
			double dx, double dz, float health) {
		var entity = (net.minecraft.world.entity.Mob) type.create(player.level(), EntitySpawnReason.COMMAND);
		if (entity instanceof net.minecraft.world.entity.monster.cubemob.AbstractCubeMob cube) {
			cube.setSize(1, true);
		}
		entity.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(health);
		entity.setHealth(health);
		entity.snapTo(player.getX() + dx, player.getY(), player.getZ() + dz, 0, 0);
		entity.setNoAi(true);
		player.level().addFreshEntity(entity);
		return entity;
	}

	/** Cluster Hunt Weapons, the Spiritual Compass, Beef Jerky and the new advancements. */
	private static void scythes(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		assertServer(singleplayer, "Cluster boss advancements; The Hunt Begins is shared", server -> {
			ServerPlayer player = player(server);
			return done(player, "cluster/tier1") && done(player, "cluster/tier5") && done(player, "animal/conquest");
		});
		int[] ids = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			wearNothing(player);
			for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					player.getBoundingBox().inflate(40), e -> e != player)) {
				e.discard();
			}
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,
					new net.minecraft.world.item.ItemStack(com.huntlike.gear.ModScythes.SLIMY_FALX));
			var slime = spawnTest(player, EntityTypes.SLIME, 2.0, 0.0, 100.0F);
			var near = spawnTest(player, EntityTypes.PIG, 2.0, 2.5, 10.0F);
			var far = spawnTest(player, EntityTypes.PIG, 2.0, 5.5, 10.0F);
			var magma = spawnTest(player, EntityTypes.MAGMA_CUBE, -2.0, 0.0, 100.0F);
			return new int[] {slime.getId(), near.getId(), far.getId(), magma.getId()};
		});
		context.waitTicks(25);
		singleplayer.getServer().runOnServer(server -> player(server).attack(player(server).level().getEntity(ids[0])));
		context.waitTicks(25);
		singleplayer.getServer().runOnServer(server -> player(server).attack(player(server).level().getEntity(ids[3])));
		context.waitTicks(2);
		float[] searing = singleplayer.getServer().computeOnServer(server -> {
			var level = player(server).level();
			float[] health = new float[4];
			for (int i = 0; i < 4; i++) {
				health[i] = ((net.minecraft.world.entity.LivingEntity) level.getEntity(ids[i])).getHealth();
			}
			HuntLike.LOGGER.info("[test] Slimy Falx (Searing) hit: slime {}, pig in sweep {}, far pig {}, magma cube {}",
					health[0], health[1], health[2], health[3]);
			return health;
		});
		assertServer(singleplayer, "Slimy Falx, Searing: slime takes (4 + 4) x 1.05; the sweep hits a pig 3 blocks away for 4, not one 6 away",
				server -> Math.abs(searing[0] - (100.0F - 8.4F)) < 0.05F && Math.abs(searing[1] - 6.0F) < 0.05F && searing[2] == 10.0F);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setShiftKeyDown(true);
			player.getMainHandItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			player.getMainHandItem().use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
		});
		assertServer(singleplayer, "Sublimation: sneak + right-click switches to Freezing once (2 s cooldown)", server -> {
			var stack = player(server).getMainHandItem();
			var model = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA);
			var lore = stack.get(net.minecraft.core.component.DataComponents.LORE).lines();
			HuntLike.LOGGER.info("[test] Slimy Falx lore {}", lore.stream().map(net.minecraft.network.chat.Component::getString).toList());
			return lore.get(0).getString().equals("Sweeping Weapon")
					&& lore.get(lore.size() - 1).getStyle().getColor().getValue() == com.huntlike.gear.ModScythes.FREEZING_COLOR
					&& com.huntlike.gear.ModScythes.isFrost(stack) && model != null && Boolean.TRUE.equals(model.getBoolean(0))
					&& player(server).getCooldowns().isOnCooldown(stack);
		});
		context.runOnClient(client -> {
			client.player.setYRot(90.0F);
			client.player.setXRot(10.0F);
		});
		context.waitTicks(25);
		context.takeScreenshot("50-scythe-frost");
		singleplayer.getServer().runOnServer(server -> player(server).attack(player(server).level().getEntity(ids[3])));
		context.waitTicks(2);
		assertServer(singleplayer, "Freezing hits the magma cube harder than Searing did", server -> {
			float now = ((net.minecraft.world.entity.LivingEntity) player(server).level().getEntity(ids[3])).getHealth();
			float searingHit = 100.0F - searing[3];
			float freezingHit = searing[3] - now;
			HuntLike.LOGGER.info("[test] magma cube: Searing hit {}, Freezing hit {}", searingHit, freezingHit);
			return freezingHit > searingHit + 0.1F;
		});

		// Slice and Dice: a cluster finished with a scythe counts double.
		singleplayer.getServer().runCommand("huntlike quest start @a cluster 1");
		context.waitTicks(3);
		int slimeId = singleplayer.getServer().computeOnServer(server -> spawnTest(player(server), EntityTypes.SLIME, 2.0, -1.0, 1.0F).getId());
		context.waitTicks(25);
		singleplayer.getServer().runOnServer(server -> player(server).attack(player(server).level().getEntity(slimeId)));
		context.waitTicks(3);
		check(singleplayer, "Slice and Dice: a slime cluster killed with a scythe = 2 quota points", data -> data.quest != null
				&& data.quest.kills == 2);
		singleplayer.getServer().runCommand("huntlike quest cancel @a");

		// Spiritual Compass: the 3 nearest souls glow.
		int[] pigs = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					player.getBoundingBox().inflate(40), e -> e != player)) {
				e.discard();
			}
			int[] result = new int[4];
			double[] distance = {3, 5, 7, 12};
			for (int i = 0; i < 4; i++) {
				result[i] = spawnTest(player, EntityTypes.PIG, -distance[i], 0.0, 10.0F).getId();
			}
			return result;
		});
		context.waitTicks(3);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var stack = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.SPIRITUAL_COMPASS);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
			stack.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
		});
		context.waitTicks(5);
		boolean compass = context.computeOnClient(client -> client.shouldEntityAppearGlowing(client.level.getEntity(pigs[0]))
				&& client.shouldEntityAppearGlowing(client.level.getEntity(pigs[1]))
				&& client.shouldEntityAppearGlowing(client.level.getEntity(pigs[2]))
				&& !client.shouldEntityAppearGlowing(client.level.getEntity(pigs[3])));
		HuntLike.LOGGER.info("[test] Spiritual Compass highlights the 3 nearest souls: {}", compass ? "OK" : "FAILED");
		if (!compass) {
			throw new AssertionError("Spiritual Compass");
		}
		context.takeScreenshot("51-spiritual-compass");

		// Warm and Slimy Beef Jerky and the 10,000 lured kills.
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var jerky = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.WARM_AND_SLIMY_BEEF_JERKY, 2);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, jerky);
			jerky.finishUsingItem(player.level(), player);
			var again = jerky.use(player.level(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			HuntLike.LOGGER.info("[test] Beef Jerky: jerky {} second bite {} cluster odds 1/{}", QuestManager.data(player).jerky, again,
					com.huntlike.hunt.HuntDefs.odds("cluster", 1, 25, com.huntlike.hunt.HuntDefs.luck(QuestManager.data(player), "cluster")));
			if (!QuestManager.data(player).jerky || again != net.minecraft.world.InteractionResult.FAIL
					|| com.huntlike.hunt.HuntDefs.odds("cluster", 1, 25, com.huntlike.hunt.HuntDefs.luck(QuestManager.data(player), "cluster")) != 24) {
				throw new AssertionError("Beef Jerky: once, Cluster odds 1/25 -> 1/24");
			}
			HuntData data = QuestManager.data(player);
			data.luredKills = com.huntlike.hunt.QuestManager.NOT_SELF_DEFENSE_KILLS - 1;
			QuestManager.countLuredKill(player, data);
		});
		context.waitTicks(3);
		assertServer(singleplayer, "The texture was nice at least and Ok ok, it might not be just self-defense", server ->
				done(player(server), "cluster/jerky") && done(player(server), "animal/not_self_defense"));
	}

	private static List<com.huntlike.pet.PetCube> petPieces(MinecraftServer server) {
		ServerPlayer player = player(server);
		return com.huntlike.pet.PetManager.pieces(player.level(), player, com.huntlike.pet.PetManager.latest(player), player).stream()
				.filter(net.minecraft.world.entity.LivingEntity::isAlive).toList();
	}

	private static void petInteract(TestSingleplayerContext singleplayer, net.minecraft.world.item.ItemStack stack, int times) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			for (int i = 0; i < times; i++) {
				List<com.huntlike.pet.PetCube> pieces = petPieces(server);
				if (pieces.isEmpty()) {
					return;
				}
				player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack.copy());
				com.huntlike.pet.PetManager.interact(pieces.get(0), player, net.minecraft.world.InteractionHand.MAIN_HAND);
			}
		});
	}

	/** The dropped Slimy Core, on the ground or already picked up. */
	private static net.minecraft.world.item.ItemStack petCore(ServerPlayer player) {
		var dropped = player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				player.getBoundingBox().inflate(40), e -> e.getItem().is(com.huntlike.registry.ModItems.SLIME_PET_CORE));
		if (!dropped.isEmpty()) {
			return dropped.get(0).getItem();
		}
		var inventory = player.getInventory();
		for (int i = 0; i < inventory.getContainerSize(); i++) {
			if (inventory.getItem(i).is(com.huntlike.registry.ModItems.SLIME_PET_CORE)) {
				return inventory.getItem(i);
			}
		}
		return net.minecraft.world.item.ItemStack.EMPTY;
	}

	private static void killPet(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			for (var piece : petPieces(server)) {
				piece.hurtServer(player(server).level(), player(server).level().damageSources().genericKill(), Float.MAX_VALUE);
			}
		});
		context.waitTicks(30);
	}

	private static String petInfo(MinecraftServer server) {
		var state = com.huntlike.pet.PetManager.latest(player(server));
		StringBuilder pieces = new StringBuilder();
		for (var piece : petPieces(server)) {
			pieces.append(piece.isMagma() ? "magma" : "slime").append(piece.getSize()).append('/').append(piece.getMaxHealth()).append(' ');
		}
		return "status " + state.status + " big " + state.big + " ionic " + state.ionic + " magma " + state.magma + " heat " + state.heat
				+ " chest " + state.chest + " page " + state.page + " pieces [" + pieces.toString().trim() + "]";
	}

	/** Soul Monolith ★: a block more reach and Regeneration I; the Beef Jerky is in the creative tab. */
	private static void upgradedMonolith(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
			player.removeEffect(net.minecraft.world.effect.MobEffects.RESISTANCE);
			player.level().setBlockAndUpdate(player.blockPosition().offset(9, 0, 0),
					com.huntlike.registry.ModBlocks.UPGRADED_SOUL_MONOLITH.defaultBlockState());
			player.hurtServer(player.level(), player.level().damageSources().generic(), 6.0F);
		});
		context.waitTicks(80);
		assertServer(singleplayer, "Soul Monolith ★ reaches 9 blocks and gives Resistance I and Regeneration I", server -> {
			ServerPlayer player = player(server);
			HuntLike.LOGGER.info("[test] near Soul Monolith ★: resistance {} regeneration {} health {}",
					player.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE),
					player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION), player.getHealth());
			return player.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE)
					&& player.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
		});
		singleplayer.getServer().runOnServer(server -> player(server).level().setBlockAndUpdate(
				player(server).blockPosition().offset(9, 0, 0), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()));
		boolean inTab = context.computeOnClient(client -> {
			net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(client.player.connection.enabledFeatures(), true,
					client.level.registryAccess());
			var tabs = net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB;
			for (var tab : tabs) {
				if (tab.getDisplayItems().stream().anyMatch(stack -> stack.is(com.huntlike.registry.ModItems.WARM_AND_SLIMY_BEEF_JERKY))) {
					return true;
				}
			}
			return false;
		});
		HuntLike.LOGGER.info("[test] Warm and Slimy Beef Jerky in a creative tab: {}", inTab ? "OK" : "FAILED");
		if (!inTab) {
			throw new AssertionError("Beef Jerky missing from the creative tabs");
		}
		assertServer(singleplayer, "Cluster drop descriptions; Slime-Washed Holmium is the Tier V 1/100 drop", server -> {
			var thermal = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.THERMAL_SLIMEBALL);
			var holmium = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.SLIME_WASHED_HOLMIUM);
			var lore = holmium.get(net.minecraft.core.component.DataComponents.LORE);
			HuntLike.LOGGER.info("[test] {} / {}: {}", thermal.getHoverName().getString(), holmium.getHoverName().getString(),
					lore == null ? "no lore" : lore.lines().stream().map(net.minecraft.network.chat.Component::getString).toList());
			boolean dropped = com.huntlike.hunt.HuntDefs.clusterDrops(com.huntlike.hunt.HuntDefs.CLUSTER.tier(5)).stream()
					.anyMatch(drop -> drop.item() == com.huntlike.registry.ModItems.SLIME_WASHED_HOLMIUM && drop.chanceDen() == 100);
			return thermal.getHoverName().getString().equals("Thermal Slime Ball") && lore != null
					&& lore.lines().get(0).getString().startsWith("It felt more magnetic") && dropped;
		});
	}

	/** The Cluster Hunt's slime pet: taming, upgrades, riding, the menu, splitting, regrowing, Magma Cube, the core and reviving. */
	private static void slimePet(ClientGameTestContext context, TestSingleplayerContext singleplayer) {
		int[] wild = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			for (var e : player.level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
					player.getBoundingBox().inflate(40), e -> e != player)) {
				e.discard();
			}
			var slime = spawnTest(player, EntityTypes.SLIME, 3.0, 0.0, 1.0F);
			var lured = spawnTest(player, EntityTypes.SLIME, -3.0, 0.0, 1.0F);
			lured.addTag(com.huntlike.hunt.SpiritLure.LURED_TAG);
			return new int[] {slime.getId(), lured.getId()};
		});
		context.waitTicks(3);
		assertServer(singleplayer, "Slime pet: a lured slime refuses Thermal Slimeballs", server -> {
			ServerPlayer player = player(server);
			var lured = (net.minecraft.world.entity.monster.cubemob.Slime) player.level().getEntity(wild[1]);
			return !com.huntlike.pet.PetManager.feedWild(player, lured,
					new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.THERMAL_SLIMEBALL, 64))
					&& com.huntlike.pet.PetManager.latest(player) == null;
		});
		int feeds = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			var slime = (net.minecraft.world.entity.monster.cubemob.Slime) player.level().getEntity(wild[0]);
			var stack = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.THERMAL_SLIMEBALL, 64);
			for (int i = 1; i <= 20; i++) {
				if (com.huntlike.pet.PetManager.feedWild(player, slime, stack)) {
					return i;
				}
			}
			return -1;
		});
		context.waitTicks(5);
		assertServer(singleplayer, "Slime pet: tamed with Thermal Slimeballs (at most 20) into a 20-health medium slime named \"... Cluster\"",
				server -> {
					HuntLike.LOGGER.info("[test] pet tamed after {} slimeballs: {} name {}", feeds, petInfo(server),
							com.huntlike.pet.PetManager.latest(player(server)).name);
					var pieces = petPieces(server);
					return feeds > 0 && pieces.size() == 1 && pieces.get(0).getSize() == 2 && pieces.get(0).getMaxHealth() == 20.0F
							&& com.huntlike.pet.PetManager.latest(player(server)).name.endsWith(" Cluster")
							&& done(player(server), "cluster/pet_tame");
				});
		context.runOnClient(client -> {
			client.player.setYRot(-90.0F);
			client.player.setXRot(20.0F);
		});
		context.waitTicks(10);
		context.takeScreenshot("52-slime-pet");
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.IONIC_LIQUID), 1);
		assertServer(singleplayer, "Ionic Liquid: 40 health", server -> {
			var pieces = petPieces(server);
			return pieces.size() == 1 && pieces.get(0).getMaxHealth() == 40.0F && com.huntlike.pet.PetManager.latest(player(server)).ionic;
		});
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.THERMAL_SLIMEBALL), 25);
		context.waitTicks(5);
		assertServer(singleplayer, "Thermal Slimeballs after the Ionic Liquid grow it big (at most 25): 60 health", server -> {
			HuntLike.LOGGER.info("[test] grown: {} after {} feeds", petInfo(server), com.huntlike.pet.PetManager.latest(player(server)).growFeeds);
			var pieces = petPieces(server);
			return pieces.size() == 1 && pieces.get(0).getSize() == 4 && pieces.get(0).getMaxHealth() == 60.0F;
		});

		// Riding and the menu.
		petInteract(singleplayer, net.minecraft.world.item.ItemStack.EMPTY, 1);
		context.waitTicks(5);
		assertServer(singleplayer, "Riding: right-click a big pet with an empty hand", server ->
				player(server).getVehicle() instanceof com.huntlike.pet.PetCube);
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK));
		context.waitTicks(10);
		context.takeScreenshot("53-riding-pet");
		context.runOnClient(client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
		singleplayer.getServer().runOnServer(server -> com.huntlike.pet.PetManager.openMenu(petPieces(server).get(0), player(server)));
		context.waitForScreen(com.huntlike.client.pet.PetScreen.class);
		context.waitTicks(5);
		context.takeScreenshot("54-pet-menu-no-chest");
		singleplayer.getServer().runOnServer(server -> {
			var menu = (com.huntlike.pet.PetMenu) player(server).containerMenu;
			menu.getSlot(0).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST));
			menu.getSlot(1).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND, 7));
			menu.broadcastChanges();
		});
		context.waitTicks(5);
		context.takeScreenshot("55-pet-menu-page-1");
		singleplayer.getServer().runOnServer(server -> player(server).containerMenu.clickMenuButton(player(server), 0));
		context.waitTicks(5);
		context.takeScreenshot("56-pet-menu-page-2");
		assertServer(singleplayer, "Pet menu: chest slot, storage and the page button", server -> {
			var state = com.huntlike.pet.PetManager.latest(player(server));
			return state.chest && state.storage.get(0).is(net.minecraft.world.item.Items.DIAMOND) && state.page == 1;
		});
		context.setScreen(() -> null);
		singleplayer.getServer().runOnServer(server -> player(server).stopRiding());
		context.waitTicks(10);
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var pet = petPieces(server).get(0);
			pet.setNoAi(true);
			pet.teleportTo(player.getX() + 3.5, player.getY(), player.getZ());
			pet.setYRot(90.0F);
			pet.yBodyRot = 90.0F;
		});
		context.runOnClient(client -> {
			client.player.setYRot(-90.0F);
			client.player.setXRot(12.0F);
		});
		context.runOnClient(client -> client.gui.hud.getChat().clearMessages(false));
		context.waitTicks(15);
		context.takeScreenshot("56b-pet-with-chest");
		int chestPetId = singleplayer.getServer().computeOnServer(server -> petPieces(server).get(0).getId());
		String clientChest = context.computeOnClient(client -> {
			var entity = client.level.getEntity(chestPetId);
			if (!(entity instanceof com.huntlike.pet.PetCube cube)) {
				return "no entity " + entity;
			}
			var renderer = client.getEntityRenderDispatcher().getRenderer(cube);
			var state = client.getEntityRenderDispatcher().extractEntity(cube, 0.0F);
			return "client hasChest " + cube.hasChest() + " size " + cube.getSize() + " renderer " + renderer.getClass().getSimpleName()
					+ " state " + state.getClass().getSimpleName()
					+ (state instanceof com.huntlike.client.pet.PetRenderState pState ? " chest " + pState.chest : "");
		});
		HuntLike.LOGGER.info("[test] pet chest on the client: {}", clientChest);
		assertServer(singleplayer, "The big pet shows its chest", server -> petPieces(server).get(0).hasChest());
		singleplayer.getServer().runOnServer(server -> petPieces(server).get(0).setNoAi(false));

		// Killed: big -> two mediums (30) -> smalls (15); regrows after 45 quiet seconds.
		killPet(context, singleplayer);
		assertServer(singleplayer, "Killed big pet splits into two 30-health mediums", server -> {
			HuntLike.LOGGER.info("[test] after the first kill: {}", petInfo(server));
			var pieces = petPieces(server);
			return pieces.size() == 2 && pieces.stream().allMatch(p -> p.getSize() == 2 && p.getMaxHealth() == 30.0F);
		});
		killPet(context, singleplayer);
		assertServer(singleplayer, "Killed mediums split into 15-health smalls", server -> {
			HuntLike.LOGGER.info("[test] after the second kill: {}", petInfo(server));
			var pieces = petPieces(server);
			return pieces.size() == 4 && pieces.stream().allMatch(p -> p.getSize() == 1 && p.getMaxHealth() == 15.0F)
					&& com.huntlike.pet.PetManager.latest(player(server)).status == com.huntlike.pet.PetState.SPLIT;
		});
		singleplayer.getServer().runOnServer(server -> com.huntlike.pet.PetManager.latest(player(server)).lastDamage =
				player(server).level().getGameTime() - com.huntlike.pet.PetManager.REGROW_BIG - 1);
		context.waitTicks(30);
		assertServer(singleplayer, "45 quiet seconds: even with only small pieces left, it grows back into the big pet (chest kept)", server -> {
			HuntLike.LOGGER.info("[test] after regrowing: {}", petInfo(server));
			var pieces = petPieces(server);
			var state = com.huntlike.pet.PetManager.latest(player(server));
			return pieces.size() == 1 && pieces.get(0).getSize() == 4 && pieces.get(0).getHealth() == 60.0F
					&& state.status == com.huntlike.pet.PetState.WHOLE && state.storage.get(0).is(net.minecraft.world.item.Items.DIAMOND);
		});

		singleplayer.getServer().runOnServer(server -> {
			var pet = petPieces(server).get(0);
			pet.hurtServer(player(server).level(), player(server).level().damageSources().generic(), 10.0F);
		});
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SLIME_BALL), 1);
		assertServer(singleplayer, "A slime ball heals the pet by 4", server -> {
			float health = petPieces(server).get(0).getHealth();
			HuntLike.LOGGER.info("[test] pet health after 10 damage and a slime ball: {}", health);
			return Math.abs(health - 54.0F) < 0.01F;
		});
		// Corroded Palladium, then heat: a big Magma Cube.
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.CORRODED_PALLADIUM), 1);
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAGMA_CREAM), 2);
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.MAGMA_BLOCK), 1);
		petInteract(singleplayer, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.LAVA_BUCKET), 4);
		context.waitTicks(10);
		assertServer(singleplayer, "Heat 100: the pet becomes an 80-health, fire-immune Magma Cube", server -> {
			HuntLike.LOGGER.info("[test] magma: {}", petInfo(server));
			var pieces = petPieces(server);
			return pieces.size() == 1 && pieces.get(0) instanceof com.huntlike.pet.PetMagma && pieces.get(0).getMaxHealth() == 80.0F
					&& pieces.get(0).fireImmune() && done(player(server), "cluster/pet_magma");
		});
		context.takeScreenshot("57-magma-pet");

		// Every piece killed: the Slimy Core drops; reviving brings the Magma Cube back with its chest.
		for (int i = 0; i < 4; i++) {
			killPet(context, singleplayer);
		}
		assertServer(singleplayer, "Whole pet killed: a Slimy Core that can't burn drops", server -> {
			ServerPlayer player = player(server);
			var core = petCore(player);
			HuntLike.LOGGER.info("[test] core: {} {}", petInfo(server), core.isEmpty() ? "none" : core.getHoverName().getString());
			return com.huntlike.pet.PetManager.latest(player).status == com.huntlike.pet.PetState.CORE
					&& core.getHoverName().getString().endsWith(" Slimy Core")
					&& core.has(net.minecraft.core.component.DataComponents.DAMAGE_RESISTANT);
		});
		assertServer(singleplayer, "Crafting the revive recipe carries the core's pet over", server -> {
			ServerPlayer player = player(server);
			var core = petCore(player);
			player.inventoryMenu.getSlot(1).set(core.copy());
			var revived = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.REVIVED_SLIME_PET_CORE);
			revived.getItem().onCraftedBy(revived, player);
			player.inventoryMenu.getSlot(1).set(net.minecraft.world.item.ItemStack.EMPTY);
			HuntLike.LOGGER.info("[test] revived core: {} for pet {}", revived.getHoverName().getString(),
					com.huntlike.pet.PetManager.corePet(revived));
			return com.huntlike.pet.PetManager.latest(player).id.equals(com.huntlike.pet.PetManager.corePet(revived))
					&& revived.getHoverName().getString().startsWith("Revived ");
		});
		singleplayer.getServer().runOnServer(server -> com.huntlike.pet.PetManager.revive(player(server),
				player(server).position().add(3, 0, 0), com.huntlike.pet.PetManager.latest(player(server)).id));
		context.waitTicks(5);
		assertServer(singleplayer, "Revived: the big Magma Cube comes back with its upgrades and chest", server -> {
			HuntLike.LOGGER.info("[test] revived: {}", petInfo(server));
			var pieces = petPieces(server);
			var state = com.huntlike.pet.PetManager.latest(player(server));
			return pieces.size() == 1 && pieces.get(0) instanceof com.huntlike.pet.PetMagma && pieces.get(0).getMaxHealth() == 80.0F
					&& state.ionic && state.chest && state.storage.get(0).is(net.minecraft.world.item.Items.DIAMOND);
		});

		// Several pets: only one follows at a time; the others sit.
		boolean tamedSecond = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			var slime = (net.minecraft.world.entity.monster.cubemob.Slime) spawnTest(player, EntityTypes.SLIME, 0.0, 3.0, 1.0F);
			var stack = new net.minecraft.world.item.ItemStack(com.huntlike.registry.ModItems.THERMAL_SLIMEBALL, 64);
			for (int i = 0; i < 20; i++) {
				if (com.huntlike.pet.PetManager.feedWild(player, slime, stack)) {
					return true;
				}
			}
			return false;
		});
		context.waitTicks(3);
		assertServer(singleplayer, "A second pet can be tamed; it follows and the first one sits", server -> {
			var pets = com.huntlike.pet.PetManager.roster(player(server)).pets;
			HuntLike.LOGGER.info("[test] pets: {}", pets.stream().map(p -> p.name + (p.sitting ? " (sitting)" : " (following)")).toList());
			return tamedSecond && pets.size() == 2 && pets.get(0).sitting && !pets.get(1).sitting;
		});
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			var first = com.huntlike.pet.PetManager.roster(player).pets.get(0);
			var cube = com.huntlike.pet.PetManager.pieces(player.level(), player, first, player).get(0);
			player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			com.huntlike.pet.PetManager.interact(cube, player, net.minecraft.world.InteractionHand.MAIN_HAND);
			player.setShiftKeyDown(false);
		});
		assertServer(singleplayer, "Telling the first pet to follow makes the second one sit", server -> {
			var pets = com.huntlike.pet.PetManager.roster(player(server)).pets;
			return !pets.get(0).sitting && pets.get(1).sitting;
		});

		// A following pet boxed in 12 blocks away teleports to its owner after 5 seconds.
		BlockPos boxed = singleplayer.getServer().computeOnServer(server -> {
			ServerPlayer player = player(server);
			var first = com.huntlike.pet.PetManager.roster(player).pets.get(0);
			var cube = com.huntlike.pet.PetManager.pieces(player.level(), player, first, player).get(0);
			BlockPos center = player.blockPosition().offset(0, 0, 12);
			for (int x = -2; x <= 2; x++) {
				for (int z = -2; z <= 2; z++) {
					for (int y = -1; y <= 4; y++) {
						boolean wall = Math.abs(x) == 2 || Math.abs(z) == 2 || y == -1 || y == 4;
						player.level().setBlockAndUpdate(center.offset(x, y, z), wall
								? net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()
								: net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
					}
				}
			}
			cube.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
			return center;
		});
		context.waitTicks(140);
		assertServer(singleplayer, "A stuck pet (boxed in, 12 blocks away) teleports to its owner", server -> {
			ServerPlayer player = player(server);
			var first = com.huntlike.pet.PetManager.roster(player).pets.get(0);
			var cube = com.huntlike.pet.PetManager.pieces(player.level(), player, first, player).get(0);
			HuntLike.LOGGER.info("[test] stuck pet distance after 7 s: {}", cube.distanceTo(player));
			return cube.distanceTo(player) < 6.0;
		});
	}

	private static void wearNothing(ServerPlayer player) {
		for (var slot : new net.minecraft.world.entity.EquipmentSlot[] {net.minecraft.world.entity.EquipmentSlot.HEAD,
				net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS,
				net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.OFFHAND}) {
			player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
		}
	}

	private static void wear(TestSingleplayerContext singleplayer, com.huntlike.gear.ModGear.ArmorSet set) {
		singleplayer.getServer().runOnServer(server -> {
			ServerPlayer player = player(server);
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new net.minecraft.world.item.ItemStack(set.helmet()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new net.minecraft.world.item.ItemStack(set.chestplate()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new net.minecraft.world.item.ItemStack(set.leggings()));
			player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new net.minecraft.world.item.ItemStack(set.boots()));
		});
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
