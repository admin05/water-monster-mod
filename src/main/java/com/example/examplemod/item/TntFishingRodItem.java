package com.example.examplemod.item;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.entity.NoBlockDamageTntEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.player.PlayerEntity;

public class TntFishingRodItem extends Item {
    private static final String BREAK_BLOCKS_KEY = "BreakBlocks";

    public static final int RING_COUNT = 5;
    public static final int INNER_RING_TNT = 16;
    public static final double RING_SPACING = 2.25;
    public static final double SUMMON_HEIGHT = 12.0;

    public TntFishingRodItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        if (world instanceof ServerWorld serverWorld) {
            ItemStack stack = user.getStackInHand(hand);
            Vec3d center = getTargetCenter(user);
            summonTntRing(serverWorld, user, center, breaksBlocks(stack));
            if (user instanceof ServerPlayerEntity serverPlayer) {
                stack.damage(1, serverWorld, serverPlayer, item -> {});
            }
        }
        user.swingHand(hand);
        return ActionResult.SUCCESS;
    }

    public static void summonTntRing(ServerWorld world, LivingEntity user, Vec3d center, boolean breakBlocks) {
        double y = center.y + SUMMON_HEIGHT;
        for (int ring = 0; ring < RING_COUNT; ring++) {
            double radius = RING_SPACING * (ring + 1);
            int tntInRing = INNER_RING_TNT + ring * 8;
            double ringOffset = Math.PI * ring / tntInRing;

            for (int i = 0; i < tntInRing; i++) {
                double angle = Math.PI * 2.0 * i / tntInRing + ringOffset;
                double x = center.x + Math.cos(angle) * radius;
                double z = center.z + Math.sin(angle) * radius;

                NoBlockDamageTntEntity tnt = new NoBlockDamageTntEntity(ExampleMod.NO_BLOCK_DAMAGE_TNT, world);
                tnt.setPosition(x, y, z);
                tnt.setOwner(user);
                tnt.setBreakBlocks(breakBlocks);
                tnt.setFuse(85 + ring * 6 + world.random.nextInt(22));
                tnt.setVelocity(
                        (center.x - x) * 0.01,
                        -0.22 - ring * 0.01,
                        (center.z - z) * 0.01
                );
                world.spawnEntity(tnt);
            }
        }
    }

    public static boolean toggleBreakBlocks(ItemStack stack) {
        boolean next = !breaksBlocks(stack);
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(BREAK_BLOCKS_KEY, next));
        return next;
    }

    public static boolean breaksBlocks(ItemStack stack) {
        NbtComponent component = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = component.copyNbt();
        return nbt.getBoolean(BREAK_BLOCKS_KEY, false);
    }

    public static Text modeText(boolean breakBlocks) {
        return Text.literal("TNT Fishing Rod: " + (breakBlocks ? "break blocks" : "protect blocks"));
    }

    private static Vec3d getTargetCenter(PlayerEntity user) {
        HitResult hit = user.raycast(24.0, 0.0f, false);
        if (hit.getType() != HitResult.Type.MISS) {
            return hit.getPos();
        }
        return user.getEyePos().add(user.getRotationVec(1.0f).multiply(12.0));
    }
}
