package com.huntlike.client.item;

import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import org.joml.Vector3fc;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;

import com.huntlike.HuntLike;

/** A shield drawn with the vanilla shield model and its own 64x64 texture (Towernaar's shields). */
public class TexturedShieldRenderer implements NoDataSpecialModelRenderer {
	private final ShieldModel model;
	private final Identifier texture;

	public TexturedShieldRenderer(ShieldModel model, Identifier texture) {
		this.model = model;
		this.texture = texture;
	}

	@Override
	public void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean foil, int outline) {
		collector.submitModel(this.model, Unit.INSTANCE, pose,
				foil ? RenderTypes.entitySolidGlint(this.texture) : RenderTypes.entitySolid(this.texture), light, overlay, outline);
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		PoseStack pose = new PoseStack();
		this.model.root().getExtentsForGui(pose, output);
	}

	public record Unbaked(Identifier texture) implements NoDataSpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
				instance -> instance.group(Identifier.CODEC.fieldOf("texture").forGetter(Unbaked::texture)).apply(instance, Unbaked::new));

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
			return new TexturedShieldRenderer(new ShieldModel(context.entityModelSet().bakeLayer(ModelLayers.SHIELD)), this.texture);
		}
	}

	public static void initialize() {
		SpecialModelRenderers.ID_MAPPER.put(HuntLike.id("textured_shield"), Unbaked.MAP_CODEC);
	}
}
