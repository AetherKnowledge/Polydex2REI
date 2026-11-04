package cc.thonly.polydex2rei.mixin;

import cc.thonly.polydex2rei.client.PolydexTabNavigation;
import cc.thonly.polydex2rei.plugin.Polydex2REICategory;
import cc.thonly.polydex2rei.plugin.Polydex2REIDisplay;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import me.shedaniel.rei.impl.display.DisplaySpec;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(AbstractDisplayViewingScreen.class)
public abstract class AbstractDisplayViewingScreenMixin {
    @Unique
    private Polydex2REIDisplay polydex2rei$query;
    @Unique
    private Map<DisplayCategory<?>, List<DisplaySpec>> polydex2rei$categories;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void capturePolydexQuery(Map<DisplayCategory<?>, List<DisplaySpec>> categoryMap,
                                     @Nullable CategoryIdentifier<?> category, CallbackInfo ci) {
        polydex2rei$categories = categoryMap;
        List<DisplaySpec> displays = categoryMap.get(Polydex2REICategory.INSTANCE);
        if (displays != null) {
            for (DisplaySpec spec : displays) {
                if (spec.provideInternalDisplay() instanceof Polydex2REIDisplay query) {
                    polydex2rei$query = query;
                    break;
                }
            }
        }
        polydex2rei$openIfSelected(((DisplayScreen) this).getCurrentCategoryId());
    }

    @Inject(method = "selectCategory(Lme/shedaniel/rei/api/common/category/CategoryIdentifier;Z)V", at = @At("RETURN"))
    private void openPolydexCategory(CategoryIdentifier<?> category, boolean init, CallbackInfo ci) {
        polydex2rei$openIfSelected(category);
    }

    @Unique
    private void polydex2rei$openIfSelected(CategoryIdentifier<?> category) {
        if (polydex2rei$query != null && Polydex2REICategory.INSTANCE.getCategoryIdentifier().equals(category)) {
            PolydexTabNavigation.open((AbstractDisplayViewingScreen) (Object) this, polydex2rei$categories, polydex2rei$query);
        }
    }
}
