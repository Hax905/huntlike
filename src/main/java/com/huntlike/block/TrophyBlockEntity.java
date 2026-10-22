package com.huntlike.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.huntlike.registry.ModBlocks;

/** Remembers the trophy's lore ("A sign of [player]'s mastery...") so the item keeps it when broken. */
public class TrophyBlockEntity extends BlockEntity {
	private ItemLore lore = ItemLore.EMPTY;

	public TrophyBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlocks.TROPHY_ENTITY, pos, state);
	}

	public ItemLore lore() {
		return lore;
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		lore = input.read("lore", ItemLore.CODEC).orElse(ItemLore.EMPTY);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (!lore.lines().isEmpty()) {
			output.store("lore", ItemLore.CODEC, lore);
		}
	}

	@Override
	protected void applyImplicitComponents(DataComponentGetter components) {
		super.applyImplicitComponents(components);
		lore = components.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
	}

	@Override
	protected void collectImplicitComponents(DataComponentMap.Builder components) {
		super.collectImplicitComponents(components);
		if (!lore.lines().isEmpty()) {
			components.set(DataComponents.LORE, lore);
		}
	}

	@Override
	public void removeComponentsFromTag(ValueOutput output) {
		super.removeComponentsFromTag(output);
		output.discard("lore");
	}
}
