package com.example.examplemod.entity.client;

import com.example.examplemod.entity.WaterMonsterEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.state.ArmedEntityRenderState;
import net.minecraft.util.Identifier;

public class WaterMonsterRenderer extends MobEntityRenderer<WaterMonsterEntity, WaterMonsterRenderState, WaterMonsterModel> {
    private static final Identifier TEXTURE = Identifier.of("examplemod", "textures/entity/water_creature.png");
    private static final Identifier PHASE_THREE_SKIN = Identifier.of("examplemod", "textures/entity/water_monster_phase_three_steve.png");
    private static final Identifier CRITICAL_SKIN = Identifier.of("examplemod", "textures/entity/water_monster_critical_him.png");
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
        state.phaseThreeSkin = entity.shouldUsePhaseThreeSkin();
        state.criticalSkinFlicker = entity.shouldFlickerCriticalSkin();
        state.humanoidForm = state.randomPlayerSkin || state.phaseThreeSkin || entity.isHumanoidForm();
        state.attacking = entity.isAttacking();
        state.skinVariant = entity.getSkinVariant();
    }

    @Override
    public Identifier getTexture(WaterMonsterRenderState state) {
        if (state.randomPlayerSkin) {
            return RANDOM_PLAYER_SKINS[Math.floorMod(state.skinVariant, RANDOM_PLAYER_SKINS.length)];
        }
        if (state.phaseThreeSkin) {
            if (state.criticalSkinFlicker && shouldShowCriticalSkin(state)) {
                return CRITICAL_SKIN;
            }
            return PHASE_THREE_SKIN;
        }
        return TEXTURE;
    }

    private static boolean shouldShowCriticalSkin(WaterMonsterRenderState state) {
        int flickerStep = ((int) state.age / 8) % 5;
        return flickerStep == 1 || flickerStep == 3;
    }
}
