package draylar.goml.mixin;

import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin which prevents TNT primed by player A from going off in player B's claim.
 */
@Mixin(TntEntity.class)
public abstract class TntEntityMixin extends Entity {

    @Shadow private LivingEntity causingEntity;

    public TntEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @Inject(at = @At("HEAD"), method = "explode", cancellable = true)
    private void goml_attemptExplosion(CallbackInfo ci) {
        if (getWorld().isClient) {
            return;
        }
        if (causingEntity instanceof PlayerEntity) {
            Selection<Entry<ClaimBox, Claim>> claimsFound = ClaimUtils.getClaimsAt(getWorld(), getBlockPos());

            if (!claimsFound.isEmpty()) {
                boolean noPermission = claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !boxInfo.getValue().hasPermission((PlayerEntity) causingEntity));

                if(noPermission) {
                    ci.cancel();
                }
            }
        }
    }
}
