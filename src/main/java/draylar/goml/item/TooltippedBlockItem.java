package draylar.goml.item;

import eu.pb4.polymer.core.api.block.PolymerHeadBlock;
import eu.pb4.polymer.core.api.item.PolymerHeadBlockItem;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.function.Consumer;

public class TooltippedBlockItem extends PolymerHeadBlockItem {

    private final int lines;

    public <T extends Block & PolymerHeadBlock> TooltippedBlockItem(T block, Settings settings, int lines) {
        super(block, settings);
        this.lines = lines;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        this.addLines(tooltip::add);
    }

    public void addLines(Consumer<Text> textConsumer) {
        for (int i = 1; i <= lines; i++) {
            textConsumer.accept(Text.translatable(String.format("%s.description.%d", getTranslationKey(), i)).formatted(Formatting.GRAY));
        }
    }
}
