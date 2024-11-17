package draylar.goml.mixin;

import draylar.goml.block.ClaimAnchorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PistonBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBlock.class)
public abstract class PistonBlockMixin {

    @Inject(at = @At("HEAD"), method = "isMovable", cancellable = true)
    private static void goml_isMoveable(BlockState state, World world, BlockPos pos, Direction motionDir, boolean canBreak, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
        if (world.isClient) {
            return;
        }

        if(state.getBlock() instanceof ClaimAnchorBlock) {
            cir.setReturnValue(false);
        }
    }
}
