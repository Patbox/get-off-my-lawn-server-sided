package draylar.goml.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import draylar.goml.api.ClaimUtils;
import net.minecraft.block.piston.PistonHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Mixin(PistonHandler.class)
public class PistonHandlerMixin {
    @Shadow @Final private List<BlockPos> movedBlocks;
    @Shadow @Final private List<BlockPos> brokenBlocks;
    @Shadow @Final private World world;
    @Shadow @Final private Direction motionDirection;
    @Unique
    private boolean claimsEmpty;
    private HashSet<UUID> trusted;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void storeClaimInfo(World world, BlockPos pos, Direction dir, boolean retracted, CallbackInfo ci) {
        if (world.isClient) {
            return;
        }
        var claims = ClaimUtils.getClaimsAt(world, pos);
        this.claimsEmpty = claims.isEmpty();
        this.trusted = new HashSet<>();
        claims.forEach(x -> {
            this.trusted.addAll(x.getValue().getOwners());
            this.trusted.addAll(x.getValue().getTrusted());
        });
    }

    @ModifyReturnValue(method = "calculatePush", at = @At("RETURN"))
    private boolean preventMovement(boolean value) {
        if (world.isClient) {
            return value;
        }
        if (value) {
            if (!checkClaims(this.movedBlocks) || !checkClaims(this.brokenBlocks)) {
                this.movedBlocks.clear();
                this.brokenBlocks.clear();
                return false;
            }
            return true;
        }

        return false;
    }

    @Unique
    private boolean checkClaims(List<BlockPos> blocks) {
        for (var pos : blocks) {
            var claims = ClaimUtils.getClaimsAt(this.world, pos);

            boolean firstFound = true;

            if (claims.isEmpty() && this.claimsEmpty) {
                firstFound = false;
            }

            if (firstFound && claims.noneMatch(x -> x.getValue().hasPermission(this.trusted))) {
                return false;
            }

            var mut = new BlockPos.Mutable();
            claims = ClaimUtils.getClaimsAt(this.world, mut.set(pos).move(this.motionDirection));
            if (claims.isEmpty() && this.claimsEmpty) {
                continue;
            }

            if (claims.noneMatch(x -> x.getValue().hasPermission(this.trusted))) {
                return false;
            }
        }
        return true;
    }
}
