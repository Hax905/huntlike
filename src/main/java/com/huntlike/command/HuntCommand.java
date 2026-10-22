package com.huntlike.command;

import java.util.Collection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.HuntDefs;
import com.huntlike.hunt.HuntDefs.Drop;
import com.huntlike.hunt.HuntDefs.Hunt;
import com.huntlike.hunt.QuestManager;

/**
 * Operator commands:
 * /huntlike xp|tokens add &lt;amount&gt;, /huntlike reset,
 * /huntlike quest start &lt;players&gt; &lt;hunt&gt; &lt;tier&gt;, /huntlike quest cancel &lt;players&gt;,
 * /huntlike stats [player], /huntlike boss &lt;hunt&gt; &lt;tier&gt; [players].
 */
public final class HuntCommand {
	private HuntCommand() {
	}

	public static void initialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
				Commands.literal("huntlike")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("xp").then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer(0))
										.executes(ctx -> add(ctx, true)))))
						.then(Commands.literal("tokens").then(Commands.literal("add")
								.then(Commands.argument("amount", IntegerArgumentType.integer(0))
										.executes(ctx -> add(ctx, false)))))
						.then(Commands.literal("reset").executes(HuntCommand::reset))
						.then(Commands.literal("quest")
								.then(Commands.literal("start")
										.then(Commands.argument("players", EntityArgument.players())
												.then(huntArgument()
														.then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
																.executes(ctx -> start(ctx, false))))))
								.then(Commands.literal("cancel")
										.then(Commands.argument("players", EntityArgument.players())
												.executes(HuntCommand::cancel))))
						.then(Commands.literal("stats")
								.executes(ctx -> stats(ctx, ctx.getSource().getPlayerOrException()))
								.then(Commands.argument("player", EntityArgument.player())
										.executes(ctx -> stats(ctx, EntityArgument.getPlayer(ctx, "player")))))
						.then(Commands.literal("boss")
								.then(huntArgument()
										.then(Commands.argument("tier", IntegerArgumentType.integer(1, 5))
												.executes(ctx -> bossSelf(ctx))
												.then(Commands.argument("players", EntityArgument.players())
														.executes(ctx -> start(ctx, true))))))));
	}

	private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> huntArgument() {
		return Commands.argument("hunt", StringArgumentType.word())
				.suggests((ctx, builder) -> SharedSuggestionProvider.suggest(HuntDefs.HUNTS.stream().map(Hunt::id), builder));
	}

	private static Hunt hunt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		String id = StringArgumentType.getString(ctx, "hunt");
		for (Hunt hunt : HuntDefs.HUNTS) {
			if (hunt.id().equals(id)) {
				return hunt;
			}
		}
		throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Unknown Hunt: " + id)).create();
	}

	private static int start(CommandContext<CommandSourceStack> ctx, boolean boss) throws CommandSyntaxException {
		return start(ctx, EntityArgument.getPlayers(ctx, "players"), boss);
	}

	private static int bossSelf(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		return start(ctx, java.util.List.of(ctx.getSource().getPlayerOrException()), true);
	}

	private static int start(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, boolean boss)
			throws CommandSyntaxException {
		Hunt hunt = hunt(ctx);
		int tier = IntegerArgumentType.getInteger(ctx, "tier");
		int done = 0;
		for (ServerPlayer player : players) {
			boolean ok = boss ? QuestManager.forceBoss(player, hunt, tier) : QuestManager.forceStart(player, hunt, tier);
			if (ok) {
				done++;
			} else {
				ctx.getSource().sendFailure(Component.literal(player.getScoreboardName()
						+ (QuestManager.data(player).quest != null ? " already has an active quest." : ": no room to summon the boss here.")));
			}
		}
		int count = done;
		String what = (boss ? "Summoned " : "Started ") + hunt.name() + " [Tier " + HuntDefs.ROMAN[tier] + "]"
				+ (boss ? (count == 1 ? " boss" : " bosses") : (count == 1 ? " quest" : " quests"));
		if (count > 0) {
			ctx.getSource().sendSuccess(() -> Component.literal(what + " for " + count + " player" + (count == 1 ? "." : "s.")), true);
		}
		return count;
	}

	private static int cancel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		int done = 0;
		for (ServerPlayer player : EntityArgument.getPlayers(ctx, "players")) {
			HuntData data = QuestManager.data(player);
			if (data.quest != null) {
				QuestManager.fail(player, data);
				done++;
			}
		}
		int count = done;
		if (count == 0) {
			ctx.getSource().sendFailure(Component.literal("No active quests to cancel."));
		} else {
			ctx.getSource().sendSuccess(() -> Component.literal("Cancelled " + count + " quest" + (count == 1 ? "." : "s.")), true);
		}
		return count;
	}

	private static int stats(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
		HuntData data = QuestManager.data(player);
		CommandSourceStack source = ctx.getSource();
		source.sendSuccess(() -> Component.literal("HuntLike stats - " + player.getScoreboardName())
				.withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
		for (Hunt hunt : HuntDefs.HUNTS) {
			HuntData.Progress progress = data.progress(hunt.id());
			int rank = progress.rank();
			source.sendSuccess(() -> line(hunt.name(), hunt.rankName(rank) + " (Rank " + rank + ") - " + progress.xp + " XP, "
					+ progress.tokens + " tokens", hunt.color()), false);
			StringBuilder kills = new StringBuilder();
			for (int i = 0; i < hunt.tiers().size(); i++) {
				kills.append(i > 0 ? "  " : "").append(HuntDefs.ROMAN[i + 1]).append(": ").append(progress.bossKills[i]);
			}
			source.sendSuccess(() -> line("Boss kills", kills.toString(), 0xAAAAAA), false);
			java.util.LinkedHashMap<String, String> names = new java.util.LinkedHashMap<>();
			for (Drop drop : HuntDefs.drops(hunt.tier(hunt.tiers().size()))) {
				names.put(drop.id(), new net.minecraft.world.item.ItemStack(drop.item()).getHoverName().getString());
			}
			names.put("soul_stone", "Soul Stone");
			StringBuilder drops = new StringBuilder();
			names.forEach((id, name) -> {
				if (progress.dropsSeen.contains(id)) {
					drops.append(drops.length() > 0 ? ", " : "").append(name).append(" x").append(progress.dropCounts.getOrDefault(id, 0));
				}
			});
			String dropText = drops.length() > 0 ? drops.toString() : "none yet";
			source.sendSuccess(() -> line("Drops", dropText, 0xAAAAAA), false);
		}
		source.sendSuccess(() -> line("Lured mobs slain", String.valueOf(data.luredKills), 0xAAAAAA), false);
		source.sendSuccess(() -> line("Mystery Concoction", data.concoction ? "eaten" : "not eaten", 0xAAAAAA), false);
		source.sendSuccess(() -> line("Koro chapters", data.koroDone + " / 4", 0xAAAAAA), false);
		HuntData.Quest quest = data.quest;
		String questText = quest == null ? "none"
				: HuntDefs.hunt(quest.hunt).name() + " [Tier " + HuntDefs.ROMAN[quest.tier] + "] - "
						+ switch (quest.phase) {
							case HuntData.PHASE_KILLING -> quest.kills + "/" + quest.quota + " kills";
							case HuntData.PHASE_SUMMONING -> "summoning";
							default -> "boss alive";
						};
		source.sendSuccess(() -> line("Active quest", questText, 0xAAAAAA), false);
		return 1;
	}

	private static MutableComponent line(String label, String value, int color) {
		return Component.literal(label + ": ").withStyle(style -> style.withColor(color))
				.append(Component.literal(value).withStyle(ChatFormatting.WHITE));
	}

	private static int add(CommandContext<CommandSourceStack> ctx, boolean xp) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		int amount = IntegerArgumentType.getInteger(ctx, "amount");
		HuntData data = QuestManager.data(player);
		HuntData.Progress progress = data.progress("animal");
		if (xp) {
			progress.xp += amount;
		} else {
			progress.tokens += amount;
		}
		player.setAttached(QuestManager.DATA, data);
		QuestManager.sync(player);
		ctx.getSource().sendSuccess(() -> Component.literal("Added " + amount + (xp ? " Animal XP." : " Animal Ritual Tokens.")), false);
		return 1;
	}

	private static int reset(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		HuntData data = QuestManager.data(player);
		if (data.quest != null) {
			QuestManager.fail(player, data);
		}
		player.setAttached(QuestManager.DATA, new HuntData());
		QuestManager.sync(player);
		ctx.getSource().sendSuccess(() -> Component.literal("HuntLike progress reset."), false);
		return 1;
	}
}
