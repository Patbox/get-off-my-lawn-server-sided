package draylar.goml.mixin;

import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * UseBlockCallback doesn't seem to cover buckets well.
 * This mixin serves as an extra protection layer against client desync when using buckets in claims you don't own.
 */
@Mixin(BucketItem.class)
public class BucketItemMixin extends Item {

    @Shadow @Final private Fluid fluid;

    public BucketItemMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void goml_preventBucketUsageInClaims(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        if (world.isClient) {
            return;
        }

        HitResult hitResult = raycast(world, user, this.fluid == Fluids.EMPTY ? RaycastContext.FluidHandling.SOURCE_ONLY : RaycastContext.FluidHandling.NONE);
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos blockPos = blockHitResult.getBlockPos();

        Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(world, blockPos);

        if (!claimsFound.isEmpty()) {
            boolean noPermission = claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !boxInfo.getValue().hasPermission(user));

            if(noPermission) {
                user.sendMessage(Text.literal("This block is protected by a claim."), true);
                cir.setReturnValue(ActionResult.FAIL);
            }
        }
    }
}