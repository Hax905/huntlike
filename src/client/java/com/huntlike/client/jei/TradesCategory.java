package com.huntlike.client.jei;

import java.util.List;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.huntlike.HuntLike;
import com.huntlike.registry.ModBlocks;

/** Gear tab, Koro, Towernaar and PRD trades: what goes in and what comes out (no unlock hints: they'd spoil the game). */
public final class TradesCategory extends AbstractRecipeCategory<TradesCategory.Entry> {
	public record Entry(String title, int color, List<ItemStack> cost, List<ItemStack> result, String note) {
	}

	public static final IRecipeType<Entry> TYPE = IRecipeType.create(HuntLike.MOD_ID, "hunt_trades", Entry.class);
	static final int WIDTH = 162;
	static final int COST_COLUMNS = 5;

	public TradesCategory(IGuiHelper gui) {
		super(TYPE, Component.literal("Hunt Trades"), gui.createDrawableItemLike(ModBlocks.ALPHA_ALTAR), WIDTH, 70);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
		for (int i = 0; i < entry.cost().size(); i++) {
			builder.addSlot(RecipeIngredientRole.INPUT, (i % COST_COLUMNS) * 18 + 1, 13 + (i / COST_COLUMNS) * 18)
					.setStandardSlotBackground().add(entry.cost().get(i));
		}
		for (int i = 0; i < entry.result().size(); i++) {
			builder.addSlot(RecipeIngredientRole.OUTPUT, 125 + (i % 2) * 18, 13 + (i / 2) * 18)
					.setStandardSlotBackground().add(entry.result().get(i));
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Entry entry, IFocusGroup focuses) {
		builder.addText(Component.literal(entry.title()), WIDTH, 10).setPosition(0, 0).setColor(0xFF000000 | entry.color())
				.setShadow(true);
		builder.addText(Component.literal("->"), 16, 10).setPosition(COST_COLUMNS * 18 + 8, 27).setColor(0xFF404040);
	}
}
