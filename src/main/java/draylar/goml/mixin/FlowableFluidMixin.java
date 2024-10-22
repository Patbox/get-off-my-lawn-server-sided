package draylar.goml.mixin;

import draylar.goml.api.ClaimUtils;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FlowableFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FlowableFluid.class)
public class FlowableFluidMixin {
    @Inject(method = "flow", at = @At("HEAD"), cancellable = true)
    private void applyFluidFlowEvent(WorldAccess worldAccess, BlockPos pos, BlockState state, Direction direction, FluidState fluidState, CallbackInfo ci) {
        if (!(worldAccess instanceof ServerWorld world)) {
            return;
        }

        if (!ClaimUtils.canFluidFlow(world, pos.offset(direction, -1), pos)) {
            ci.cancel();
        }
    }
}
