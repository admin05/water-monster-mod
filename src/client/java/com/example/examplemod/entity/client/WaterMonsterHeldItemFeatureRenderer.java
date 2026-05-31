package com.example.examplemod.entity.client;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.util.math.MatrixStack;

public class WaterMonsterHeldItemFeatureRenderer extends HeldItemFeatureRenderer<WaterMonsterRenderState, WaterMonsterModel> {
    public WaterMonsterHeldItemFeatureRenderer(FeatureRendererContext<WaterMonsterRenderState, WaterMonsterModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, WaterMonsterRenderState state, float limbAngle, float limbDistance) {
        if (!state.humanoidForm) return;
        super.render(matrices, commandQueue, light, state, limbAngle, limbDistance);
    }
}
