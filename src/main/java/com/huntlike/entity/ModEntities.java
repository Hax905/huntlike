package com.huntlike.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import com.huntlike.HuntLike;

/** HuntLike entity types. */
public final class ModEntities {
	private ModEntities() {
	}

	private static final ResourceKey<EntityType<?>> BOSS_EGG_KEY = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id("boss_egg"));

	public static final EntityType<BossEgg> BOSS_EGG = Registry.register(BuiltInRegistries.ENTITY_TYPE, BOSS_EGG_KEY,
			EntityType.Builder.<BossEgg>of(BossEgg::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.noSave()
					.build(BOSS_EGG_KEY));

	public static void initialize() {
	}
}
