package com.example.examplemod;

import com.example.examplemod.entity.client.ExampleModEntityRenderer;
import com.example.examplemod.entity.client.NoBlockDamageTntRenderer;
import com.example.examplemod.entity.client.WaterMonsterRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class ExampleModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ExampleMod.LOGGER.info("ExampleMod client initialized!");
        ExampleModEntityRenderer.register();
        EntityRendererRegistry.register(ExampleMod.WATER_MONSTER, WaterMonsterRenderer::new);
        EntityRendererRegistry.register(ExampleMod.NO_BLOCK_DAMAGE_TNT, NoBlockDamageTntRenderer::new);
    }
}
