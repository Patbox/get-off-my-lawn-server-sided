package draylar.goml.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import draylar.goml.api.ClaimUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.DispenserBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DispenserBlock.class)
public class DispenserBlockMixin {

    @Shadow @Final public static EnumProperty<Direction> FACING;

    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void safeSetBlock(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        var nextPos = pos.offset(state.get(FACING));

        if (!ClaimUtils.hasMatchingClaims(world, nextPos, pos)) {
            ci.cancel();
        }
    }
}
