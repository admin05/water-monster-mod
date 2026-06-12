package com.example.examplemod;

import com.example.examplemod.entity.NoBlockDamageTntEntity;
import com.example.examplemod.entity.WaterMonsterAltarProtection;
import com.example.examplemod.entity.WaterMonsterEntity;
import com.example.examplemod.item.TntFishingRodItem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExampleMod implements ModInitializer {
    public static final String MOD_ID = "examplemod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final int WATER_MONSTER_SUMMON_RITUAL_TICKS = 160;
    private static final int WATER_MONSTER_SUMMON_SOUL_SOUND_TICK = 64;
    private static final int WATER_MONSTER_SUMMON_PORTAL_SOUND_TICK = 124;
    private static final int WATER_MONSTER_ALTAR_BLOCKS = 6;
    private static final double WATER_MONSTER_SKY_CIRCLE_HEIGHT = 24.0;
    private static final double FULL_CIRCLE = Math.PI * 2.0;

    public static final Item EXAMPLE_ITEM = new Item(new Item.Settings().registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "example_item"))));
    public static final Item TNT_FISHING_ROD = new TntFishingRodItem(new Item.Settings()
            .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "tnt_fishing_rod")))
            .maxCount(1)
            .maxDamage(64)
            .rarity(Rarity.RARE)
    );
    public static final RegistryKey<ItemGroup> EXAMPLE_GROUP_KEY = RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "example_group"));

    private static final RegistryKey<EntityType<?>> WATER_MONSTER_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "water_monster"));
    private static final RegistryKey<EntityType<?>> NO_BLOCK_DAMAGE_TNT_KEY = RegistryKey.of(
            RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "no_block_damage_tnt"));

    public static final EntityType<WaterMonsterEntity> WATER_MONSTER = Registry.register(
            Registries.ENTITY_TYPE,
            WATER_MONSTER_KEY,
            EntityType.Builder.create(WaterMonsterEntity::new, SpawnGroup.MONSTER)
                    .dimensions(0.8f, 1.8f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(3)
                    .build(WATER_MONSTER_KEY)
    );

    public static final EntityType<NoBlockDamageTntEntity> NO_BLOCK_DAMAGE_TNT = Registry.register(
            Registries.ENTITY_TYPE,
            NO_BLOCK_DAMAGE_TNT_KEY,
            EntityType.Builder.create(NoBlockDamageTntEntity::new, SpawnGroup.MISC)
                    .dimensions(0.98f, 0.98f)
                    .maxTrackingRange(10)
                    .trackingTickInterval(10)
                    .build(NO_BLOCK_DAMAGE_TNT_KEY)
    );
    private static final List<PendingWaterMonsterSummon> PENDING_WATER_MONSTER_SUMMONS = new ArrayList<>();

    @Override
    public void onInitialize() {
        LOGGER.info("ExampleMod initializing!");

        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "example_item"), EXAMPLE_ITEM);
        Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "tnt_fishing_rod"), TNT_FISHING_ROD);
        Registry.register(Registries.ITEM_GROUP, EXAMPLE_GROUP_KEY,
                FabricItemGroup.builder()
                        .icon(() -> new ItemStack(EXAMPLE_ITEM))
                        .displayName(Text.translatable("itemGroup.examplemod.example_group"))
                        .build());
        ItemGroupEvents.modifyEntriesEvent(EXAMPLE_GROUP_KEY).register(entries -> {
            entries.add(EXAMPLE_ITEM);
            entries.add(TNT_FISHING_ROD);
        });
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            if (hand == Hand.MAIN_HAND) {
                WaterMonsterAltarProtection.alertDefenders(world, pos, player);
            }
            if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isOf(TNT_FISHING_ROD)) {
                return ActionResult.PASS;
            }
            if (!world.isClient()) {
                boolean breakBlocks = TntFishingRodItem.toggleBreakBlocks(player.getStackInHand(hand));
                player.sendMessage(TntFishingRodItem.modeText(breakBlocks), true);
            }
            player.swingHand(hand);
            return ActionResult.SUCCESS;
        });

        FabricDefaultAttributeRegistry.register(WATER_MONSTER, WaterMonsterEntity.createAttributes());
        UseBlockCallback.EVENT.register(ExampleMod::trySummonWaterMonster);
        ServerTickEvents.END_WORLD_TICK.register(ExampleMod::tickPendingWaterMonsterSummons);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) ->
                WaterMonsterAltarProtection.onPlayerBrokenAltarBlock(world, player, pos));

        LOGGER.info("ExampleMod initialized successfully!");
    }

    private static ActionResult trySummonWaterMonster(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || !player.getStackInHand(hand).isEmpty()) {
            return ActionResult.PASS;
        }

        BlockPos topCryingObsidian = hitResult.getBlockPos();
        List<BlockPos> altarBlocks = getWaterMonsterAltarBlocks(world, topCryingObsidian);
        if (altarBlocks.isEmpty()) {
            return ActionResult.PASS;
        }

        if (world instanceof ServerWorld serverWorld) {
            if (!hasPendingSummon(serverWorld, topCryingObsidian)) {
                PENDING_WATER_MONSTER_SUMMONS.add(new PendingWaterMonsterSummon(
                        serverWorld,
                        topCryingObsidian.toImmutable(),
                        altarBlocks.stream().map(BlockPos::toImmutable).toList(),
                        player.getYaw()
                ));
                serverWorld.playSound(null, topCryingObsidian, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 1.0f, 0.7f);
            }
        }

        player.swingHand(hand);
        return ActionResult.SUCCESS;
    }

    private static boolean hasPendingSummon(ServerWorld world, BlockPos topCryingObsidian) {
        for (PendingWaterMonsterSummon summon : PENDING_WATER_MONSTER_SUMMONS) {
            if (summon.matches(world, topCryingObsidian)) {
                return true;
            }
        }
        return false;
    }

    private static void tickPendingWaterMonsterSummons(ServerWorld world) {
        Iterator<PendingWaterMonsterSummon> iterator = PENDING_WATER_MONSTER_SUMMONS.iterator();
        while (iterator.hasNext()) {
            PendingWaterMonsterSummon summon = iterator.next();
            if (!summon.matchesWorld(world)) {
                continue;
            }

            if (!summon.isAltarStillValid(world)) {
                iterator.remove();
                continue;
            }

            if (summon.tickAndShouldSpawn(world)) {
                spawnWaterMonsterFromRitual(world, summon);
                iterator.remove();
            }
        }
    }

    private static void spawnWaterMonsterFromRitual(ServerWorld world, PendingWaterMonsterSummon summon) {
        BlockPos topCryingObsidian = summon.topCryingObsidian();
        emitRitualCompletionBurst(world, topCryingObsidian);

        WaterMonsterEntity waterMonster = new WaterMonsterEntity(WATER_MONSTER, world);
        waterMonster.setAltarBlocks(summon.altarBlocks());
        waterMonster.refreshPositionAndAngles(topCryingObsidian.getX() + 0.5, topCryingObsidian.getY() + 1.0, topCryingObsidian.getZ() + 0.5, summon.yaw(), 0.0f);
        world.spawnEntity(waterMonster);
        world.playSound(null, topCryingObsidian, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.3f, 0.7f);
        world.playSound(null, topCryingObsidian, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.HOSTILE, 1.4f, 0.8f);
        world.playSound(null, topCryingObsidian, SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.HOSTILE, 1.0f, 0.55f);
    }

    private static List<BlockPos> getWaterMonsterAltarBlocks(World world, BlockPos topCryingObsidian) {
        BlockPos center = topCryingObsidian.down();
        if (!world.getBlockState(topCryingObsidian).isOf(Blocks.CRYING_OBSIDIAN) || !world.getBlockState(center).isOf(Blocks.CRYING_OBSIDIAN)) {
            return List.of();
        }

        if (!hasCryingObsidianBaseCross(world, center)) {
            return List.of();
        }

        return cryingObsidianAltarBlocks(topCryingObsidian);
    }

    private static boolean hasCryingObsidianBaseCross(World world, BlockPos center) {
        return world.getBlockState(center.offset(Direction.NORTH)).isOf(Blocks.CRYING_OBSIDIAN)
                && world.getBlockState(center.offset(Direction.SOUTH)).isOf(Blocks.CRYING_OBSIDIAN)
                && world.getBlockState(center.offset(Direction.WEST)).isOf(Blocks.CRYING_OBSIDIAN)
                && world.getBlockState(center.offset(Direction.EAST)).isOf(Blocks.CRYING_OBSIDIAN);
    }

    private static List<BlockPos> cryingObsidianAltarBlocks(BlockPos topCryingObsidian) {
        BlockPos center = topCryingObsidian.down();
        return List.of(
                topCryingObsidian.toImmutable(),
                center.toImmutable(),
                center.offset(Direction.NORTH).toImmutable(),
                center.offset(Direction.SOUTH).toImmutable(),
                center.offset(Direction.WEST).toImmutable(),
                center.offset(Direction.EAST).toImmutable()
        );
    }

    private static void emitRitualParticles(ServerWorld world, BlockPos topCryingObsidian, List<BlockPos> altarBlocks, int ritualAge) {
        double centerX = topCryingObsidian.getX() + 0.5;
        double centerY = topCryingObsidian.getY() + 0.15;
        double centerZ = topCryingObsidian.getZ() + 0.5;
        double progress = (double) ritualAge / WATER_MONSTER_SUMMON_RITUAL_TICKS;

        emitAltarObsidianTears(world, altarBlocks, ritualAge);
        emitGroundSoulRing(world, centerX, centerY, centerZ, ritualAge, progress);
        emitEndermanPortalColumn(world, centerX, centerY, centerZ, ritualAge);
        emitSoulFireCrown(world, centerX, centerY, centerZ, ritualAge, progress);
        emitSkyCircleRitual(world, centerX, centerY, centerZ, altarBlocks, ritualAge, progress);

        if (ritualAge % 5 == 0) {
            spawnParticle(world, ParticleTypes.FLAME, centerX, centerY + 0.7, centerZ, 12, 0.55, 0.45, 0.55, 0.04);
            spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, centerX, centerY + 0.85, centerZ, 10, 0.5, 0.5, 0.5, 0.03);
        }
        if (ritualAge % 8 == 0) {
            spawnParticle(world, ParticleTypes.FALLING_OBSIDIAN_TEAR, centerX, centerY + 1.8, centerZ, 8, 0.55, 0.18, 0.55, 0.0);
            spawnParticle(world, ParticleTypes.PORTAL, centerX, centerY + 1.0, centerZ, 24, 0.75, 0.8, 0.75, 0.16);
        }
        if (ritualAge == 0) {
            world.playSound(null, topCryingObsidian, SoundEvents.BLOCK_PORTAL_TRIGGER, SoundCategory.HOSTILE, 1.0f, 0.75f);
        } else if (ritualAge == WATER_MONSTER_SUMMON_SOUL_SOUND_TICK) {
            world.playSound(null, topCryingObsidian, SoundEvents.BLOCK_SOUL_SAND_STEP, SoundCategory.HOSTILE, 1.0f, 0.65f);
        } else if (ritualAge == WATER_MONSTER_SUMMON_PORTAL_SOUND_TICK) {
            world.playSound(null, topCryingObsidian, SoundEvents.BLOCK_PORTAL_AMBIENT, SoundCategory.HOSTILE, 0.9f, 1.2f);
        }
    }

    private static void emitSkyCircleRitual(ServerWorld world, double centerX, double centerY, double centerZ, List<BlockPos> altarBlocks, int ritualAge, double progress) {
        double skyY = centerY + WATER_MONSTER_SKY_CIRCLE_HEIGHT;
        double circleProgress = smoothStep(Math.min(1.0, progress / 0.58));
        double radius = 0.35 + circleProgress * 5.25;

        emitSkyCircle(world, centerX, skyY, centerZ, ritualAge, radius);
        emitChinesePattern(world, centerX, skyY, centerZ, ritualAge, radius, circleProgress);

        if (progress > 0.28) {
            emitAltarBeams(world, centerX, skyY, centerZ, altarBlocks, ritualAge);
        }
        if (progress > 0.62) {
            emitRandomSkyBolts(world, centerX, skyY, centerZ, centerY, ritualAge, radius);
        }
    }

    private static void emitSkyCircle(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius) {
        int points = 56;
        for (int i = 0; i < points; i++) {
            double angle = FULL_CIRCLE * i / points + ritualAge * 0.015;
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, x, skyY, z, 1, 0.015, 0.015, 0.015, 0.0);
            if (i % 4 == 0) {
                spawnParticle(world, ParticleTypes.PORTAL, x, skyY, z, 1, 0.02, 0.02, 0.02, 0.02);
            }
        }
    }

    private static void emitChinesePattern(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius, double circleProgress) {
        if (circleProgress < 0.35) return;

        double patternRadius = Math.max(0.7, radius * 0.58);
        emitTaijiDots(world, centerX, skyY, centerZ, ritualAge, patternRadius);
        emitBaguaMarks(world, centerX, skyY, centerZ, ritualAge, radius * 0.78);
        emitHuiwenBorder(world, centerX, skyY, centerZ, ritualAge, radius * 0.92);
        emitTalismanStrokes(world, centerX, skyY, centerZ, ritualAge, radius * 0.44);
        emitSealMark(world, centerX, skyY, centerZ, ritualAge, radius * 0.24);
    }

    private static void emitTaijiDots(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius) {
        int points = 34;
        double spin = ritualAge * 0.035;
        for (int i = 0; i < points; i++) {
            double t = (double) i / (points - 1);
            double angle = spin + Math.PI * (t - 0.5);
            double waveRadius = radius * Math.sin(Math.PI * t);
            double x = centerX + Math.cos(angle) * waveRadius;
            double z = centerZ + Math.sin(angle) * waveRadius;
            spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, x, skyY + 0.02, z, 1, 0.01, 0.01, 0.01, 0.0);
            spawnParticle(world, ParticleTypes.PORTAL, centerX - (x - centerX), skyY + 0.02, centerZ - (z - centerZ), 1, 0.01, 0.01, 0.01, 0.02);
        }

        double eyeOffset = radius * 0.32;
        spawnParticle(world, ParticleTypes.SOUL, centerX + Math.cos(spin) * eyeOffset, skyY + 0.04, centerZ + Math.sin(spin) * eyeOffset, 3, 0.04, 0.01, 0.04, 0.0);
        spawnParticle(world, ParticleTypes.REVERSE_PORTAL, centerX - Math.cos(spin) * eyeOffset, skyY + 0.04, centerZ - Math.sin(spin) * eyeOffset, 3, 0.04, 0.01, 0.04, 0.02);
    }

    private static void emitBaguaMarks(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius) {
        double spin = -ritualAge * 0.01;
        for (int trigram = 0; trigram < 8; trigram++) {
            double angle = spin + FULL_CIRCLE * trigram / 8.0;
            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);
            double radialX = Math.cos(angle);
            double radialZ = Math.sin(angle);
            double markX = centerX + radialX * radius;
            double markZ = centerZ + radialZ * radius;

            for (int line = -1; line <= 1; line++) {
                double lineCenterX = markX + radialX * line * 0.18;
                double lineCenterZ = markZ + radialZ * line * 0.18;
                emitShortSkyLine(world, lineCenterX, skyY + 0.03, lineCenterZ, tangentX, tangentZ, 0.34, (trigram + line + 8) % 3 == 0);
            }
        }
    }

    private static void emitHuiwenBorder(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius) {
        int motifs = 16;
        double spin = ritualAge * 0.006;
        for (int motif = 0; motif < motifs; motif++) {
            double angle = spin + FULL_CIRCLE * motif / motifs;
            double nextAngle = spin + FULL_CIRCLE * (motif + 0.5) / motifs;
            double innerRadius = radius * 0.86;
            double outerRadius = radius;

            double x1 = centerX + Math.cos(angle) * innerRadius;
            double z1 = centerZ + Math.sin(angle) * innerRadius;
            double x2 = centerX + Math.cos(angle) * outerRadius;
            double z2 = centerZ + Math.sin(angle) * outerRadius;
            double x3 = centerX + Math.cos(nextAngle) * outerRadius;
            double z3 = centerZ + Math.sin(nextAngle) * outerRadius;
            double x4 = centerX + Math.cos(nextAngle) * innerRadius;
            double z4 = centerZ + Math.sin(nextAngle) * innerRadius;

            emitSkySegment(world, x1, skyY + 0.055, z1, x2, skyY + 0.055, z2, ParticleTypes.SOUL_FIRE_FLAME, 4);
            emitSkySegment(world, x2, skyY + 0.055, z2, x3, skyY + 0.055, z3, ParticleTypes.SOUL_FIRE_FLAME, 5);
            emitSkySegment(world, x3, skyY + 0.055, z3, x4, skyY + 0.055, z4, ParticleTypes.PORTAL, 4);
            if (motif % 2 == 0) {
                emitSkySegment(world, x4, skyY + 0.055, z4, x1, skyY + 0.055, z1, ParticleTypes.PORTAL, 5);
            }
        }
    }

    private static void emitTalismanStrokes(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double radius) {
        double spin = ritualAge * 0.012;
        for (int talisman = 0; talisman < 4; talisman++) {
            double angle = spin + FULL_CIRCLE * talisman / 4.0 + Math.PI / 4.0;
            double radialX = Math.cos(angle);
            double radialZ = Math.sin(angle);
            double tangentX = -Math.sin(angle);
            double tangentZ = Math.cos(angle);
            double baseX = centerX + radialX * radius;
            double baseZ = centerZ + radialZ * radius;

            emitSkySegment(world, baseX - radialX * 0.55, skyY + 0.08, baseZ - radialZ * 0.55, baseX + radialX * 0.55, skyY + 0.08, baseZ + radialZ * 0.55, ParticleTypes.SOUL_FIRE_FLAME, 8);
            emitSkySegment(world, baseX - tangentX * 0.32, skyY + 0.085, baseZ - tangentZ * 0.32, baseX + tangentX * 0.32, skyY + 0.085, baseZ + tangentZ * 0.32, ParticleTypes.PORTAL, 6);
            emitSkySegment(world, baseX + radialX * 0.15 - tangentX * 0.22, skyY + 0.09, baseZ + radialZ * 0.15 - tangentZ * 0.22, baseX + radialX * 0.45 + tangentX * 0.22, skyY + 0.09, baseZ + radialZ * 0.45 + tangentZ * 0.22, ParticleTypes.SOUL, 5);
            emitSkySegment(world, baseX - radialX * 0.38 + tangentX * 0.2, skyY + 0.095, baseZ - radialZ * 0.38 + tangentZ * 0.2, baseX - radialX * 0.05 - tangentX * 0.2, skyY + 0.095, baseZ - radialZ * 0.05 - tangentZ * 0.2, ParticleTypes.REVERSE_PORTAL, 5);
        }
    }

    private static void emitSealMark(ServerWorld world, double centerX, double skyY, double centerZ, int ritualAge, double halfSize) {
        double spin = -ritualAge * 0.018;
        double cos = Math.cos(spin);
        double sin = Math.sin(spin);
        double[][] corners = new double[][] {
                {-halfSize, -halfSize},
                {halfSize, -halfSize},
                {halfSize, halfSize},
                {-halfSize, halfSize}
        };

        for (int i = 0; i < corners.length; i++) {
            double[] a = corners[i];
            double[] b = corners[(i + 1) % corners.length];
            double ax = centerX + a[0] * cos - a[1] * sin;
            double az = centerZ + a[0] * sin + a[1] * cos;
            double bx = centerX + b[0] * cos - b[1] * sin;
            double bz = centerZ + b[0] * sin + b[1] * cos;
            emitSkySegment(world, ax, skyY + 0.11, az, bx, skyY + 0.11, bz, ParticleTypes.FLAME, 8);
        }

        emitRotatedSealStroke(world, centerX, skyY + 0.12, centerZ, cos, sin, -halfSize * 0.45, 0.0, halfSize * 0.45, 0.0);
        emitRotatedSealStroke(world, centerX, skyY + 0.12, centerZ, cos, sin, 0.0, -halfSize * 0.45, 0.0, halfSize * 0.45);
        emitRotatedSealStroke(world, centerX, skyY + 0.12, centerZ, cos, sin, -halfSize * 0.32, -halfSize * 0.28, halfSize * 0.32, halfSize * 0.28);
    }

    private static void emitRotatedSealStroke(ServerWorld world, double centerX, double y, double centerZ, double cos, double sin, double ax, double az, double bx, double bz) {
        double startX = centerX + ax * cos - az * sin;
        double startZ = centerZ + ax * sin + az * cos;
        double endX = centerX + bx * cos - bz * sin;
        double endZ = centerZ + bx * sin + bz * cos;
        emitSkySegment(world, startX, y, startZ, endX, y, endZ, ParticleTypes.FLAME, 6);
    }

    private static void emitSkySegment(ServerWorld world, double startX, double startY, double startZ, double endX, double endY, double endZ, ParticleEffect particle, int steps) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            spawnParticle(world, particle, lerp(startX, endX, t), lerp(startY, endY, t), lerp(startZ, endZ, t), 1, 0.004, 0.004, 0.004, 0.0);
        }
    }

    private static void emitShortSkyLine(ServerWorld world, double centerX, double y, double centerZ, double dirX, double dirZ, double halfLength, boolean broken) {
        int points = 5;
        for (int i = -points; i <= points; i++) {
            if (broken && Math.abs(i) <= 1) continue;
            double offset = halfLength * i / points;
            spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, centerX + dirX * offset, y, centerZ + dirZ * offset, 1, 0.004, 0.004, 0.004, 0.0);
        }
    }

    private static void emitAltarBeams(ServerWorld world, double centerX, double skyY, double centerZ, List<BlockPos> altarBlocks, int ritualAge) {
        for (BlockPos block : altarBlocks) {
            double targetX = block.getX() + 0.5;
            double targetY = block.getY() + 1.05;
            double targetZ = block.getZ() + 0.5;
            ParticleEffect beamParticle = ritualAge % 3 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.PORTAL;
            emitBeam(world, targetX, skyY - 0.35, targetZ, targetX, targetY, targetZ, beamParticle, 18, 0.012);
            if (ritualAge % 6 == 0) {
                spawnParticle(world, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, targetX, targetY + 0.25, targetZ, 4, 0.08, 0.05, 0.08, 0.0);
            }
        }

        emitBeam(world, centerX, skyY, centerZ, centerX, skyY - WATER_MONSTER_SKY_CIRCLE_HEIGHT + 1.4, centerZ, ParticleTypes.SOUL_FIRE_FLAME, 28, 0.018);
    }

    private static void emitRandomSkyBolts(ServerWorld world, double centerX, double skyY, double centerZ, double altarY, int ritualAge, double radius) {
        for (int bolt = 0; bolt < 3; bolt++) {
            double seed = ritualAge * 12.9898 + bolt * 78.233;
            double angle = FULL_CIRCLE * pseudoRandom(seed);
            double distance = radius * Math.sqrt(pseudoRandom(seed + 19.19)) * 0.82;
            double endX = centerX + Math.cos(angle) * distance;
            double endZ = centerZ + Math.sin(angle) * distance;
            double endY = altarY + 1.0 + pseudoRandom(seed + 37.37) * 2.8;
            ParticleEffect particle = bolt == 0 ? ParticleTypes.FLAME : (bolt == 1 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.PORTAL);
            emitBeam(world, centerX, skyY - 0.1, centerZ, endX, endY, endZ, particle, 16, 0.025);
        }
    }

    private static void emitBeam(ServerWorld world, double startX, double startY, double startZ, double endX, double endY, double endZ, ParticleEffect particle, int steps, double jitter) {
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = lerp(startX, endX, t);
            double y = lerp(startY, endY, t);
            double z = lerp(startZ, endZ, t);
            spawnParticle(world, particle, x, y, z, 1, jitter, jitter, jitter, 0.0);
            if (i % 5 == 0) {
                spawnParticle(world, ParticleTypes.SOUL, x, y, z, 1, jitter * 0.5, jitter * 0.5, jitter * 0.5, 0.0);
            }
        }
    }

    private static double smoothStep(double value) {
        double clamped = Math.max(0.0, Math.min(1.0, value));
        return clamped * clamped * (3.0 - 2.0 * clamped);
    }

    private static double pseudoRandom(double seed) {
        double value = Math.sin(seed) * 43758.5453123;
        return value - Math.floor(value);
    }

    private static double lerp(double start, double end, double progress) {
        return start + (end - start) * progress;
    }

    private static void emitAltarObsidianTears(ServerWorld world, List<BlockPos> altarBlocks, int ritualAge) {
        for (BlockPos pos : altarBlocks) {
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 1.05;
            double z = pos.getZ() + 0.5;
            spawnParticle(world, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, x, y, z, 2, 0.28, 0.02, 0.28, 0.0);
            if (ritualAge % 10 == 0) {
                spawnParticle(world, ParticleTypes.FALLING_OBSIDIAN_TEAR, x, y + 0.55, z, 3, 0.2, 0.12, 0.2, 0.0);
            }
        }
    }

    private static void emitGroundSoulRing(ServerWorld world, double centerX, double centerY, double centerZ, int ritualAge, double progress) {
        int ringPoints = 22;
        double ringRadius = 0.7 + progress * 2.25;
        double ringY = centerY + 0.03 + Math.sin(ritualAge * 0.18) * 0.08;
        for (int i = 0; i < ringPoints; i++) {
            double angle = (FULL_CIRCLE * i / ringPoints) + ritualAge * 0.12;
            double x = centerX + Math.cos(angle) * ringRadius;
            double z = centerZ + Math.sin(angle) * ringRadius;
            spawnParticle(world, ParticleTypes.SOUL, x, ringY, z, 1, 0.03, 0.02, 0.03, 0.01);
            if (i % 3 == 0) {
                spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, x, ringY + 0.12, z, 1, 0.015, 0.015, 0.015, 0.005);
            }
        }
    }

    private static void emitEndermanPortalColumn(ServerWorld world, double centerX, double centerY, double centerZ, int ritualAge) {
        for (int i = 0; i < 11; i++) {
            double spiralAge = ritualAge * 0.28 + i * 0.72;
            double radius = 0.42 + i * 0.12;
            double y = centerY + 0.25 + i * 0.2;
            double x = centerX + Math.cos(spiralAge) * radius;
            double z = centerZ + Math.sin(spiralAge) * radius;
            spawnParticle(world, ParticleTypes.PORTAL, x, y, z, 2, 0.05, 0.07, 0.05, 0.08);
            spawnParticle(world, ParticleTypes.REVERSE_PORTAL, centerX, y + 0.1, centerZ, 1, radius * 0.25, 0.04, radius * 0.25, 0.03);
            spawnParticle(world, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, x, y + 0.2, z, 1, 0.04, 0.03, 0.04, 0.0);
        }
    }

    private static void emitSoulFireCrown(ServerWorld world, double centerX, double centerY, double centerZ, int ritualAge, double progress) {
        int crownPoints = 10;
        double crownRadius = 0.35 + progress * 0.8;
        double crownY = centerY + 1.25 + progress * 1.25;
        for (int i = 0; i < crownPoints; i++) {
            double angle = -ritualAge * 0.2 + FULL_CIRCLE * i / crownPoints;
            double x = centerX + Math.cos(angle) * crownRadius;
            double z = centerZ + Math.sin(angle) * crownRadius;
            spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, x, crownY, z, 1, 0.02, 0.05, 0.02, 0.01);
            if (i % 2 == 0) {
                spawnParticle(world, ParticleTypes.FLAME, x, crownY - 0.18, z, 1, 0.02, 0.04, 0.02, 0.01);
            }
        }
    }

    private static void emitRitualCompletionBurst(ServerWorld world, BlockPos topCryingObsidian) {
        double centerX = topCryingObsidian.getX() + 0.5;
        double centerY = topCryingObsidian.getY() + 1.0;
        double centerZ = topCryingObsidian.getZ() + 0.5;
        spawnParticle(world, ParticleTypes.PORTAL, centerX, centerY + 0.2, centerZ, 72, 0.95, 1.2, 0.95, 0.22);
        spawnParticle(world, ParticleTypes.REVERSE_PORTAL, centerX, centerY + 0.8, centerZ, 38, 0.75, 0.85, 0.75, 0.08);
        spawnParticle(world, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, centerX, centerY + 0.75, centerZ, 24, 0.55, 0.75, 0.55, 0.0);
        spawnParticle(world, ParticleTypes.FALLING_OBSIDIAN_TEAR, centerX, centerY + 1.6, centerZ, 18, 0.6, 0.2, 0.6, 0.0);
        spawnParticle(world, ParticleTypes.SOUL, centerX, centerY + 0.1, centerZ, 36, 0.85, 0.5, 0.85, 0.05);
        spawnParticle(world, ParticleTypes.SOUL_FIRE_FLAME, centerX, centerY + 0.45, centerZ, 32, 0.75, 0.65, 0.75, 0.05);
        spawnParticle(world, ParticleTypes.FLAME, centerX, centerY + 0.3, centerZ, 28, 0.6, 0.55, 0.6, 0.06);
        spawnParticle(world, ParticleTypes.EXPLOSION_EMITTER, centerX, centerY + 0.35, centerZ, 1, 0.0, 0.0, 0.0, 0.0);
    }

    private static void spawnParticle(ServerWorld world, ParticleEffect particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed) {
        world.spawnParticles(particle, x, y, z, count, deltaX, deltaY, deltaZ, speed);
    }

    private static final class PendingWaterMonsterSummon {
        private final RegistryKey<World> worldKey;
        private final BlockPos topCryingObsidian;
        private final List<BlockPos> altarBlocks;
        private final float yaw;
        private int ritualAge;

        private PendingWaterMonsterSummon(ServerWorld world, BlockPos topCryingObsidian, List<BlockPos> altarBlocks, float yaw) {
            this.worldKey = world.getRegistryKey();
            this.topCryingObsidian = topCryingObsidian;
            this.altarBlocks = altarBlocks;
            this.yaw = yaw;
        }

        private boolean matchesWorld(ServerWorld world) {
            return world.getRegistryKey().equals(worldKey);
        }

        private boolean matches(ServerWorld world, BlockPos pos) {
            return matchesWorld(world) && topCryingObsidian.equals(pos);
        }

        private boolean isAltarStillValid(ServerWorld world) {
            return getWaterMonsterAltarBlocks(world, topCryingObsidian).size() == WATER_MONSTER_ALTAR_BLOCKS;
        }

        private boolean tickAndShouldSpawn(ServerWorld world) {
            emitRitualParticles(world, topCryingObsidian, altarBlocks, ritualAge);
            ritualAge++;
            return ritualAge >= WATER_MONSTER_SUMMON_RITUAL_TICKS;
        }

        private BlockPos topCryingObsidian() {
            return topCryingObsidian;
        }

        private List<BlockPos> altarBlocks() {
            return altarBlocks;
        }

        private float yaw() {
            return yaw;
        }
    }
}
