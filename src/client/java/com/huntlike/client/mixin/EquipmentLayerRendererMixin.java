package com.huntlike.client.mixin;

import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;

/**
 * The Cluster Hunt armor (Icy, Gel, Thermodynamic) is drawn see-through, like slime: its worn textures are half
 * transparent, and here it uses the translucent entity render type instead of the armor's solid cutout one.
 */
@Mixin(EquipmentLayerRenderer.class)
public abstract class EquipmentLayerRendererMixin {
	private static final Set<String> SEE_THROUGH = Set.of("icy", "gel", "thermodynamic");
	private static boolean huntlike$seeThrough;

	@Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
			at = @At("HEAD"))
	private void huntlike$start(EquipmentClientInfo.LayerType layer, ResourceKey<EquipmentAsset> asset, Model<?> model, Object state,
			ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture, int outline, int order, CallbackInfo ci) {
		Identifier id = asset.identifier();
		huntlike$seeThrough = "huntlike".equals(id.getNamespace()) && SEE_THROUGH.contains(id.getPath());
	}

	@Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V",
			at = @At("RETURN"))
	private void huntlike$end(EquipmentClientInfo.LayerType layer, ResourceKey<EquipmentAsset> asset, Model<?> model, Object state,
			ItemStack stack, PoseStack pose, SubmitNodeCollector collector, int light, Identifier texture, int outline, int order, CallbackInfo ci) {
		huntlike$seeThrough = false;
	}

	@WrapOperation(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;armorCutoutNoCull(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
	private RenderType huntlike$translucent(Identifier texture, Operation<RenderType> original) {
		return huntlike$seeThrough ? RenderTypes.entityTranslucent(texture) : original.call(texture);
	}
}
