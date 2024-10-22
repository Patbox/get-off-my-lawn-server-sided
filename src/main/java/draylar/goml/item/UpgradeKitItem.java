package draylar.goml.item;

import com.jamieswhiteshirt.rtree3i.Box;
import com.jamieswhiteshirt.rtree3i.Entry;
import com.jamieswhiteshirt.rtree3i.Selection;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.Claim;
import draylar.goml.api.ClaimBox;
import draylar.goml.api.ClaimUtils;
import draylar.goml.api.event.ClaimEvents;
import draylar.goml.block.ClaimAnchorBlock;
import draylar.goml.block.entity.ClaimAnchorBlockEntity;
import eu.pb4.polymer.core.api.item.PolymerItem;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import xyz.nucleoid.packettweaker.PacketContext;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class UpgradeKitItem extends Item implements PolymerItem {

    private final ClaimAnchorBlock from;
    private final ClaimAnchorBlock to;
    private final Item clientItem;

    public UpgradeKitItem(Settings settings, ClaimAnchorBlock from, ClaimAnchorBlock to, Item display) {
        super(settings);
        this.clientItem = display;

        this.from = from;
        this.to = to;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if(context == null || context.getPlayer() == null || context.getWorld().isClient) {
            return ActionResult.PASS;
        }

        BlockPos pos = context.getBlockPos();
        World world = context.getWorld();
        BlockState block = world.getBlockState(pos);

        if(block.getBlock().equals(from)) {
            // get claims at block position
            Selection<Entry<ClaimBox, Claim>> claimsFound = GetOffMyLawn.CLAIM.get(world).getClaims().entries(box ->
                    box.contains(Box.create(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1))
            );

            if(!claimsFound.isEmpty()) {
                boolean noPermission = claimsFound.anyMatch((Entry<ClaimBox, Claim> boxInfo) -> !boxInfo.getValue().getOwners().contains(context.getPlayer().getUuid()));

                // get claim at location
                AtomicReference<Entry<ClaimBox, Claim>> currentClaim = new AtomicReference<>();
                claimsFound.forEach(claim -> {
                    if (claim.getValue().getOrigin().equals(pos) && claim.getValue().getOwners().contains(context.getPlayer().getUuid())) {
                        currentClaim.set(claim);
                    }
                });


                // if we have permission
                if(!noPermission) {
                    var radius = to.getRadius();
                    var newBox = ClaimUtils.createClaimBox(pos, radius);

                    // if we don't overlap with another claim
                    var claims = ClaimUtils.getClaimsInBox(world, newBox.rtree3iBox(), currentClaim.get().getKey().toBox());
                    if (claims.isEmpty()) {
                        var claimInfo = currentClaim.get().getValue();
                        var oldSize = claimInfo.getClaimBox();

                        // remove claim
                        GetOffMyLawn.CLAIM.get(world).remove(claimInfo);

                        // set block
                        BlockEntity oldBE = world.getBlockEntity(pos);
                        world.setBlockState(pos, to.getDefaultState());

                        if (this.to.asItem() != null) {
                            claimInfo.internal_setIcon(this.to.asItem().getDefaultStack());
                        }
                        claimInfo.internal_setType(this.to);

                        claimInfo.internal_setClaimBox(newBox);
                        if (world instanceof ServerWorld world1) {
                            claimInfo.internal_updateChunkCount(world1);
                        }
                        claimInfo.internal_setWorld(currentClaim.get().getValue().getWorld());
                        GetOffMyLawn.CLAIM.get(world).add(claimInfo);

                        // decrement stack
                        if(!context.getPlayer().isCreative() && !context.getPlayer().isSpectator()) {
                            context.getStack().decrement(1);
                        }

                        // transfer BE data
                        BlockEntity newBE = world.getBlockEntity(pos);
                        if(oldBE instanceof ClaimAnchorBlockEntity && newBE instanceof ClaimAnchorBlockEntity) {
                            ((ClaimAnchorBlockEntity) newBE).from(((ClaimAnchorBlockEntity) oldBE));
                        }

                        ClaimEvents.CLAIM_RESIZED.invoker().onResizeEvent(claimInfo, oldSize, newBox);
                        return ActionResult.SUCCESS;
                    } else {
                        var list = Text.literal("");

                        claims.forEach((c) -> {
                            var box = c.getKey().toBox();

                            list.append(Text.literal("[").formatted(Formatting.GRAY)
                                    .append(Text.literal(box.x1() + ", " + box.y1() + ", " + box.z1()).formatted(Formatting.WHITE))
                                    .append(" | ")
                                    .append(Text.literal(box.x2() + ", " + box.y2() + ", " + box.z2()).formatted(Formatting.WHITE))
                                    .append("] ")
                            );
                        });

                        context.getPlayer().sendMessage(GetOffMyLawn.CONFIG.prefix(Text.translatable("text.goml.cant_upgrade_claim.collides_with", list).formatted(Formatting.RED)), false);
                    }
                }
            }
        }

        return ActionResult.PASS;
    }


    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        if(tooltip == null) {
            return;
        }

        tooltip.add(Text.translatable(from.getTranslationKey()).append(" -> ").append(Text.translatable(to.getTranslationKey())).formatted(Formatting.GRAY));
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public Item getPolymerItem(ItemStack itemStack, PacketContext context) {
        return this.clientItem;
    }

    @Override
    public @Nullable Identifier getPolymerItemModel(ItemStack stack, PacketContext context) {
        return null;
    }
}
