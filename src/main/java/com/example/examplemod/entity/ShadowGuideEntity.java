package com.example.examplemod.entity;

import com.example.examplemod.ExampleMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;
import java.util.UUID;

public class ShadowGuideEntity extends PathAwareEntity {
    private static final int MAX_GUIDE_TICKS = 20 * 50;
    private UUID targetPlayerUuid;

    public ShadowGuideEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        this.setPersistent();
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, 20.0)
                .add(EntityAttributes.MOVEMENT_SPEED, 0.36)
                .add(EntityAttributes.FOLLOW_RANGE, 48.0)
                .add(EntityAttributes.STEP_HEIGHT, 1.0);
    }

    public void setTargetPlayer(ServerPlayerEntity player) {
        this.targetPlayerUuid = player.getUuid();
    }

    public boolean isGuiding(PlayerEntity player) {
        return targetPlayerUuid != null && targetPlayerUuid.equals(player.getUuid());
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new ApproachTargetPlayerGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getEntityWorld() instanceof ServerWorld world)) return;

        ServerPlayerEntity target = getTargetPlayer(world);
        if (target == null || !target.isAlive() || target.isSpectator()) {
            discard();
            return;
        }

        if (this.age > MAX_GUIDE_TICKS || target.getCommandTags().contains(ExampleMod.WATER_MONSTER_SUMMONED_TAG)) {
            vanish(world);
            return;
        }

        this.getLookControl().lookAt(target, 45.0f, 45.0f);
        if (this.age % 12 == 0) {
            world.spawnParticles(ParticleTypes.PORTAL, this.getX(), this.getBodyY(0.65), this.getZ(), 8, 0.18, 0.45, 0.18, 0.04);
        }
        if (this.squaredDistanceTo(target) <= 8.0) {
            ExampleMod.giveWaterMonsterGuideGifts(target);
            vanish(world);
        }
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    private ServerPlayerEntity getTargetPlayer(ServerWorld world) {
        if (targetPlayerUuid == null) return null;
        return world.getServer().getPlayerManager().getPlayer(targetPlayerUuid);
    }

    private void vanish(ServerWorld world) {
        world.spawnParticles(ParticleTypes.SMOKE, this.getX(), this.getBodyY(0.5), this.getZ(), 16, 0.25, 0.45, 0.25, 0.02);
        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 0.8f, 0.55f);
        discard();
    }

    private static final class ApproachTargetPlayerGoal extends Goal {
        private final ShadowGuideEntity entity;

        private ApproachTargetPlayerGoal(ShadowGuideEntity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return entity.targetPlayerUuid != null && entity.getEntityWorld() instanceof ServerWorld;
        }

        @Override
        public boolean shouldContinue() {
            return canStart();
        }

        @Override
        public boolean shouldRunEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(entity.getEntityWorld() instanceof ServerWorld world)) return;
            ServerPlayerEntity target = entity.getTargetPlayer(world);
            if (target == null) return;

            entity.getLookControl().lookAt(target, 45.0f, 45.0f);
            if (entity.squaredDistanceTo(target) > 6.25) {
                Vec3d targetPos = entityPos(target);
                entity.getNavigation().startMovingTo(targetPos.x, targetPos.y, targetPos.z, 1.18);
                if (!entity.isNavigating() && entity.squaredDistanceTo(target) > 36.0) {
                    Vec3d step = entityPos(target).subtract(entityPos(entity)).normalize().multiply(0.08);
                    entity.addVelocity(step.x, 0.0, step.z);
                }
            } else {
                entity.getNavigation().stop();
            }
        }

        private static Vec3d entityPos(net.minecraft.entity.Entity entity) {
            return new Vec3d(entity.getX(), entity.getY(), entity.getZ());
        }
    }
}
