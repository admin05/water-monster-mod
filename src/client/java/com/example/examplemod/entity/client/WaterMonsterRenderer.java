package com.example.examplemod.entity.client;

import com.example.examplemod.entity.WaterMonsterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Identifier;

public class WaterMonsterRenderer extends MobEntityRenderer<WaterMonsterEntity, WaterMonsterRenderState, WaterMonsterModel> {
    private static final Identifier TEXTURE = Identifier.of("examplemod", "textures/entity/water_creature.png");
    private static final Identifier[] RANDOM_PLAYER_SKINS = {
            Identifier.of("examplemod", "textures/entity/water_monster_steveawa.png"),
            Identifier.of("examplemod", "textures/entity/water_monster_steve.png"),
            Identifier.of("examplemod", "textures/entity/water_monster_steve_crown.png")
    };

    public WaterMonsterRenderer(EntityRendererFactory.Context context) {
        super(context, new WaterMonsterModel(context.getPart(ExampleModEntityRenderer.WATER_MONSTER_LAYER)), 0.5F);
        this.addFeature(new WaterMonsterHeldItemFeatureRenderer(this));
    }

    @Override
    public WaterMonsterRenderState createRenderState() {
        return new WaterMonsterRenderState();
    }

    @Override
    public void updateRenderState(WaterMonsterEntity entity, WaterMonsterRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        ArmedEntityRenderState.updateRenderState(entity, state, this.itemModelResolver, tickDelta);
        state.randomPlayerSkin = entity.shouldUseRandomPlayerSkin();
        state.humanoidForm = state.randomPlayerSkin || entity.isHumanoidForm();
        state.attacking = entity.isAttacking();
        state.skinVariant = entity.getSkinVariant();
    }

    @Override
    public Identifier getTexture(WaterMonsterRenderState state) {
        if (state.randomPlayerSkin) {
            return RANDOM_PLAYER_SKINS[Math.floorMod(state.skinVariant, RANDOM_PLAYER_SKINS.length)];
        }
        return TEXTURE;
    }
}
