package com.huntlike.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.huntlike.client.entity.*;
import com.huntlike.client.screen.HuntMenuScreen;
import com.huntlike.entity.ModEntities;
import com.huntlike.network.HuntPayloads;

/** Client entrypoint: the Hunt menu, synced data and renderers. */
public class HuntLikeClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(HuntPayloads.Sync.TYPE,
				(payload, context) -> ClientHuntState.update(payload.tag()));
		ClientPlayNetworking.registerGlobalReceiver(HuntPayloads.OpenMenu.TYPE,
				(payload, context) -> context.client().gui.setScreen(new HuntMenuScreen(payload.tab())));
		ClientPlayNetworking.registerGlobalReceiver(HuntPayloads.Highlight.TYPE, (payload, context) -> {
			ClientHuntState.highlightId = payload.entityId();
			ClientHuntState.highlightUntil = context.client().level.getGameTime() + payload.ticks();
		});
		ClientPlayNetworking.registerGlobalReceiver(HuntPayloads.GearToast.TYPE, (payload, context) -> {
			var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(payload.itemId()));
			context.client().gui.toastManager().addToast(new GearToast(new net.minecraft.world.item.ItemStack(item), payload.name()));
		});
		EntityRendererRegistry.register(ModEntities.BOSS_EGG, ThrownItemRenderer::new);
		EntityRendererRegistry.register(ModEntities.CORROSIVE_STINGER, ThrownItemRenderer::new);
		EntityRendererRegistry.register(ModEntities.SYLVIORNIS, SylviornisRenderer::new);
		EntityRendererRegistry.register(ModEntities.HYLOCHOERUS, HylochoerusRenderer::new);
		EntityRendererRegistry.register(ModEntities.OVIS_AMMON, OvisAmmonRenderer::new);
		EntityRendererRegistry.register(ModEntities.AUROCHS, AurochsRenderer::new);
		EntityRendererRegistry.register(ModEntities.EPICYON_HAYDENI, EpicyonHaydeniRenderer::new);
		EntityRendererRegistry.register(ModEntities.BOS_TAURUS, BosTaurusRenderer::new);
		EntityRendererRegistry.register(ModEntities.CANIS_LUPUS, CanisLupusRenderer::new);
	}
}
