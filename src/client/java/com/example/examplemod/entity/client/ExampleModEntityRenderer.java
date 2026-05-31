package com.example.examplemod.entity.client;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ExampleModEntityRenderer {
    public static final EntityModelLayer WATER_MONSTER_LAYER = new EntityModelLayer(
            Identifier.of("examplemod", "water_monster"), "main");

    public static void register() {
        EntityModelLayerRegistry.registerModelLayer(WATER_MONSTER_LAYER, WaterMonsterModel::getTexturedModelData);
    }
}
