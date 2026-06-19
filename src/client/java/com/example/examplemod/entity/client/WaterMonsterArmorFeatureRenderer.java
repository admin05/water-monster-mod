package com.example.examplemod.entity.client;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.equipment.EquipmentModel;
import net.minecraft.client.render.entity.equipment.EquipmentRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.client.util.math.MatrixStack;

public class WaterMonsterArmorFeatureRenderer extends FeatureRenderer<WaterMonsterRenderState, WaterMonsterModel> {
    private final EquipmentRenderer equipmentRenderer;
    private boolean armorRenderingDisabled;

    public WaterMonsterArmorFeatureRenderer(
            FeatureRendererContext<WaterMonsterRenderState, WaterMonsterModel> context,
            EquipmentRenderer equipmentRenderer
    ) {
        super(context);
        this.equipmentRenderer = equipmentRenderer;
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, WaterMonsterRenderState state, float limbAngle, float limbDistance) {
        if (!state.humanoidForm || armorRenderingDisabled) return;

        try {
            renderArmor(matrices, commandQueue, light, state.equippedChestStack, EquipmentSlot.CHEST, state);
            renderArmor(matrices, commandQueue, light, state.equippedLegsStack, EquipmentSlot.LEGS, state);
            renderArmor(matrices, commandQueue, light, state.equippedFeetStack, EquipmentSlot.FEET, state);
            renderArmor(matrices, commandQueue, light, state.equippedHeadStack, EquipmentSlot.HEAD, state);
        } catch (RuntimeException ignored) {
            armorRenderingDisabled = true;
        }
    }

    private void renderArmor(MatrixStack matrices, OrderedRenderCommandQueue commandQueue, int light, ItemStack stack, EquipmentSlot slot, WaterMonsterRenderState state) {
        EquippableComponent equippable = stack.get(DataComponentTypes.EQUIPPABLE);
        if (equippable == null || equippable.slot() != slot || equippable.assetId().isEmpty()) {
            return;
        }

        EquipmentModel.LayerType layerType = slot == EquipmentSlot.LEGS
                ? EquipmentModel.LayerType.HUMANOID_LEGGINGS
                : EquipmentModel.LayerType.HUMANOID;
        equipmentRenderer.render(
                layerType,
                equippable.assetId().orElseThrow(),
                this.getContextModel(),
                state,
                stack,
                matrices,
                commandQueue,
                light,
                state.outlineColor
        );
    }
}
