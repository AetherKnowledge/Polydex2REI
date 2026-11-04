package cc.thonly.polydex2rei.mixin;

import cc.thonly.polydex2rei.util.ItemUtils;
import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.impl.client.gui.widget.DisplayedEntryWidget;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(DisplayedEntryWidget.class)
public class DisplayedEntryWidgetMixin {
    @ModifyExpressionValue(
            method = "doAction",
            at = @At(
                    value = "INVOKE",
                    target = "Lme/shedaniel/rei/api/common/entry/EntryStack;copy()Lme/shedaniel/rei/api/common/entry/EntryStack;"
            )
    )
    private EntryStack<?> replaceCopiedEntry(EntryStack<?> original) {
        Object value = original.getValue();

        if (value instanceof ItemStack itemStack) {
            ItemStack serverStack = ItemUtils.getServerStack(itemStack);
            return EntryStacks.of(serverStack);
        }

        return original;
    }
}
