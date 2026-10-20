package com.huntlike.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import com.huntlike.hunt.HuntData;
import com.huntlike.hunt.QuestManager;

/** Operator test command: /huntlike xp|tokens add <amount>, /huntlike reset. */
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
						.then(Commands.literal("reset").executes(HuntCommand::reset))));
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
