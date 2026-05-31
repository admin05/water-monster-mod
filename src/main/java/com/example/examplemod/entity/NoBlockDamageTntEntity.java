package com.example.examplemod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;

public class NoBlockDamageTntEntity extends Entity {
    private static final String FUSE_NBT_KEY = "fuse";
    private static final String BREAK_BLOCKS_NBT_KEY = "break_blocks";
    private static final int DEFAULT_FUSE = 60;

    private LivingEntity owner;
    private int fuse = DEFAULT_FUSE;
    private boolean breakBlocks;

    public NoBlockDamageTntEntity(EntityType<? extends NoBlockDamageTntEntity> entityType, World world) {
        super(entityType, world);
        this.intersectionChecked = true;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    public void tick() {
        this.tickPortalTeleportation();
        this.applyGravity();
        this.move(MovementType.SELF, this.getVelocity());
        this.tickBlockCollision();
        this.setVelocity(this.getVelocity().multiply(0.98));

        if (this.isOnGround()) {
            this.setVelocity(this.getVelocity().multiply(0.7, -0.5, 0.7));
        }

        this.fuse--;
        if (this.fuse <= 0) {
            this.discard();
            if (!this.getEntityWorld().isClient()) {
                this.explode();
            }
        } else {
            this.updateWaterState();
            if (this.getEntityWorld().isClient()) {
                this.getEntityWorld().addParticleClient(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    public LivingEntity getOwner() {
        return owner;
    }

    public void setOwner(LivingEntity owner) {
        this.owner = owner;
    }

    public void setFuse(int fuse) {
        this.fuse = fuse;
    }

    public int getFuse() {
        return this.fuse;
    }

    public void setBreakBlocks(boolean breakBlocks) {
        this.breakBlocks = breakBlocks;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected Entity.MoveEffect getMoveEffect() {
        return Entity.MoveEffect.NONE;
    }

    @Override
    protected double getGravity() {
        return 0.06;
    }

    @Override
    protected void writeCustomData(WriteView view) {
        view.putShort(FUSE_NBT_KEY, (short) this.fuse);
        view.putBoolean(BREAK_BLOCKS_NBT_KEY, this.breakBlocks);
    }

    @Override
    protected void readCustomData(ReadView view) {
        this.fuse = view.getShort(FUSE_NBT_KEY, (short) DEFAULT_FUSE);
        this.breakBlocks = view.getBoolean(BREAK_BLOCKS_NBT_KEY, false);
    }

    private void explode() {
        this.getEntityWorld().createExplosion(
                this,
                this.getX(),
                this.getY(),
                this.getZ(),
                3.6f,
                false,
                this.breakBlocks ? World.ExplosionSourceType.TNT : World.ExplosionSourceType.NONE
        );
    }
}
