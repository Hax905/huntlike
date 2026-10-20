package com.huntlike.client;

import net.minecraft.client.renderer.entity.ThrownItemRenderer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

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
				(payload, context) -> context.client().setScreen(new HuntMenuScreen(payload.tab())));
		EntityRendererRegistry.register(ModEntities.BOSS_EGG, ThrownItemRenderer::new);
	}
}
