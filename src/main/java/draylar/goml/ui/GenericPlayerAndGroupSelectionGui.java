package draylar.goml.ui;

import com.mojang.authlib.GameProfile;
import draylar.goml.api.group.PlayerGroup;
import draylar.goml.api.group.PlayerGroupProvider;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class GenericPlayerAndGroupSelectionGui extends GenericPlayerSelectionGui {
    private final Predicate<PlayerGroup> shouldDisplayGroup;
    private final Consumer<PlayerGroup> onClickGroup;
    private final List<PlayerGroup> groups = new ArrayList<>();


    public GenericPlayerAndGroupSelectionGui(ServerPlayerEntity player, Text title, Predicate<GameProfile> shouldDisplay,
                                             Predicate<PlayerGroup> shouldDisplayGroup, Consumer<GameProfile> onClick, Consumer<PlayerGroup> onClickGroup, Runnable postClose) {
        super(player, title, shouldDisplay, onClick, postClose);
        this.shouldDisplayGroup = shouldDisplayGroup;
        this.onClickGroup = onClickGroup;
    }

    @Override
    protected void updateDisplay() {
        this.groups.clear();
        for (var group : PlayerGroupProvider.getAllGroups(player)) {
            if (this.shouldDisplayGroup.test(group)) {
                this.groups.add(group);
            }
        }

        super.updateDisplay();
    }

    @Override
    protected DisplayElement getElement(int id) {
        return id < this.groups.size() ? getGroupElement(id) : getPlayerElement(id - this.groups.size());
    }

    protected DisplayElement getGroupElement(int id) {
        var group = this.groups.get(id);
        var b = GuiElementBuilder.from(group.icon())
                .setName(Text.empty().append(group.selfDisplayName()).append(Text.literal(" (").formatted(Formatting.DARK_GRAY)
                                .append(Text.empty().append(group.provider().getName()).setStyle(Style.EMPTY.withColor(0x45abff))
                                        .append(Text.literal(")").formatted(Formatting.DARK_GRAY)))
                        )
                )
                .hideDefaultTooltip()
                .setCallback((x, y, z) -> {
                    playClickSound(this.player);
                    this.onClickGroup.accept(group);
                    this.close(this.closeCallback != null);
                });

        return DisplayElement.of(b);
    }
}
