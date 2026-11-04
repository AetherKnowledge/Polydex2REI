package cc.thonly.polydex2rei.mixin;

import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultBlastingDisplay;
import me.shedaniel.rei.plugin.common.displays.cooking.DefaultCookingDisplay;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(DefaultBlastingDisplay.class)
public abstract class DefaultBlastingDisplayMixin extends DefaultCookingDisplay {
    @Shadow
    public static DisplaySerializer<DefaultBlastingDisplay> SERIALIZER;

    protected DefaultBlastingDisplayMixin(List<EntryIngredient> inputs, List<EntryIngredient> outputs,
                                          Optional<Identifier> location, float xp, double cookTime) {
        super(inputs, outputs, location, xp, cookTime);
    }

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void useBlastingSerializer(CallbackInfo ci) {
        // REI 26.3.824 uses the smoking constructor for its blasting serializer.
        SERIALIZER = serializer(DefaultBlastingDisplay::new);
    }
}
