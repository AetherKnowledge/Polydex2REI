package cc.thonly.polydex2rei.plugin;

import cc.thonly.polydex2rei.network.Action;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public record Polydex2REIDisplay(Action action, String itemId) implements Display {
    public static final Component DISABLED_TEXT = Component.translatable("item.disabled").withStyle(ChatFormatting.RED);
    public static final ItemStack DEFAULT_ITEM = ((Supplier<ItemStack>) () -> {
        ItemStack defaultStack = Items.BEDROCK.getDefaultInstance();
        defaultStack.set(DataComponents.ITEM_NAME, DISABLED_TEXT);
        return defaultStack;
    }).get();
    public static final EntryIngredient INGREDIENT = EntryIngredient.of(EntryStack.of(VanillaEntryTypes.ITEM, DEFAULT_ITEM));

    @Override
    public List<EntryIngredient> getInputEntries() {
        return List.of(INGREDIENT);
    }

    @Override
    public List<EntryIngredient> getOutputEntries() {
        return List.of(INGREDIENT);
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return Polydex2REIPlugin.CATEGORY;
    }

    @Override
    public Optional<Identifier> getDisplayLocation() {
        return Optional.of(Identifier.parse(UUID.randomUUID().toString()));
    }

    @Override
    public @Nullable DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

}
