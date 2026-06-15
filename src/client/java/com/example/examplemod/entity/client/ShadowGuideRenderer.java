package com.example.examplemod.entity.client;

import com.example.examplemod.entity.ShadowGuideEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Identifier;

public class ShadowGuideRenderer extends MobEntityRenderer<ShadowGuideEntity, WaterMonsterRenderState, WaterMonsterModel> {
    private static final Identifier TEXTURE = Identifier.of("examplemod", "textures/entity/water_monster_critical_him.png");

    public ShadowGuideRenderer(EntityRendererFactory.Context context) {
        super(context, new WaterMonsterModel(context.getPart(ExampleModEntityRenderer.WATER_MONSTER_LAYER)), 0.5F);
        this.addFeature(new WaterMonsterHeldItemFeatureRenderer(this));
    }

    @Override
    public WaterMonsterRenderState createRenderState() {
        return new WaterMonsterRenderState();
    }

    @Override
    public void updateRenderState(ShadowGuideEntity entity, WaterMonsterRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        ArmedEntityRenderState.updateRenderState(entity, state, this.itemModelResolver, tickDelta);
        state.humanoidForm = true;
        state.attacking = false;
        state.randomPlayerSkin = false;
        state.phaseThreeSkin = false;
        state.criticalSkinFlicker = false;
        state.skinVariant = 0;
    }

    @Override
    public Identifier getTexture(WaterMonsterRenderState state) {
        return TEXTURE;
    }
}
