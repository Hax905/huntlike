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
			singleplayer.getServer().runCommand("huntlike tokens add 64");
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
			check(singleplayer, "boss spawned", data -> data.quest != null && data.quest.phase == HuntData.PHASE_BOSS);
			context.takeScreenshot("08-boss-spawned");

			singleplayer.getServer().runOnServer(server -> QuestManager.openMenu(player(server)));
			context.waitForScreen(HuntMenuScreen.class);
			context.waitTicks(5);
			context.takeScreenshot("09-active-quest-boss");
			context.setScreen(() -> null);

			// Let the boss fight for a few seconds, then kill it as the player.
			context.waitTicks(60);
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

			singleplayer.getServer().runCommand("huntlike xp add 200");
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
			context.takeScreenshot("13-boss-card-defeated");
			HuntLike.LOGGER.info("[test] HuntLike client game test passed");
		}
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
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
