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

/** Boss and Interlude drops: one page per tier or stage side, each drop with its odds in the tooltip. */
public final class DropsCategory extends AbstractRecipeCategory<DropsCategory.Entry> {
	public record Slot(ItemStack stack, List<Component> lines) {
	}

	public record Entry(String title, int color, List<Slot> slots, String note) {
	}

	public static final IRecipeType<Entry> TYPE = IRecipeType.create(HuntLike.MOD_ID, "boss_drops", Entry.class);
	static final int WIDTH = 162;
	static final int PER_ROW = 9;

	public DropsCategory(IGuiHelper gui) {
		super(TYPE, Component.literal("Hunt Drops"), gui.createDrawableItemLike(ModBlocks.ALPHA_ALTAR), WIDTH, 56);
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
		for (int i = 0; i < entry.slots().size(); i++) {
			Slot slot = entry.slots().get(i);
			builder.addSlot(RecipeIngredientRole.OUTPUT, (i % PER_ROW) * 18 + 1, 13 + (i / PER_ROW) * 18)
					.setStandardSlotBackground()
					.add(slot.stack())
					.addRichTooltipCallback((view, tooltip) -> slot.lines().forEach(tooltip::add));
		}
	}

	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder builder, Entry entry, IFocusGroup focuses) {
		builder.addText(Component.literal(entry.title()), WIDTH, 10).setPosition(0, 0).setColor(0xFF000000 | entry.color())
				.setShadow(true);
		builder.addText(Component.literal(entry.note()), WIDTH, 20).setPosition(0, 35).setColor(0xFF555555);
	}
}
