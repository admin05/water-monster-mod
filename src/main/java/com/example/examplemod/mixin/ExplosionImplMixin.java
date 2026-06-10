package com.example.examplemod.mixin;

import com.example.examplemod.entity.WaterMonsterAltarProtection;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.explosion.ExplosionImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ExplosionImpl.class)
public class ExplosionImplMixin {
    @Shadow
    @Final
    private ServerWorld world;

    @Inject(method = "destroyBlocks", at = @At("HEAD"))
    private void examplemod$protectWaterMonsterAltar(List<BlockPos> positions, CallbackInfo ci) {
        WaterMonsterAltarProtection.removeProtectedBlocks(world, positions);
    }
}
