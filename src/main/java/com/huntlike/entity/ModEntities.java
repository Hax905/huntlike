package com.huntlike.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.animal.wolf.Wolf;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import com.huntlike.HuntLike;
import com.huntlike.entity.animal.*;

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

	// Hunt bosses and Herd Call animals: their own entity types, so they carry their own names (also in death
	// messages) and can get their own models later.
	public static final EntityType<Sylviornis> SYLVIORNIS = animal("sylviornis", Sylviornis::new, 0.4F, 0.7F);
	public static final EntityType<Hylochoerus> HYLOCHOERUS = animal("hylochoerus", Hylochoerus::new, 0.9F, 0.9F);
	public static final EntityType<OvisAmmon> OVIS_AMMON = animal("ovis_ammon", OvisAmmon::new, 0.9F, 1.3F);
	public static final EntityType<Aurochs> AUROCHS = animal("aurochs", Aurochs::new, 0.9F, 1.4F);
	public static final EntityType<EpicyonHaydeni> EPICYON_HAYDENI = animal("epicyon_haydeni", EpicyonHaydeni::new, 0.6F, 0.85F);
	public static final EntityType<BosTaurus> BOS_TAURUS = animal("bos_taurus", BosTaurus::new, 0.9F, 1.4F);
	public static final EntityType<CanisLupus> CANIS_LUPUS = animal("canis_lupus", CanisLupus::new, 0.6F, 0.85F);

	private static <T extends net.minecraft.world.entity.Mob> EntityType<T> animal(String name, EntityType.EntityFactory<T> factory,
			float width, float height) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key,
				EntityType.Builder.of(factory, MobCategory.CREATURE).sized(width, height).clientTrackingRange(10).build(key));
	}

	public static void initialize() {
		FabricDefaultAttributeRegistry.register(SYLVIORNIS, Chicken.createAttributes());
		FabricDefaultAttributeRegistry.register(HYLOCHOERUS, Pig.createAttributes());
		FabricDefaultAttributeRegistry.register(OVIS_AMMON, Sheep.createAttributes());
		FabricDefaultAttributeRegistry.register(AUROCHS, Cow.createAttributes());
		FabricDefaultAttributeRegistry.register(EPICYON_HAYDENI, Wolf.createAttributes());
		FabricDefaultAttributeRegistry.register(BOS_TAURUS, Cow.createAttributes());
		FabricDefaultAttributeRegistry.register(CANIS_LUPUS, Wolf.createAttributes());
	}
}
