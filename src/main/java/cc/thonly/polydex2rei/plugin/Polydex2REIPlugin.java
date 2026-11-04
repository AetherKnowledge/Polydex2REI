package cc.thonly.polydex2rei.plugin;

import cc.thonly.polydex2rei.Polydex2REI;
import cc.thonly.polydex2rei.client.PolydexTabNavigation;
import dev.architectury.utils.GameInstance;
import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import eu.pb4.polymer.core.api.utils.PolymerSyncedObject;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import me.shedaniel.rei.api.common.registry.display.ServerDisplayRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class Polydex2REIPlugin implements REIClientPlugin {
    public static final CategoryIdentifier<Polydex2REIDisplay> CATEGORY =
            CategoryIdentifier.of(Polydex2REI.id("category"));

    @Override
    public void registerCategories(CategoryRegistry registry) {
        registry.add(Polydex2REICategory.INSTANCE);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.exclusionZones().register(AbstractContainerScreen.class, PolydexTabNavigation::getExclusionZones);
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
//        registry.beginFiller(Object.class).fill(o -> new Polydex2REIDisplay());
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        registry.removeEntryIf(entryStack -> {
            Object value = entryStack.getValue();
            if (!(value instanceof ItemStack itemStack)) {
                return false;
            }
            Item item = itemStack.getItem();
            return PolymerSyncedObject.getSyncedObject(BuiltInRegistries.ITEM, item) != null;
        });

        for (ClientPolymerItem clientPolymerItem : ClientPolymerItem.REGISTRY) {
            // Server-only items have no local registry entry; Polymer syncs their visual stacks.
            ItemStack clientItemStack = clientPolymerItem.visualStack().copy();
            EntryStack<ItemStack> entryStack = EntryStack.of(VanillaEntryTypes.ITEM, clientItemStack);
            registry.addEntry(entryStack);
        }
    }
}
