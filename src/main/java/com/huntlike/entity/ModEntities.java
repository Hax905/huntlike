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

	private static final ResourceKey<EntityType<?>> CORROSIVE_STINGER_KEY = ResourceKey.create(Registries.ENTITY_TYPE,
			HuntLike.id("corrosive_stinger"));

	public static final EntityType<CorrosiveStinger> CORROSIVE_STINGER = Registry.register(BuiltInRegistries.ENTITY_TYPE,
			CORROSIVE_STINGER_KEY, EntityType.Builder.<CorrosiveStinger>of(CorrosiveStinger::new, MobCategory.MISC)
					.sized(0.25F, 0.25F)
					.clientTrackingRange(4)
					.updateInterval(10)
					.noSave()
					.build(CORROSIVE_STINGER_KEY));

	private static final ResourceKey<EntityType<?>> CLUSTER_BALL_KEY = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id("cluster_ball"));

	public static final EntityType<ClusterBall> CLUSTER_BALL = Registry.register(BuiltInRegistries.ENTITY_TYPE, CLUSTER_BALL_KEY,
			EntityType.Builder.<ClusterBall>of(ClusterBall::new, MobCategory.MISC)
					.sized(0.4F, 0.4F)
					.clientTrackingRange(6)
					.updateInterval(10)
					.noSave()
					.build(CLUSTER_BALL_KEY));

	// Cluster Hunt bosses: slimes and magma cubes with their own names; their size comes from the fight.
	public static final EntityType<com.huntlike.entity.cluster.Cubert> CUBERT = cluster("cubert",
			com.huntlike.entity.cluster.Cubert::new, false);
	public static final EntityType<com.huntlike.entity.cluster.Blucky> BLUCKY = cluster("blucky",
			com.huntlike.entity.cluster.Blucky::new, false);
	public static final EntityType<com.huntlike.entity.cluster.HotHead> HOT_HEAD = cluster("hot_head",
			com.huntlike.entity.cluster.HotHead::new, true);
	public static final EntityType<com.huntlike.entity.cluster.VarianceSlime> VARIANCE_SLIME = cluster("variance_slime",
			com.huntlike.entity.cluster.VarianceSlime::new, false);
	public static final EntityType<com.huntlike.entity.cluster.VarianceMagma> VARIANCE_MAGMA = cluster("variance_magma",
			com.huntlike.entity.cluster.VarianceMagma::new, true);
	public static final EntityType<com.huntlike.entity.cluster.TheVile> THE_VILE = cluster("the_vile",
			com.huntlike.entity.cluster.TheVile::new, true);

	private static <T extends net.minecraft.world.entity.Mob> EntityType<T> cluster(String name, EntityType.EntityFactory<T> factory,
			boolean fireImmune) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, HuntLike.id(name));
		EntityType.Builder<T> builder = EntityType.Builder.of(factory, MobCategory.MONSTER).sized(0.52F, 0.52F).eyeHeight(0.325F)
				.clientTrackingRange(10);
		if (fireImmune) {
			builder = builder.fireImmune();
		}
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

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
		var slime = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityTypes.SLIME);
		var magma = net.minecraft.world.entity.ai.attributes.DefaultAttributes.getSupplier(net.minecraft.world.entity.EntityTypes.MAGMA_CUBE);
		FabricDefaultAttributeRegistry.register(CUBERT, slime);
		FabricDefaultAttributeRegistry.register(BLUCKY, slime);
		FabricDefaultAttributeRegistry.register(HOT_HEAD, magma);
		FabricDefaultAttributeRegistry.register(VARIANCE_SLIME, slime);
		FabricDefaultAttributeRegistry.register(VARIANCE_MAGMA, magma);
		FabricDefaultAttributeRegistry.register(THE_VILE, slime);
	}
}
