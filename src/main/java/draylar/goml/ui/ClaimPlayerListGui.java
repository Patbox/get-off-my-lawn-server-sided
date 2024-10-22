package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.api.Claim;
import draylar.goml.api.group.PlayerGroup;
import draylar.goml.registry.GOMLTextures;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@ApiStatus.Internal
public class ClaimPlayerListGui extends GenericPlayerListGui {
    private final Claim claim;
    private final boolean isAdmin;

    private final boolean canModifyOwners;
    private final boolean canModifyTrusted;
    private final List<PlayerGroup> groups = new ArrayList<>();

    public ClaimPlayerListGui(ServerPlayerEntity player, Claim claim, boolean canModifyTrusted, boolean canModifyOwners, boolean isAdmin, @Nullable Runnable onClose) {
        super(player, onClose);
        this.claim = claim;
        this.setTitle(Text.translatable("text.goml.gui.player_list.title"));
        this.canModifyOwners = canModifyOwners;
        this.canModifyTrusted = canModifyTrusted;
        this.isAdmin = isAdmin;
        this.updateDisplay();
        this.open();
    }

    public static void open(ServerPlayerEntity player, Claim claim, boolean admin, @Nullable Runnable onClose) {
        new ClaimPlayerListGui(player, claim, claim.isOwner(player) || admin, admin, admin, onClose);
    }

    @Override
    protected DisplayElement getElement(int id) {
        return id < this.groups.size() ? this.getGroupElement(id) : super.getElement(id - this.groups.size());
    }

    private DisplayElement getGroupElement(int id) {
        var group = this.groups.get(id);

        var builder = GuiElementBuilder.from(group.icon())

                .setName(Text.empty().append(group.selfDisplayName()).append(Text.literal(" (").formatted(Formatting.DARK_GRAY)
                        .append(Text.empty().append(group.provider().getName()).setStyle(Style.EMPTY.withColor(0x45abff))
                        .append(Text.literal(")").formatted(Formatting.DARK_GRAY)))
                        )
                ).hideDefaultTooltip();


        if (this.canModifyTrusted) {
            builder.addLoreLine(Text.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback((x, y, z) -> {
                playClickSound(player);
                this.claim.untrust(group);
                this.updateDisplay();
            });
        }

        return DisplayElement.of(builder);
    }

    @Override
    protected void updateDisplay() {
        this.groups.clear();
        this.groups.addAll(this.claim.getGroups());
        this.groups.sort(Comparator.comparing(x -> x.getKey().toString()));
        this.uuids.clear();
        this.uuids.addAll(this.claim.getOwners());
        this.uuids.addAll(this.claim.getTrusted());
        super.updateDisplay();
    }

    @Override
    protected DisplayElement getNavElement(int id) {
        return switch (id) {
            case 5 -> this.canModifyTrusted
                    ? DisplayElement.of(new GuiElementBuilder(Items.PLAYER_HEAD)
                    .setName(Text.translatable("text.goml.gui.player_list.add_player").formatted(Formatting.GREEN))
                    .setSkullOwner(GOMLTextures.GUI_ADD)
                    .setCallback((x, y, z) -> {
                        playClickSound(this.player);
                        this.ignoreCloseCallback = true;
                        this.close(true);
                        this.ignoreCloseCallback = false;
                        new GenericPlayerAndGroupSelectionGui(
                                this.player,
                                Text.translatable("text.goml.gui.player_add_gui.title"),
                                (p) -> !this.claim.hasDirectPermission(p.getId()),
                                (p) -> !this.claim.getGroups().contains(p),
                                (p) -> this.claim.trust(p.getId()),
                                this.claim::trust,
                                this::refreshOpen).updateAndOpen();
                    }))
                    : DisplayElement.filler();
            default -> super.getNavElement(id);
        };
    }

    @Override
    protected void modifyBuilder(GuiElementBuilder builder, Optional<GameProfile> optional, UUID uuid) {
        var exist = optional.isPresent();
        var gameProfile = exist ? optional.get() : null;
        var isOwner = this.claim.isOwner(uuid);
        var canRemove = isOwner ? this.canModifyOwners : this.canModifyTrusted;

        builder.setName(Text.literal(exist ? gameProfile.getName() : uuid.toString())
                .formatted(isOwner ? Formatting.GOLD : Formatting.WHITE).append(isOwner
                        ? Text.literal(" (").formatted(Formatting.DARK_GRAY)
                        .append(Text.translatable("text.goml.owner").formatted(Formatting.WHITE))
                        .append(Text.literal(")").formatted(Formatting.DARK_GRAY))
                        : Text.empty()
                ));

        if (canRemove) {
            builder.addLoreLine(Text.translatable("text.goml.gui.click_to_remove"));
            builder.setCallback((x, y, z) -> {
                playClickSound(player);
                (isOwner ? this.claim.getOwners() : this.claim.getTrusted()).remove(uuid);
                this.updateDisplay();
            });
        }
    }
}
