package cc.thonly.polydex2rei.mixin;

import cc.thonly.polydex2rei.network.Action;
import cc.thonly.polydex2rei.network.StackActionPayload;
import cc.thonly.polydex2rei.plugin.Polydex2REICategory;
import cc.thonly.polydex2rei.plugin.Polydex2REIDisplay;
import cc.thonly.polydex2rei.util.ClientPolymerItemUtils;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.impl.client.ClientHelperImpl;
import me.shedaniel.rei.impl.display.DisplaySpec;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Mixin(ClientHelperImpl.ViewSearchBuilderImpl.class)
public class ViewSearchBuilderImplMixin {
    @Inject(method = "buildMapInternal", at = @At("RETURN"), cancellable = true)
    private void addPolydexCategory(CallbackInfoReturnable<Map<DisplayCategory<?>, List<DisplaySpec>>> cir) {
        if (PluginManager.areAnyReloading()) {
            return;
        }
        ViewSearchBuilder builder = (ViewSearchBuilder) this;
        boolean usages = builder.getRecipesFor().isEmpty();
        List<EntryStack<?>> query = usages ? builder.getUsagesFor() : builder.getRecipesFor();
        if (query.size() != 1 || !(query.getFirst().getValue() instanceof ItemStack item) || item.isEmpty()
                || !ClientPlayNetworking.canSend(StackActionPayload.PACKET_ID)) {
            return;
        }

        // REI already matched the recipes. Only append the bridge's category.
        Map<DisplayCategory<?>, List<DisplaySpec>> categories = new LinkedHashMap<>(cir.getReturnValue());
        categories.put(Polydex2REICategory.INSTANCE, List.of(new Polydex2REIDisplay(
                usages ? Action.RIGHT : Action.LEFT, ClientPolymerItemUtils.getRealItemId(item))));
        cir.setReturnValue(categories);
    }
}
