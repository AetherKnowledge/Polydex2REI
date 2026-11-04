package cc.thonly.polydex2rei.mixin;

import cc.thonly.polydex2rei.util.ClientPolymerItemUtils;
import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.comparison.ComparisonContext;
import me.shedaniel.rei.api.common.entry.comparison.ItemComparatorRegistry;
import me.shedaniel.rei.plugin.client.entry.ItemEntryDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ItemEntryDefinition.class)
@Pseudo
public class ItemEntryDefinitionMixin {
    @Inject(
            method = "equals(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lme/shedaniel/rei/api/common/entry/comparison/ComparisonContext;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    public void modifyEquals(ItemStack o1, ItemStack o2, ComparisonContext context, CallbackInfoReturnable<Boolean> cir) {
        // Different server items can share the same vanilla client carrier.
        if ((ClientPolymerItemUtils.isPolyItem(o1) || ClientPolymerItemUtils.isPolyItem(o2))
                && !ClientPolymerItemUtils.getRealItemId(o1).equals(ClientPolymerItemUtils.getRealItemId(o2))) {
            cir.setReturnValue(false);
            return;
        }
        if (ItemStack.isSameItemSameComponents(o1, o2)) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(
            method = "hash(Lme/shedaniel/rei/api/common/entry/EntryStack;Lnet/minecraft/world/item/ItemStack;Lme/shedaniel/rei/api/common/entry/comparison/ComparisonContext;)J",
            at = @At("HEAD"),
            cancellable = true
    )
    public void hash(EntryStack<ItemStack> entry, ItemStack value, ComparisonContext context, CallbackInfoReturnable<Long> cir) {
        if (ClientPolymerItemUtils.isPolyItem(value)) {
            String realItemId = ClientPolymerItemUtils.getRealItemId(value);
            if (realItemId != null) {
                long code = 1;
                code = 31 * code + realItemId.hashCode();
                code = 31 * code + Long.hashCode(ItemComparatorRegistry.getInstance().hashOf(context, value));
                cir.setReturnValue(code);
                cir.cancel();
            }
        }
    }

    @Inject(method = "wildcard(Lme/shedaniel/rei/api/common/entry/EntryStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true)
    public void wildcard(EntryStack<ItemStack> entry, ItemStack value, CallbackInfoReturnable<ItemStack> cir) {
        if (ClientPolymerItemUtils.isPolyItem(value)) {
            String realItemId = ClientPolymerItemUtils.getRealItemId(value);
            ClientPolymerItem item = ClientPolymerItem.REGISTRY.get(Identifier.parse(realItemId));

            if (item != null) {
                cir.setReturnValue(item.visualStack().copy());
            } else {
                cir.setReturnValue(value.copyWithCount(1));
            }
        }
    }

    @Inject(method = "cheatsAs(Lme/shedaniel/rei/api/common/entry/EntryStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"),
            cancellable = true
    )
    public void cheatsAs(EntryStack<ItemStack> entry, ItemStack value, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack itemStack = entry.getValue();
        Optional<String> itemModelId = ClientPolymerItemUtils.getItemModelId(itemStack);
        if (itemModelId.isEmpty()) {
            return;
        }
        String strId = itemModelId.get();
        Identifier itemId = Identifier.parse(strId);
        ClientPolymerItem clientPolymerItem = ClientPolymerItem.REGISTRY.get(itemId);
        if (clientPolymerItem != null) {
            ItemStack copy = clientPolymerItem.visualStack().copyWithCount(value.getCount());
            cir.setReturnValue(copy);
        }
    }

    @Inject(method = "getIdentifier(Lme/shedaniel/rei/api/common/entry/EntryStack;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/Identifier;",
            at = @At("HEAD"),
            cancellable = true
    )
    public void getIdentifier(EntryStack<ItemStack> entry, ItemStack value, CallbackInfoReturnable<Identifier> cir) {
        if (ClientPolymerItemUtils.isPolyItem(value)) {
            String realItemId = ClientPolymerItemUtils.getRealItemId(value);
            cir.setReturnValue(Identifier.parse(realItemId));
            cir.cancel();
        }
    }
}
