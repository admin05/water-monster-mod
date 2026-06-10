package com.example.examplemod.entity;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.item.TntFishingRodItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class WaterMonsterEntity extends HostileEntity {
    private static final int PHASE_ONE = 1;
    private static final int PHASE_TWO = 2;
    private static final int PHASE_THREE = 3;
    private static final double PHASE_HEALTH = 100.0;
    private static final double TOTAL_HEALTH = PHASE_HEALTH * 3.0;
    private static final double WATER_SPEED = 2.6;
    private static final double LAND_SPEED = 0.32;
    private static final double SMART_TARGET_RANGE = 64.0;

    private final List<ItemStack> copiedInventory = new ArrayList<>();
    private final ServerBossBar bossBar = new ServerBossBar(Text.literal("Water Monster"), BossBar.Color.BLUE, BossBar.Style.NOTCHED_10);
    private int lastPhase = PHASE_ONE;
    private int mimicActionCooldown;
    private int smartActionCooldown;
    private int inventoryCopyCooldown;
    private int targetScanCooldown;
    private int tntRodCooldown;
    private int tntRailCannonCooldown;
    private int shieldUseTicks;
    private int shieldBreakCooldown;
    private int spearChargeTicks;
    private LivingEntity spearTarget;
    private int maceLaunchTicks;
    private int maceDiveTicks;
    private double maceStartY;
    private double macePeakY;

    public WaterMonsterEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    public void tick() {
        super.tick();
        int phase = getCombatPhase();
        handlePhaseTransition(phase);
        updateBossBar(phase);
        updateShapeSpeed();
        PlayerEntity copiedPlayer = mimicNearestPlayer(phase);
        tickTntRodCooldown();
        tickTntRailCannonCooldown();
        tickShieldBreakCooldown();

        if (phase == PHASE_ONE) {
            tickMimicPhase(copiedPlayer);
            return;
        }

        updateSmartTarget();
        updateCombatMovement();
        if (phase >= PHASE_THREE && (tryUseTntRailCannonOnTarget() || tryUseTntFishingRodOnTarget())) {
            return;
        }
        if (shouldPreferSwordOrAxeOnFlatGround(this.getTarget()) || (!tickMaceWindCombat() && !tickSpearCombat())) {
            useCopiedInventoryIntelligently();
        }
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new PhaseMeleeAttackGoal(this, 1.45, false));
        this.goalSelector.add(2, new DragPlayerUnderwaterGoal(this));
        this.goalSelector.add(3, new DestroyBoatGoal(this));
        this.goalSelector.add(4, new WanderAroundGoal(this, 0.9, 15));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 16.0f));

        this.targetSelector.add(0, new ActiveTargetGoal<PlayerEntity>(
                this,
                PlayerEntity.class,
                true,
                (target, world) -> target instanceof PlayerEntity player && isWorthTargeting(player)
        ));
        this.targetSelector.add(1, new TargetBoatGoal(this));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.MAX_HEALTH, TOTAL_HEALTH)
                .add(EntityAttributes.ATTACK_DAMAGE, 14.0)
                .add(EntityAttributes.MOVEMENT_SPEED, WATER_SPEED)
                .add(EntityAttributes.FOLLOW_RANGE, 64.0)
                .add(EntityAttributes.ARMOR, 4.0)
                .add(EntityAttributes.STEP_HEIGHT, 1.0);
    }

    public boolean isHumanoidForm() {
        return !this.isTouchingWater();
    }

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        bossBar.clearPlayers();
    }

    private int getCombatPhase() {
        if (this.getHealth() > PHASE_HEALTH * 2.0f) return PHASE_ONE;
        if (this.getHealth() > PHASE_HEALTH) return PHASE_TWO;
        return PHASE_THREE;
    }

    private boolean isAutonomousCombatPhase() {
        return getCombatPhase() >= PHASE_TWO;
    }

    private void updateBossBar(int phase) {
        bossBar.setPercent(Math.max(0.0f, Math.min(1.0f, this.getHealth() / this.getMaxHealth())));
        bossBar.setName(Text.literal("Water Monster - Phase " + phase));
        bossBar.setColor(switch (phase) {
            case PHASE_ONE -> BossBar.Color.BLUE;
            case PHASE_TWO -> BossBar.Color.YELLOW;
            default -> BossBar.Color.RED;
        });
    }

    private void handlePhaseTransition(int phase) {
        if (phase == lastPhase) return;

        lastPhase = phase;
        smartActionCooldown = 20;
        mimicActionCooldown = 12;
        spearChargeTicks = 0;
        spearTarget = null;
        maceLaunchTicks = 0;
        maceDiveTicks = 0;
        if (phase >= PHASE_THREE) {
            tntRailCannonCooldown = Math.min(tntRailCannonCooldown, 60);
            tntRodCooldown = Math.min(tntRodCooldown, 80);
        }
        stopShielding();
    }

    private void updateShapeSpeed() {
        EntityAttributeInstance speed = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speed == null) return;

        double wanted = this.isHumanoidForm() ? LAND_SPEED : WATER_SPEED;
        if (speed.getBaseValue() != wanted) {
            speed.setBaseValue(wanted);
        }
    }

    private PlayerEntity mimicNearestPlayer(int phase) {
        if (!(this.getEntityWorld() instanceof ServerWorld)) return null;

        PlayerEntity player = getBestPlayerToCopy();
        if (player == null) return null;

        if (copiedInventory.isEmpty() || inventoryCopyCooldown <= 0) {
            copyPlayerInventory(player);
            inventoryCopyCooldown = 100;
        } else {
            inventoryCopyCooldown--;
        }

        if (this.isHumanoidForm() || phase == PHASE_ONE) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = player.getEquippedStack(slot);
                if (!ItemStack.areEqual(this.getEquippedStack(slot), stack)) {
                    this.equipStack(slot, stack.copy());
                }
            }

            this.setSneaking(player.isSneaking());
            this.setSprinting(player.isSprinting());
            if (player.handSwinging && !this.handSwinging) {
                this.swingHand(player.getActiveHand());
            }
        }

        if (phase >= PHASE_TWO) {
            equipTotemIfAvailable();
        }
        return player;
    }

    private PlayerEntity getBestPlayerToCopy() {
        LivingEntity target = this.getTarget();
        if (target instanceof PlayerEntity player && this.distanceTo(player) <= 24.0 && isWorthTargeting(player)) {
            return player;
        }
        return this.getEntityWorld().getClosestPlayer(this, 16.0);
    }

    private void updateSmartTarget() {
        if (!(this.getEntityWorld() instanceof ServerWorld)) return;
        if (targetScanCooldown > 0) {
            targetScanCooldown--;
            return;
        }
        targetScanCooldown = 10;

        PlayerEntity best = findBestPlayerTarget(SMART_TARGET_RANGE);
        LivingEntity current = this.getTarget();
        if (best != null && (current == null || !current.isAlive() || current.squaredDistanceTo(best) > 9.0)) {
            this.setTarget(best);
        }
    }

    private PlayerEntity findBestPlayerTarget(double range) {
        List<PlayerEntity> players = this.getEntityWorld().getEntitiesByClass(
                PlayerEntity.class,
                this.getBoundingBox().expand(range),
                WaterMonsterEntity::isWorthTargeting
        );
        PlayerEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (PlayerEntity player : players) {
            double score = scorePlayerTarget(player);
            if (score < bestScore) {
                best = player;
                bestScore = score;
            }
        }
        return best;
    }

    private double scorePlayerTarget(PlayerEntity player) {
        double score = this.distanceTo(player);
        if (player.isTouchingWater()) score -= 14.0;
        if (player.hasVehicle()) score -= 8.0;
        if (player.getHealth() < player.getMaxHealth() * 0.4f) score -= 5.0;
        if (!this.canSee(player)) score += 10.0;
        if (isDangerousTarget(player)) score -= 3.0;
        return score;
    }

    private static boolean isWorthTargeting(PlayerEntity player) {
        return player != null && player.isAlive() && !player.isCreative() && !player.isSpectator();
    }

    private void copyPlayerInventory(PlayerEntity player) {
        copiedInventory.clear();
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) copiedInventory.add(stack.copy());
        }
    }

    private void updateCombatMovement() {
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return;

        double distance = this.distanceTo(target);
        double speed = this.isTouchingWater() ? 2.25 : 1.25;
        this.getLookControl().lookAt(target, 45.0f, 45.0f);
        tickShieldUse(target, distance);

        if (distance > 3.0) {
            this.getNavigation().startMovingTo(target, speed);
        } else {
            if (this.getEntityWorld() instanceof ServerWorld serverWorld && smartActionCooldown <= 0) {
                Item weapon = getBestCopiedMeleeWeapon(target);
                if (weapon != null) equipCopied(weapon);
                this.tryAttack(serverWorld, target);
                this.swingHand(Hand.MAIN_HAND);
                smartActionCooldown = 12;
            }
            if (this.isTouchingWater() && !target.isTouchingWater()) {
                Vec3d pull = horizontalDirectionTo(target, this);
                target.addVelocity(pull.x * 0.25, -0.25, pull.z * 0.25);
            }
        }
    }

    private boolean isDangerousTarget(LivingEntity target) {
        Item main = target.getMainHandStack().getItem();
        Item off = target.getOffHandStack().getItem();
        return main == Items.BOW || main == Items.CROSSBOW || main == Items.TRIDENT || main == Items.MACE
                || main == Items.NETHERITE_AXE || main == Items.NETHERITE_SWORD
                || main == Items.DIAMOND_AXE || main == Items.DIAMOND_SWORD
                || off == Items.SHIELD;
    }

    private Vec3d predictAimPoint(LivingEntity target, double projectileSpeed, double verticalBias) {
        double distance = this.distanceTo(target);
        double ticksAhead = Math.min(16.0, Math.max(2.0, distance / Math.max(0.1, projectileSpeed)));
        return target.getEyePos().add(target.getVelocity().multiply(ticksAhead)).add(0.0, verticalBias, 0.0);
    }

    private static Vec3d horizontalDirectionTo(Entity from, Entity to) {
        Vec3d direction = new Vec3d(to.getX() - from.getX(), 0.0, to.getZ() - from.getZ());
        if (direction.lengthSquared() < 0.0001) return new Vec3d(1.0, 0.0, 0.0);
        return direction.normalize();
    }

    private BoatEntity findBestBoatTarget(double range) {
        List<BoatEntity> boats = this.getEntityWorld().getEntitiesByClass(
                BoatEntity.class,
                this.getBoundingBox().expand(range),
                boat -> boat != null && !boat.isRemoved()
        );
        BoatEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (BoatEntity boat : boats) {
            double score = this.distanceTo(boat);
            if (!boat.getPassengerList().isEmpty()) score -= 10.0;
            if (boat.hasPassenger(player -> player instanceof PlayerEntity)) score -= 12.0;
            if (score < bestScore) {
                best = boat;
                bestScore = score;
            }
        }
        return best;
    }

    private void tickMimicPhase(PlayerEntity copiedPlayer) {
        if (mimicActionCooldown > 0) {
            mimicActionCooldown--;
        }
        if (copiedPlayer == null || !copiedPlayer.isAlive()) return;

        LivingEntity target = this.getTarget();
        if (!(target instanceof PlayerEntity) || !target.isAlive()) {
            this.setTarget(copiedPlayer);
            target = copiedPlayer;
        }

        double distance = this.distanceTo(target);
        this.getLookControl().lookAt(target, 45.0f, 45.0f);
        tickShieldUse(target, distance);
        mirrorActiveItemUse(copiedPlayer, target, distance);

        if (distance > 5.0 && copiedPlayer.isSprinting()) {
            this.getNavigation().startMovingTo(target, this.isTouchingWater() ? 2.0 : 1.1);
        } else if (copiedPlayer.isSneaking()) {
            this.getNavigation().stop();
        }

        if (copiedPlayer.handSwinging && mimicActionCooldown <= 0) {
            mimicOffensiveAction(copiedPlayer, target, distance);
        }
    }

    private void mirrorActiveItemUse(PlayerEntity copiedPlayer, LivingEntity target, double distance) {
        if (!copiedPlayer.isUsingItem()) return;

        Hand activeHand = copiedPlayer.getActiveHand();
        ItemStack activeStack = copiedPlayer.getStackInHand(activeHand);
        this.setStackInHand(activeHand, activeStack.copy());

        Item activeItem = activeStack.getItem();
        if (activeItem == Items.SHIELD && activeHand == Hand.OFF_HAND && !this.isUsingItem()) {
            this.setCurrentHand(Hand.OFF_HAND);
            shieldUseTicks = 20;
            return;
        }

        if (mimicActionCooldown > 0 || !(this.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        if ((activeItem == Items.BOW || activeItem == Items.CROSSBOW) && distance >= 6.0 && distance <= 34.0) {
            shootArrow(serverWorld, target);
            mimicActionCooldown = 35;
        } else if (isFoodLike(activeItem) && this.getHealth() < this.getMaxHealth()) {
            useFood();
            mimicActionCooldown = 45;
        }
    }

    private void mimicOffensiveAction(PlayerEntity copiedPlayer, LivingEntity target, double distance) {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return;

        Item item = copiedPlayer.getMainHandStack().getItem();
        this.setStackInHand(Hand.MAIN_HAND, copiedPlayer.getMainHandStack().copy());

        if ((item == Items.BOW || item == Items.CROSSBOW) && hasCopied(Items.ARROW) && distance >= 6.0 && distance <= 34.0) {
            shootArrow(serverWorld, target);
            mimicActionCooldown = 35;
            return;
        }
        if (item == Items.TRIDENT && hasCopied(Items.TRIDENT) && distance <= 30.0) {
            throwTrident(serverWorld, target);
            mimicActionCooldown = 45;
            return;
        }
        if (item == Items.WIND_CHARGE && distance > 4.0) {
            launchWindCharge(serverWorld, target);
            mimicActionCooldown = 30;
            return;
        }
        if (item == Items.ENDER_PEARL && distance > 10.0) {
            throwEnderPearl(serverWorld, target);
            mimicActionCooldown = 70;
            return;
        }

        if (distance <= 3.4) {
            this.tryAttack(serverWorld, target);
            this.swingHand(Hand.MAIN_HAND);
        } else if (!copiedPlayer.isSneaking()) {
            this.getNavigation().startMovingTo(target, copiedPlayer.isSprinting() ? 1.45 : 1.05);
            this.swingHand(Hand.MAIN_HAND);
        }
        mimicActionCooldown = copiedPlayer.isSprinting() ? 8 : 12;
    }

    private void useCopiedInventoryIntelligently() {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return;
        if (smartActionCooldown > 0) {
            smartActionCooldown--;
            return;
        }

        LivingEntity target = this.getTarget();
        if (this.getHealth() < this.getMaxHealth() * 0.55f && useFood()) {
            smartActionCooldown = 45;
            return;
        }

        if (target == null || !target.isAlive()) return;
        double distance = this.distanceTo(target);

        useShieldIfAvailable(target, distance);

        if (shouldPreferSwordOrAxeOnFlatGround(target)) {
            Item weapon = getBestCopiedSwordOrAxe();
            if (weapon != null) equipCopied(weapon);
            if (distance < 3.2) {
                this.tryAttack(serverWorld, target);
                this.swingHand(Hand.MAIN_HAND);
                smartActionCooldown = 12;
            }
            return;
        }

        if (hasCopied(Items.TRIDENT) && handleTridentCombat(serverWorld, target, distance)) {
            return;
        }

        if ((hasCopied(Items.BOW) || hasCopied(Items.CROSSBOW)) && hasCopied(Items.ARROW) && distance >= 7.0 && distance <= 34.0) {
            equipCopied(hasCopied(Items.BOW) ? Items.BOW : Items.CROSSBOW);
            shootArrow(serverWorld, target);
            consumeCopied(Items.ARROW);
            smartActionCooldown = 35;
            return;
        }

        if (hasCopied(Items.ENDER_PEARL) && distance > 12.0) {
            equipCopied(Items.ENDER_PEARL);
            throwEnderPearl(serverWorld, target);
            consumeCopied(Items.ENDER_PEARL);
            smartActionCooldown = 80;
            return;
        }

        if (hasCopied(Items.WIND_CHARGE) && distance > 4.0) {
            equipCopied(Items.WIND_CHARGE);
            launchWindCharge(serverWorld, target);
            consumeCopied(Items.WIND_CHARGE);
            smartActionCooldown = 35;
        }

        if (distance < 3.0) {
            Item weapon = getBestCopiedMeleeWeapon(target);
            if (weapon != null) equipCopied(weapon);
            this.tryAttack(serverWorld, target);
            this.swingHand(Hand.MAIN_HAND);
            smartActionCooldown = 18;
        }
    }

    private void useShieldIfAvailable(LivingEntity target, double distance) {
        if (this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (shieldBreakCooldown > 0) return;
        ItemStack shield = findCopied(Items.SHIELD);
        if (shield == null) return;

        if (!ItemStack.areEqual(this.getOffHandStack(), shield)) {
            this.setStackInHand(Hand.OFF_HAND, shield.copy());
        }

        if (distance < 10.0 && !this.isUsingItem()) {
            this.setCurrentHand(Hand.OFF_HAND);
            shieldUseTicks = 20;
            this.getLookControl().lookAt(target, 45.0f, 45.0f);
        }
    }

    private void tickShieldUse(LivingEntity target, double distance) {
        if (!this.isUsingItem() || !this.getActiveHand().equals(Hand.OFF_HAND) || !this.getOffHandStack().isOf(Items.SHIELD)) {
            shieldUseTicks = 0;
            return;
        }

        shieldUseTicks++;
        if (shieldBreakCooldown > 0 || distance > 12.0 || target == null || !target.isAlive()) {
            stopShielding();
        }
    }

    private void stopShielding() {
        if (this.isUsingItem() && this.getActiveHand().equals(Hand.OFF_HAND) && this.getOffHandStack().isOf(Items.SHIELD)) {
            this.stopUsingItem();
        }
        shieldUseTicks = 0;
    }

    private void tickShieldBreakCooldown() {
        if (shieldBreakCooldown > 0) {
            shieldBreakCooldown--;
        }
    }

    private void equipTotemIfAvailable() {
        if (this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        ItemStack totem = findCopied(Items.TOTEM_OF_UNDYING);
        if (totem != null) {
            this.setStackInHand(Hand.OFF_HAND, totem.copyWithCount(1));
        }
    }

    private void tickTntRodCooldown() {
        if (tntRodCooldown > 0) {
            tntRodCooldown--;
        }
    }

    private void tickTntRailCannonCooldown() {
        if (tntRailCannonCooldown > 0) {
            tntRailCannonCooldown--;
        }
    }

    private boolean tryUseTntRailCannonOnTarget() {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return false;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return false;
        double distance = this.distanceTo(target);
        return distance >= 8.0 && distance <= 64.0 && useTntRailCannon(serverWorld, target);
    }

    private boolean useTntRailCannon(ServerWorld world, LivingEntity target) {
        if (tntRailCannonCooldown > 0) return false;

        this.setStackInHand(Hand.MAIN_HAND, new ItemStack(ExampleMod.TNT_FISHING_ROD));
        Vec3d muzzle = this.getEyePos().add(0.0, 0.35, 0.0);
        Vec3d aim = predictAimPoint(target, 2.8, 0.0).subtract(muzzle);
        if (aim.lengthSquared() < 0.0001) return false;
        aim = aim.normalize();

        Vec3d rail = new Vec3d(-aim.z, 0.0, aim.x);
        if (rail.lengthSquared() < 0.0001) {
            rail = new Vec3d(1.0, 0.0, 0.0);
        } else {
            rail = rail.normalize();
        }

        for (int i = 0; i < 10; i++) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3d position = muzzle
                        .add(aim.multiply(2.0 + i * 1.25))
                        .add(rail.multiply(side * 0.85))
                        .add(0.0, i * 0.03, 0.0);
                NoBlockDamageTntEntity tnt = new NoBlockDamageTntEntity(ExampleMod.NO_BLOCK_DAMAGE_TNT, world);
                tnt.setPosition(position.x, position.y, position.z);
                tnt.setOwner(this);
                tnt.setBreakBlocks(false);
                tnt.setFuse(34 + i * 2 + world.random.nextInt(6));
                Vec3d velocity = aim.multiply(1.15 + i * 0.035).add(0.0, 0.04, 0.0);
                tnt.setVelocity(velocity);
                world.spawnEntity(tnt);
            }
        }

        this.swingHand(Hand.MAIN_HAND);
        tntRailCannonCooldown = 260;
        smartActionCooldown = 50;
        return true;
    }

    private boolean tryUseTntFishingRodOnTarget() {
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld)) return false;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) return false;
        double distance = this.distanceTo(target);
        return distance >= 4.0 && distance <= 48.0 && useTntFishingRod(serverWorld, target);
    }

    private boolean useTntFishingRod(ServerWorld world, LivingEntity target) {
        if (tntRodCooldown > 0) return false;

        this.setStackInHand(Hand.MAIN_HAND, new ItemStack(ExampleMod.TNT_FISHING_ROD));
        Vec3d center = predictAimPoint(target, 1.0, 0.0);
        TntFishingRodItem.summonTntRing(world, this, center, false);
        this.swingHand(Hand.MAIN_HAND);
        tntRodCooldown = 220;
        smartActionCooldown = 45;
        return true;
    }

    private boolean tickMaceWindCombat() {
        if (!hasCopied(Items.MACE) || !hasCopied(Items.WIND_CHARGE)) {
            maceLaunchTicks = 0;
            maceDiveTicks = 0;
            return false;
        }

        LivingEntity target = this.getTarget();
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld) || target == null || !target.isAlive()) return false;
        if (shouldPreferSwordOrAxeOnFlatGround(target)) return false;

        double distance = this.distanceTo(target);
        equipCopied(Items.MACE);

        if (maceDiveTicks > 0) {
            maceDiveTicks--;
            macePeakY = Math.max(macePeakY, this.getY());
            Vec3d dir = predictAimPoint(target, 1.8, -0.25).subtract(this.getEyePos()).normalize();
            this.setVelocity(dir.x * 1.25, -1.55, dir.z * 1.25);
            this.velocityDirty = true;
            this.getLookControl().lookAt(target, 45.0f, 45.0f);
            if (distance < 2.8) {
                applyMaceImpact(serverWorld, target);
                maceDiveTicks = 0;
                smartActionCooldown = 60;
            }
            return true;
        }

        if (maceLaunchTicks > 0) {
            maceLaunchTicks--;
            macePeakY = Math.max(macePeakY, this.getY());
            Vec3d above = horizontalDirectionTo(this, target);
            this.addVelocity(above.x * 0.12, 0.02, above.z * 0.12);
            this.getLookControl().lookAt(target, 45.0f, 45.0f);
            if (maceLaunchTicks <= 0 || this.getY() - maceStartY > 4.0) {
                maceDiveTicks = 18;
            }
            return true;
        }

        if (smartActionCooldown <= 0 && distance >= 3.0 && distance <= 14.0) {
            maceStartY = this.getY();
            macePeakY = maceStartY;
            launchWindCharge(serverWorld, target);
            consumeCopied(Items.WIND_CHARGE);
            this.addVelocity(0, 1.75, 0);
            maceLaunchTicks = 14;
            return true;
        }

        return false;
    }

    private void applyMaceImpact(ServerWorld world, LivingEntity target) {
        this.tryAttack(world, target);
        Vec3d knock = horizontalDirectionTo(this, target);
        target.addVelocity(knock.x * 1.1, 0.45, knock.z * 1.1);
        this.swingHand(Hand.MAIN_HAND);
    }

    private boolean tickSpearCombat() {
        Item spear = getBestCopiedSpear();
        if (spear == null) return false;
        LivingEntity target = this.getTarget();
        if (!(this.getEntityWorld() instanceof ServerWorld serverWorld) || target == null || !target.isAlive()) {
            spearChargeTicks = 0;
            spearTarget = null;
            return false;
        }
        if (shouldPreferSwordOrAxeOnFlatGround(target)) return false;

        equipCopied(spear);
        double distance = this.distanceTo(target);

        if (spearChargeTicks > 0) {
            spearChargeTicks--;
            Vec3d dir = predictAimPoint(target, 2.2, -0.15).subtract(this.getEyePos()).normalize();
            this.setVelocity(dir.x * 2.2, Math.max(0.05, dir.y * 0.25), dir.z * 2.2);
            this.velocityDirty = true;
            this.getLookControl().lookAt(target, 45.0f, 45.0f);
            if (distance < 2.4) {
                applySpearImpact(serverWorld, target, spear);
                spearChargeTicks = 0;
                smartActionCooldown = 22;
            }
            return true;
        }

        if (distance >= 7.0 && distance <= 18.0 && smartActionCooldown <= 0) {
            spearTarget = target;
            spearChargeTicks = 18;
            this.getNavigation().stop();
            return true;
        }

        return false;
    }

    private void applySpearImpact(ServerWorld world, LivingEntity target, Item spear) {
        this.tryAttack(world, target);
        Vec3d knock = horizontalDirectionTo(this, target);
        target.addVelocity(knock.x * 0.8, 0.25, knock.z * 0.8);
        this.swingHand(Hand.MAIN_HAND);
    }

    private Item getBestCopiedSpear() {
        Item[] order = new Item[] {
                Items.NETHERITE_SPEAR, Items.DIAMOND_SPEAR, Items.IRON_SPEAR, Items.COPPER_SPEAR,
                Items.STONE_SPEAR, Items.GOLDEN_SPEAR, Items.WOODEN_SPEAR
        };
        for (Item item : order) {
            if (hasCopied(item)) return item;
        }
        return null;
    }

    private Item getBestCopiedMeleeWeapon(LivingEntity target) {
        if (shouldPreferSwordOrAxeOnFlatGround(target)) {
            Item flatGroundWeapon = getBestCopiedSwordOrAxe();
            if (flatGroundWeapon != null) return flatGroundWeapon;
        }

        Item[] order = new Item[] {
                Items.NETHERITE_AXE, Items.NETHERITE_SWORD, Items.NETHERITE_SPEAR, Items.MACE,
                Items.DIAMOND_AXE, Items.DIAMOND_SWORD, Items.DIAMOND_SPEAR,
                Items.IRON_AXE, Items.IRON_SWORD, Items.IRON_SPEAR,
                Items.COPPER_AXE, Items.COPPER_SWORD, Items.COPPER_SPEAR,
                Items.STONE_AXE, Items.STONE_SWORD, Items.STONE_SPEAR,
                Items.GOLDEN_AXE, Items.GOLDEN_SWORD, Items.GOLDEN_SPEAR,
                Items.WOODEN_AXE, Items.WOODEN_SWORD, Items.WOODEN_SPEAR
        };
        for (Item item : order) {
            if (hasCopied(item)) return item;
        }
        return getBestCopiedTool();
    }

    private Item getBestCopiedSwordOrAxe() {
        Item[] order = new Item[] {
                Items.NETHERITE_AXE, Items.NETHERITE_SWORD,
                Items.DIAMOND_AXE, Items.DIAMOND_SWORD,
                Items.IRON_AXE, Items.IRON_SWORD,
                Items.COPPER_AXE, Items.COPPER_SWORD,
                Items.STONE_AXE, Items.STONE_SWORD,
                Items.GOLDEN_AXE, Items.GOLDEN_SWORD,
                Items.WOODEN_AXE, Items.WOODEN_SWORD
        };
        for (Item item : order) {
            if (hasCopied(item)) return item;
        }
        return null;
    }

    private Item getBestCopiedTool() {
        Item[] order = new Item[] {
                Items.NETHERITE_PICKAXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE,
                Items.DIAMOND_PICKAXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE,
                Items.IRON_PICKAXE, Items.IRON_SHOVEL, Items.IRON_HOE,
                Items.COPPER_PICKAXE, Items.COPPER_SHOVEL, Items.COPPER_HOE,
                Items.STONE_PICKAXE, Items.STONE_SHOVEL, Items.STONE_HOE,
                Items.GOLDEN_PICKAXE, Items.GOLDEN_SHOVEL, Items.GOLDEN_HOE,
                Items.WOODEN_PICKAXE, Items.WOODEN_SHOVEL, Items.WOODEN_HOE,
                Items.SHEARS, Items.FLINT_AND_STEEL
        };
        for (Item item : order) {
            if (hasCopied(item)) return item;
        }
        return null;
    }

    private boolean shouldPreferSwordOrAxeOnFlatGround(LivingEntity target) {
        if (target == null || !target.isAlive() || getBestCopiedSwordOrAxe() == null) return false;
        if (this.isTouchingWater() || target.isTouchingWater()) return false;
        if (Math.abs(this.getY() - target.getY()) > 0.75) return false;
        return hasSolidGroundBelow(this) && hasSolidGroundBelow(target);
    }

    private boolean hasSolidGroundBelow(Entity entity) {
        BlockPos ground = entity.getBlockPos().down();
        return this.getEntityWorld().getBlockState(ground).isSolidBlock(this.getEntityWorld(), ground);
    }

    private boolean useFood() {
        ItemStack food = findCopiedFood();
        if (food == null) return false;
        this.setStackInHand(Hand.MAIN_HAND, food.copy());
        this.heal(getHealingAmount(food.getItem()));
        this.swingHand(Hand.MAIN_HAND);
        food.decrement(1);
        copiedInventory.removeIf(ItemStack::isEmpty);
        return true;
    }

    private ItemStack findCopiedFood() {
        ItemStack enchantedApple = findCopied(Items.ENCHANTED_GOLDEN_APPLE);
        if (enchantedApple != null) return enchantedApple;
        ItemStack goldenApple = findCopied(Items.GOLDEN_APPLE);
        if (goldenApple != null) return goldenApple;
        for (ItemStack stack : copiedInventory) {
            if (isFoodLike(stack.getItem())) return stack;
        }
        return null;
    }

    private float getHealingAmount(Item item) {
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return 18.0f;
        if (item == Items.GOLDEN_APPLE) return 12.0f;
        if (item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP) return 8.0f;
        if (item == Items.COOKED_CHICKEN || item == Items.COOKED_MUTTON || item == Items.COOKED_SALMON) return 6.0f;
        return 4.0f;
    }

    private boolean isFoodLike(Item item) {
        return item == Items.COOKED_BEEF || item == Items.BREAD || item == Items.COOKED_PORKCHOP
                || item == Items.COOKED_CHICKEN || item == Items.COOKED_MUTTON || item == Items.COOKED_COD
                || item == Items.COOKED_SALMON || item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE
                || item == Items.CARROT || item == Items.POTATO || item == Items.BAKED_POTATO || item == Items.APPLE;
    }

    private boolean hasCopied(Item item) {
        return findCopied(item) != null;
    }

    private ItemStack findCopied(Item item) {
        for (ItemStack stack : copiedInventory) {
            if (stack.isOf(item)) return stack;
        }
        return null;
    }

    private void equipCopied(Item item) {
        ItemStack stack = findCopied(item);
        if (stack != null) this.setStackInHand(Hand.MAIN_HAND, stack.copy());
    }

    private boolean consumeCopied(Item item) {
        ItemStack stack = findCopied(item);
        if (stack == null) return false;
        stack.decrement(1);
        copiedInventory.removeIf(ItemStack::isEmpty);
        return true;
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        if (isShielding() && canBreakShield(source)) {
            breakShield();
        }
        if (amount >= this.getHealth() && useTotemOfUndying()) {
            this.setHealth(Math.min(this.getMaxHealth() * 0.5f, 20.0f));
            this.clearStatusEffects();
            this.heal(8.0f);
            return false;
        }
        return super.damage(world, source, amount);
    }

    private boolean isShielding() {
        return this.isUsingItem() && this.getActiveHand().equals(Hand.OFF_HAND) && this.getOffHandStack().isOf(Items.SHIELD);
    }

    private boolean canBreakShield(DamageSource source) {
        ItemStack weaponStack = source.getWeaponStack();
        if (weaponStack == null || weaponStack.isEmpty()) return false;
        Item weapon = weaponStack.getItem();
        return weapon == Items.WOODEN_AXE || weapon == Items.STONE_AXE || weapon == Items.IRON_AXE
                || weapon == Items.GOLDEN_AXE || weapon == Items.DIAMOND_AXE || weapon == Items.NETHERITE_AXE
                || weapon == Items.MACE;
    }

    private void breakShield() {
        stopShielding();
        shieldBreakCooldown = 100;
        this.swingHand(Hand.OFF_HAND);
    }

    private boolean useTotemOfUndying() {
        ItemStack totem = findCopied(Items.TOTEM_OF_UNDYING);
        if (totem == null && this.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) {
            totem = this.getOffHandStack();
        }
        if (totem == null) return false;

        totem.decrement(1);
        copiedInventory.removeIf(ItemStack::isEmpty);
        this.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
        equipTotemIfAvailable();
        this.swingHand(Hand.OFF_HAND);
        return true;
    }

    @Override
    protected void dropLoot(ServerWorld world, DamageSource damageSource, boolean causedByPlayer) {
        super.dropLoot(world, damageSource, causedByPlayer);
        this.dropStack(world, new ItemStack(ExampleMod.TNT_FISHING_ROD));
    }

    private void throwEnderPearl(ServerWorld world, LivingEntity target) {
        EnderPearlEntity pearl = new EnderPearlEntity(world, this, new ItemStack(Items.ENDER_PEARL));
        pearl.setPosition(this.getX(), this.getEyeY() - 0.1, this.getZ());
        Vec3d landing = new Vec3d(target.getX(), target.getY(), target.getZ()).add(horizontalDirectionTo(this, target).multiply(-2.0)).add(target.getVelocity().multiply(8.0));
        Vec3d dir = landing.add(0.0, 1.0, 0.0).subtract(new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ())).normalize();
        pearl.setVelocity(dir.x, dir.y + 0.12, dir.z, 1.7f, 0.5f);
        world.spawnEntity(pearl);
        this.swingHand(Hand.MAIN_HAND);
    }

    private void launchWindCharge(ServerWorld world, LivingEntity target) {
        Vec3d dir = predictAimPoint(target, 1.4, -0.1).subtract(this.getEyePos()).normalize();
        WindChargeEntity charge = new WindChargeEntity(world, this.getX(), this.getEyeY() - 0.2, this.getZ(), dir.multiply(1.4));
        world.spawnEntity(charge);
        this.swingHand(Hand.MAIN_HAND);
    }

    private void shootArrow(ServerWorld world, LivingEntity target) {
        ItemStack arrowStack = new ItemStack(Items.ARROW);
        ArrowEntity arrow = new ArrowEntity(world, this, arrowStack, this.getMainHandStack());
        Vec3d dir = predictAimPoint(target, 2.2, 0.05).subtract(this.getEyePos()).normalize();
        arrow.setVelocity(dir.x, dir.y + 0.08, dir.z, 2.35f, 0.45f);
        world.spawnEntity(arrow);
        this.swingHand(Hand.MAIN_HAND);
    }

    private boolean handleTridentCombat(ServerWorld world, LivingEntity target, double distance) {
        equipCopied(Items.TRIDENT);

        if (distance <= 30.0) {
            throwTrident(world, target);
            consumeCopied(Items.TRIDENT);
            smartActionCooldown = 55;
            return true;
        }

        return false;
    }

    private void throwTrident(ServerWorld world, LivingEntity target) {
        ItemStack tridentStack = findCopied(Items.TRIDENT).copy();
        TridentEntity trident = new TridentEntity(world, this, tridentStack);
        trident.setPosition(this.getX(), this.getEyeY() - 0.1, this.getZ());
        Vec3d dir = predictAimPoint(target, 2.5, 0.0).subtract(new Vec3d(trident.getX(), trident.getY(), trident.getZ())).normalize();
        trident.setVelocity(dir.x, dir.y + 0.08, dir.z, 2.7f, 0.2f);
        world.spawnEntity(trident);
        this.swingHand(Hand.MAIN_HAND);
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public boolean canSpawn(WorldView world) {
        return world.getFluidState(this.getBlockPos()).isIn(FluidTags.WATER);
    }

    static class PhaseMeleeAttackGoal extends MeleeAttackGoal {
        private final WaterMonsterEntity entity;

        PhaseMeleeAttackGoal(WaterMonsterEntity entity, double speed, boolean pauseWhenMobIdle) {
            super(entity, speed, pauseWhenMobIdle);
            this.entity = entity;
        }

        @Override
        public boolean canStart() {
            return entity.isAutonomousCombatPhase() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return entity.isAutonomousCombatPhase() && super.shouldContinue();
        }
    }

    static class DragPlayerUnderwaterGoal extends Goal {
        private final WaterMonsterEntity entity;
        private PlayerEntity target;
        private int attackTimer;

        DragPlayerUnderwaterGoal(WaterMonsterEntity entity) {
            this.entity = entity;
            this.attackTimer = 0;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (!entity.isAutonomousCombatPhase()) return false;
            LivingEntity t = entity.getTarget();
            if (t instanceof PlayerEntity player && player.isAlive()) {
                target = player;
                return entity.isTouchingWater() || target.isTouchingWater();
            }
            return false;
        }

        @Override
        public boolean shouldContinue() {
            return entity.isAutonomousCombatPhase() && target != null && target.isAlive() && entity.distanceTo(target) < 6.0;
        }

        @Override
        public void stop() {
            target = null;
            attackTimer = 0;
        }

        @Override
        public void tick() {
            if (target == null) return;

            entity.getNavigation().startMovingTo(target, entity.isTouchingWater() ? 2.0 : 1.0);
            entity.getLookControl().lookAt(target, 30.0f, 30.0f);

            double dist = entity.distanceTo(target);

            if (dist < 4.0) {
                if (!target.isTouchingWater() && entity.isTouchingWater()) {
                    Vec3d pull = new Vec3d(
                            entity.getX() - target.getX(),
                            0,
                            entity.getZ() - target.getZ()
                    ).normalize();
                    target.addVelocity(pull.x * 0.6, 0.2, pull.z * 0.6);
                    target.addVelocity(0, -0.4, 0);
                }

                if (target.isTouchingWater() && dist < 3.0 && entity.getEntityWorld() instanceof ServerWorld serverWorld) {
                    target.addVelocity(0, -0.5, 0);
                    target.setAir(Math.max(-20, target.getAir() - 40));

                    if (target.getAir() <= -10) {
                        attackTimer++;
                        if (attackTimer >= 10) {
                            target.damage(serverWorld, entity.getDamageSources().mobAttack(entity), 3.0f);
                            attackTimer = 0;
                        }
                    }
                }
            }
        }
    }

    static class DestroyBoatGoal extends Goal {
        private final WaterMonsterEntity entity;
        private BoatEntity targetBoat;

        DestroyBoatGoal(WaterMonsterEntity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Override
        public boolean canStart() {
            if (!entity.isAutonomousCombatPhase()) return false;
            targetBoat = entity.findBestBoatTarget(24.0);
            return targetBoat != null;
        }

        @Override
        public boolean shouldContinue() {
            return entity.isAutonomousCombatPhase() && targetBoat != null && !targetBoat.isRemoved() && entity.distanceTo(targetBoat) < 30.0;
        }

        @Override
        public void start() {
            if (targetBoat != null) {
                entity.getNavigation().startMovingTo(targetBoat, entity.isTouchingWater() ? 2.2 : 1.0);
            }
        }

        @Override
        public void tick() {
            if (targetBoat == null || targetBoat.isRemoved()) {
                targetBoat = entity.findBestBoatTarget(28.0);
                if (targetBoat == null) return;
            }

            entity.getLookControl().lookAt(targetBoat, 30.0f, 30.0f);
            entity.getNavigation().startMovingTo(targetBoat, entity.isTouchingWater() ? 2.2 : 1.0);

            if (entity.distanceTo(targetBoat) < 2.5 && entity.getEntityWorld() instanceof ServerWorld serverWorld) {
                targetBoat.damage(serverWorld, entity.getDamageSources().mobAttack(entity), 50.0f);
                for (Entity passenger : targetBoat.getPassengerList()) {
                    if (passenger instanceof PlayerEntity player) {
                        player.damage(serverWorld, entity.getDamageSources().mobAttack(entity), 6.0f);
                        player.addVelocity(0, -0.5, 0);
                    }
                }
            }
        }

        @Override
        public void stop() {
            targetBoat = null;
        }
    }

    static class TargetBoatGoal extends Goal {
        private final WaterMonsterEntity entity;
        private BoatEntity target;

        TargetBoatGoal(WaterMonsterEntity entity) {
            this.entity = entity;
            this.setControls(EnumSet.of(Control.TARGET));
        }

        @Override
        public boolean canStart() {
            if (!entity.isAutonomousCombatPhase()) return false;
            target = entity.findBestBoatTarget(28.0);
            return target != null;
        }

        @Override
        public boolean shouldContinue() {
            return entity.isAutonomousCombatPhase() && target != null && !target.isRemoved();
        }

        @Override
        public void stop() {
            target = null;
        }
    }
}
