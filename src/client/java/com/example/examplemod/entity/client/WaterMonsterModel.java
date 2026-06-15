package com.example.examplemod.entity.client;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;

public class WaterMonsterModel extends EntityModel<WaterMonsterRenderState> implements ModelWithArms<WaterMonsterRenderState> {
    private final ModelPart squidMantle;
    private final ModelPart mouth;
    private final ModelPart jawLeft;
    private final ModelPart jawRight;
    private final ModelPart tentacleFrontLeft;
    private final ModelPart tentacleFrontRight;
    private final ModelPart tentacleBackLeft;
    private final ModelPart tentacleBackRight;
    private final ModelPart tentacleSideLeft;
    private final ModelPart tentacleSideRight;
    private final ModelPart humanHead;
    private final ModelPart humanBody;
    private final ModelPart humanLeftArm;
    private final ModelPart humanRightArm;
    private final ModelPart humanLeftLeg;
    private final ModelPart humanRightLeg;

    public WaterMonsterModel(ModelPart root) {
        super(root);
        this.squidMantle = root.getChild("squid_mantle");
        this.mouth = root.getChild("mouth");
        this.jawLeft = root.getChild("jaw_left");
        this.jawRight = root.getChild("jaw_right");
        this.tentacleFrontLeft = root.getChild("tentacle_front_left");
        this.tentacleFrontRight = root.getChild("tentacle_front_right");
        this.tentacleBackLeft = root.getChild("tentacle_back_left");
        this.tentacleBackRight = root.getChild("tentacle_back_right");
        this.tentacleSideLeft = root.getChild("tentacle_side_left");
        this.tentacleSideRight = root.getChild("tentacle_side_right");
        this.humanHead = root.getChild("human_head");
        this.humanBody = root.getChild("human_body");
        this.humanLeftArm = root.getChild("human_left_arm");
        this.humanRightArm = root.getChild("human_right_arm");
        this.humanLeftLeg = root.getChild("human_left_leg");
        this.humanRightLeg = root.getChild("human_right_leg");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        root.addChild("squid_mantle",
                ModelPartBuilder.create()
                        .uv(0, 0).cuboid(-5.0F, -10.0F, -5.0F, 10.0F, 14.0F, 10.0F)
                        .uv(40, 0).cuboid(-3.0F, -15.0F, -3.0F, 6.0F, 5.0F, 6.0F),
                ModelTransform.origin(0.0F, 15.0F, 0.0F));
        root.addChild("mouth",
                ModelPartBuilder.create().uv(0, 24).cuboid(-3.0F, -2.0F, -6.5F, 6.0F, 4.0F, 3.0F),
                ModelTransform.origin(0.0F, 15.0F, 0.0F));
        root.addChild("jaw_left",
                ModelPartBuilder.create().uv(18, 24).cuboid(0.0F, -1.0F, -7.5F, 4.0F, 2.0F, 2.0F),
                ModelTransform.origin(2.0F, 15.0F, 0.0F));
        root.addChild("jaw_right",
                ModelPartBuilder.create().uv(18, 28).cuboid(-4.0F, -1.0F, -7.5F, 4.0F, 2.0F, 2.0F),
                ModelTransform.origin(-2.0F, 15.0F, 0.0F));
        root.addChild("tentacle_front_left",
                ModelPartBuilder.create().uv(0, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F),
                ModelTransform.origin(-3.0F, 18.0F, -3.0F));
        root.addChild("tentacle_front_right",
                ModelPartBuilder.create().uv(8, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 13.0F, 2.0F),
                ModelTransform.origin(3.0F, 18.0F, -3.0F));
        root.addChild("tentacle_back_left",
                ModelPartBuilder.create().uv(16, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                ModelTransform.origin(-3.0F, 18.0F, 3.0F));
        root.addChild("tentacle_back_right",
                ModelPartBuilder.create().uv(24, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 12.0F, 2.0F),
                ModelTransform.origin(3.0F, 18.0F, 3.0F));
        root.addChild("tentacle_side_left",
                ModelPartBuilder.create().uv(32, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                ModelTransform.origin(-5.0F, 17.0F, 0.0F));
        root.addChild("tentacle_side_right",
                ModelPartBuilder.create().uv(40, 32).cuboid(-1.0F, 0.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                ModelTransform.origin(5.0F, 17.0F, 0.0F));

        root.addChild("human_head",
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                ModelTransform.origin(0.0F, 4.0F, 0.0F));
        root.addChild("human_body",
                ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                ModelTransform.origin(0.0F, 4.0F, 0.0F));
        root.addChild("human_left_arm",
                ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.origin(5.0F, 6.0F, 0.0F));
        root.addChild("human_right_arm",
                ModelPartBuilder.create().uv(40, 16).mirrored().cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.origin(-5.0F, 6.0F, 0.0F));
        root.addChild("human_left_leg",
                ModelPartBuilder.create().uv(16, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.origin(2.0F, 16.0F, 0.0F));
        root.addChild("human_right_leg",
                ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.origin(-2.0F, 16.0F, 0.0F));

        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(WaterMonsterRenderState state) {
        setSquidVisible(!state.humanoidForm);
        setHumanVisible(state.humanoidForm);
        float t = state.age;

        if (state.humanoidForm) {
            humanHead.pitch = state.pitch * 0.017453292F;
            humanHead.yaw = state.relativeHeadYaw * 0.017453292F;
            humanBody.pitch = 0.0F;
            humanBody.yaw = 0.0F;
            humanBody.roll = 0.0F;
            float walk = state.limbSwingAnimationProgress;
            float stride = state.limbSwingAmplitude;
            humanLeftArm.pitch = (float) Math.cos(walk * 0.6662F) * 1.4F * stride;
            humanRightArm.pitch = (float) Math.cos(walk * 0.6662F + Math.PI) * 1.4F * stride;
            humanLeftLeg.pitch = (float) Math.cos(walk * 0.6662F + Math.PI) * 1.4F * stride;
            humanRightLeg.pitch = (float) Math.cos(walk * 0.6662F) * 1.4F * stride;
            resetYawAndRoll(humanLeftArm);
            resetYawAndRoll(humanRightArm);
            resetYawAndRoll(humanLeftLeg);
            resetYawAndRoll(humanRightLeg);
            return;
        }

        squidMantle.roll = (float) Math.sin(t * 0.35F) * 0.12F;
        float pulse = (float) Math.sin(t * 0.6F) * 0.35F;
        float bite = state.attacking ? 0.9F : 0.15F;
        mouth.pitch = -0.15F - bite * 0.4F;
        jawLeft.yaw = 0.4F + bite;
        jawRight.yaw = -0.4F - bite;
        tentacleFrontLeft.pitch = 0.35F + pulse;
        tentacleFrontRight.pitch = 0.35F - pulse;
        tentacleBackLeft.pitch = -0.25F - pulse * 0.8F;
        tentacleBackRight.pitch = -0.25F + pulse * 0.8F;
        tentacleSideLeft.roll = 0.25F + pulse * 0.8F;
        tentacleSideRight.roll = -0.25F - pulse * 0.8F;
    }

    private void setSquidVisible(boolean visible) {
        squidMantle.visible = visible;
        mouth.visible = visible;
        jawLeft.visible = visible;
        jawRight.visible = visible;
        tentacleFrontLeft.visible = visible;
        tentacleFrontRight.visible = visible;
        tentacleBackLeft.visible = visible;
        tentacleBackRight.visible = visible;
        tentacleSideLeft.visible = visible;
        tentacleSideRight.visible = visible;
    }

    private void setHumanVisible(boolean visible) {
        humanHead.visible = visible;
        humanBody.visible = visible;
        humanLeftArm.visible = visible;
        humanRightArm.visible = visible;
        humanLeftLeg.visible = visible;
        humanRightLeg.visible = visible;
    }

    private static void resetYawAndRoll(ModelPart part) {
        part.yaw = 0.0F;
        part.roll = 0.0F;
    }

    @Override
    public void setArmAngle(WaterMonsterRenderState state, Arm arm, MatrixStack matrices) {
        ModelPart part = arm == Arm.RIGHT ? humanRightArm : humanLeftArm;
        part.applyTransform(matrices);
    }
}
