package com.huntlike;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

import net.fabricmc.api.ModInitializer;

import com.huntlike.command.HuntCommand;
import com.huntlike.entity.ModEntities;
import com.huntlike.gear.ModGear;
import com.huntlike.hunt.QuestManager;
import com.huntlike.loot.ModLoot;
import com.huntlike.network.HuntPayloads;
import com.huntlike.registry.ModBlocks;
import com.huntlike.registry.ModCreativeTab;
import com.huntlike.registry.ModItems;

/** Main entrypoint for HuntLike. */
public class HuntLike implements ModInitializer {
	public static final String MOD_ID = "huntlike";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.initialize();
		ModGear.initialize();
		ModBlocks.initialize();
		ModCreativeTab.initialize();
		ModLoot.initialize();
		ModEntities.initialize();
		HuntPayloads.initialize();
		QuestManager.initialize();
		HuntCommand.initialize();
		LOGGER.info("HuntLike loaded. The Alpha Altar is listening...");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
