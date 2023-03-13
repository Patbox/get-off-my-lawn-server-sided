package draylar.goml.item;

import com.jamieswhiteshirt.rtree3i.Box;
import draylar.goml.GetOffMyLawn;
import draylar.goml.api.ClaimUtils;
import draylar.goml.block.ClaimAnchorBlock;
import me.lucko.fabric.api.permissions.v0.Options;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.function.Consumer;

public class ClaimAnchorBlockItem extends TooltippedBlockItem {

    private final ClaimAnchorBlock claimBlock;

    public ClaimAnchorBlockItem(ClaimAnchorBlock block, Settings settings, int lines) {
        super(block, settings, lines);
        this.claimBlock = block;
    }

    @Override
    public void addLines(Consumer<Text> textConsumer) {
        super.addLines(textConsumer);
        textConsumer.accept(Text.translatable("text.goml.radius",
                Text.literal("" + this.claimBlock.getRadius()).formatted(Formatting.WHITE)
        ).formatted(Formatting.YELLOW));
    }

    @Override
    protected boolean canPlace(ItemPlacementContext context, BlockState state) {
        if (context.getWorld().isClient) {
            return true;
        }

        var pos = context.getBlockPos();
        var radius = this.claimBlock.getRadius();

        if (radius <= 0 && !ClaimUtils.isInAdminMode(context.getPlayer())) {
            context.getPlayer().sendMessage(GetOffMyLawn.CONFIG.prefix(Text.translatable("text.goml.cant_place_claim.admin_only").formatted(Formatting.RED)), false);
            return false;
        }

        radius = Math.max(radius, 1);
        var checkBox = ClaimUtils.createClaimBox(pos, radius);

        if (!ClaimUtils.isInAdminMode(context.getPlayer())) {
            var count = ClaimUtils.getClaimsOwnedBy(context.getWorld(), context.getPlayer().getUuid()).count();

            int maxCount;
            var allowedCount = Options.get(context.getPlayer(), "goml.claim_limit");
            var allowedCount2 = Options.get(context.getPlayer(), "goml.claim_limit." + context.getWorld().getRegistryKey().getValue().toString());

            if (allowedCount2.isPresent()) {
                try {
                    maxCount = Integer.parseInt(allowedCount2.get());
                } catch (Throwable t) {
                    maxCount = GetOffMyLawn.CONFIG.maxClaimsPerPlayer;
                }
            } else if (allowedCount.isPresent()) {
                try {
                    maxCount = Integer.parseInt(allowedCount.get());
                } catch (Throwable t) {
                    maxCount = GetOffMyLawn.CONFIG.maxClaimsPerPlayer;
                }
            } else {
                maxCount = GetOffMyLawn.CONFIG.maxClaimsPerPlayer;
            }

            if (maxCount != -1
                    && count >= maxCount
            ) {
                context.getPlayer().sendMessage(GetOffMyLawn.CONFIG.prefix(Text.translatable("text.goml.cant_place_claim.max_count_reached", count, GetOffMyLawn.CONFIG.maxClaimsPerPlayer).formatted(Formatting.RED)), false);
                return false;
            }

            if (GetOffMyLawn.CONFIG.isBlacklisted(context.getWorld(), checkBox.toBox())) {
                context.getPlayer().sendMessage(GetOffMyLawn.CONFIG.prefix(Text.translatable("text.goml.cant_place_claim.blacklisted_area", context.getWorld().getRegistryKey().getValue().toString(), context.getBlockPos().toShortString()).formatted(Formatting.RED)), false);
                return false;
            }
        }


        var claims = ClaimUtils.getClaimsInBox(context.getWorld(), checkBox.rtree3iBox());
        if (GetOffMyLawn.CONFIG.allowClaimOverlappingIfSameOwner) {
            claims = claims.filter(x -> !x.getValue().isOwner(context.getPlayer()) || x.getKey().toBox().equals(checkBox.toBox()));
        }

        if (claims.isNotEmpty()) {
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

            context.getPlayer().sendMessage(GetOffMyLawn.CONFIG.prefix(Text.translatable("text.goml.cant_place_claim.collides_with", list).formatted(Formatting.RED)), false);
            return false;
        }


        return super.canPlace(context, state);


    }
}
