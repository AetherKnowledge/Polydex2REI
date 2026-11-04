package cc.thonly.polydex2rei.util;

import eu.pb4.polymer.core.api.item.PolymerItemUtils;
import java.util.Optional;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class ClientPolymerItemUtils {
    public static final String POLYMER_ID = PolymerItemUtils.POLYMER_STACK;
    public static final String REI_ITEM_ID = "$rei_item_id";
    public static final String POLY_MC_ID = "PolyMcOriginal";
    public static final String DEFAULT_ITEM_ID = Identifier.withDefaultNamespace("stone").toString();

    public static ItemStack parseToEiv(ItemStack itemStack) {
        ItemStack result = new ItemStack(Items.TRIAL_KEY);
        applyIfPresent(result, itemStack, DataComponents.ITEM_NAME);
        applyIfPresent(result, itemStack, DataComponents.CUSTOM_NAME);
        applyIfPresent(result, itemStack, DataComponents.ITEM_MODEL);
        applyIfPresent(result, itemStack, DataComponents.CUSTOM_MODEL_DATA);
        applyIfPresent(result, itemStack, DataComponents.DYED_COLOR);
        CustomData nbtComponent = itemStack.get(DataComponents.CUSTOM_DATA);
        if (nbtComponent != null) {
            Optional<String> polymerStackId = getPolymerStackId(itemStack);
            if (polymerStackId.isPresent()) {
                CompoundTag nbtCompound = new CompoundTag();
                nbtCompound.putString(REI_ITEM_ID, polymerStackId.get());
                CustomData nbtCompoundResult = CustomData.of(nbtCompound);
                result.set(DataComponents.CUSTOM_DATA, nbtCompoundResult);
            }
        }
        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void applyIfPresent(ItemStack target, ItemStack source, DataComponentType componentType) {
        if (source.get(componentType) != null) {
            target.set(componentType, source.get(componentType));
        }
    }

    public static boolean isPolyItem(ItemStack itemStack) {
        return getPolymerStackId(itemStack).isPresent() || getPolyMcStackId(itemStack).isPresent()
                || getEivStackId(itemStack).isPresent();
    }

    public static String getRealItemId(ItemStack itemStack) {
        if (itemStack == null) {
            return DEFAULT_ITEM_ID;
        }
        if (itemStack.isEmpty()) {
            return DEFAULT_ITEM_ID;
        }
        Optional<String> polymerStackId = getPolymerStackId(itemStack);
        Optional<String> polyMcStackId = getPolyMcStackId(itemStack);
        Optional<String> eivStackId = getEivStackId(itemStack);
        Optional<String> itemModelId = getItemModelId(itemStack);
        if (polymerStackId.isPresent())
            return polymerStackId.get();
        if (polyMcStackId.isPresent())
            return polyMcStackId.get();
        if (eivStackId.isPresent())
            return eivStackId.get();
        if (itemModelId.isPresent()) {
            return itemModelId.get();
        }
        if (itemStack.getItem() == Items.AIR) {
            return DEFAULT_ITEM_ID;
        }
        return BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString();
    }

    public static Optional<String> getItemModelId(ItemStack itemStack) {
        Identifier modelId = itemStack.get(DataComponents.ITEM_MODEL);
        if (modelId==null) {
            return Optional.empty();
        }
        return Optional.of(modelId.toString());
    }

    public static Optional<String> getPolymerStackId(ItemStack itemStack) {
        CustomData customData = getCustomData(itemStack);
        if (customData == null) {
            return Optional.empty();
        }
        CompoundTag nbtCompound = customData.copyTag();
        Tag element = nbtCompound.get(POLYMER_ID);
        if (!(element instanceof CompoundTag polymerStack)) {
            return Optional.empty();
        }
        if (polymerStack.contains("id")) {
            return polymerStack.getString("id");
        }
        return Optional.empty();
    }

    public static Optional<String> getPolyMcStackId(ItemStack itemStack) {
        CustomData customData = getCustomData(itemStack);
        if (customData == null) {
            return Optional.empty();
        }
        CompoundTag nbtCompound = customData.copyTag();
        Tag element = nbtCompound.get(POLY_MC_ID);
        if (!(element instanceof CompoundTag polymerStack)) {
            return Optional.empty();
        }
        if (polymerStack.contains("id")) {
            return polymerStack.getString("id");
        }
        return Optional.empty();
    }

    public static Optional<String> getEivStackId(ItemStack itemStack) {
        CustomData customData = getCustomData(itemStack);
        if (customData == null) {
            return Optional.empty();
        }
        CompoundTag nbtCompound = customData.copyTag();
        if (!nbtCompound.contains(REI_ITEM_ID)) {
            return Optional.empty();
        }
        return nbtCompound.getString(REI_ITEM_ID);
    }

    public static CustomData getCustomData(ItemStack itemStack) {
        return itemStack.get(DataComponents.CUSTOM_DATA);
    }
}