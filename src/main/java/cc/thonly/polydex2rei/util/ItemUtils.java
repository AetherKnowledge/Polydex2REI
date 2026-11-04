package cc.thonly.polydex2rei.util;

import eu.pb4.polymer.core.api.client.ClientPolymerItem;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class ItemUtils {
    public static int PRINT_ID = 0;

    public static ItemStack getServerStack(ItemStack itemStack) {
        Optional<String> itemModelId = ClientPolymerItemUtils.getItemModelId(itemStack);
        if (itemModelId.isEmpty()) {
            return itemStack;
        }
        String strId = itemModelId.get();
        Identifier itemId = Identifier.parse(strId);
        ClientPolymerItem clientPolymerItem = ClientPolymerItem.REGISTRY.get(itemId);
        if (clientPolymerItem != null) {
            return clientPolymerItem.visualStack().copyWithCount(itemStack.getCount());
        }
        return itemStack;
    }

    public static void printfItemStack(ItemStack itemStack) {
        System.out.println("id" + PRINT_ID + itemStack);
        DataComponentPatch componentChanges = itemStack.getComponentsPatch();
        var changes = componentChanges.split();
        for (var component : changes.added()) {
            System.out.println(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type()) + " == " + component.value());
        }
        for (var type : changes.removed()) {
            System.out.println(BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type) + " == null");
        }
        PRINT_ID++;
    }
}
