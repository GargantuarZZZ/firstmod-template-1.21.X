package net.garz.firstmod.mixin;

import net.garz.firstmod.CropGrowthHandler;
import net.minecraft.block.CropBlock;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CropBlock.class)
public class CropBlockMixin {

    @Redirect(
            method = "randomTick(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/random/Random;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/block/CropBlock;applyGrowth(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
            )
    )
    private void redirectApplyGrowth(CropBlock self, World world, BlockPos pos, BlockState state) {
        double multiplier = CropGrowthHandler.getMultiplier(world, pos.getX(), pos.getY(), pos.getZ());
        if (multiplier <= 0) {
            return;
        }

        int growthAmount = ((CropBlockInvoker)(Object) self).callGetGrowthAmount(world);
        double baseChance = 1.0 / (growthAmount + 1);
        double finalChance = baseChance * multiplier;
        if (finalChance > 1.0) {
            finalChance = 1.0;
        }

        if (world.getRandom().nextDouble() < finalChance) {
            self.applyGrowth(world, pos, state);
        }
    }

    private interface CropBlockInvoker {
        @Invoker("getGrowthAmount")
        int callGetGrowthAmount(World world);
    }
}
