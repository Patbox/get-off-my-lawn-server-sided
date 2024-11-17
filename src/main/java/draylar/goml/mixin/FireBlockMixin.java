package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.FireBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireBlock.class)
public class FireBlockMixin {
    @Inject(method = "trySpreadingFire", at = @At("HEAD"), cancellable = true)
    private void goml_preventFire(World world, BlockPos pos, int spreadFactor, Random random, int currentAge, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        if (!ClaimUtils.canFireDestroy(world, pos)) {
            ci.cancel();
        }
    }

    @Inject(method = "scheduledTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/world/ServerWorld;scheduleBlockTick(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;I)V", shift = At.Shift.AFTER), cancellable = true)
    private void goml_preventFire2(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (!ClaimUtils.canFireDestroy(world, pos)) {
            ci.cancel();
        }
    }
}
