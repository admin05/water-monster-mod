package com.example.examplemod.entity.client;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.util.math.MatrixStack;

public class WaterMonsterArmorFeatureRenderer extends ArmorFeatureRenderer<WaterMonsterRenderState, WaterMonsterModel, BipedEntityModel<WaterMonsterRenderState>> {
    public WaterMonsterArmorFeatureRenderer(
            FeatureRendererContext<WaterMonsterRenderState, WaterMonsterModel> context,
            EquipmentModelData<BipedEntityModel<WaterMonsterRenderState>> equipmentModels,
            EquipmentRenderer equipmentRenderer
    ) {
        super(context, equipmentModels, equipmentRenderer);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, WaterMonsterRenderState state, float limbAngle, float limbDistance) {
        if (!state.humanoidForm) return;
        super.render(matrices, commandQueue, light, state, limbAngle, limbDistance);
    }
}
