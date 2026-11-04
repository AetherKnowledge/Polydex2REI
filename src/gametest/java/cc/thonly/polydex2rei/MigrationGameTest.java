package cc.thonly.polydex2rei;

import cc.thonly.polydex2rei.network.Action;
import cc.thonly.polydex2rei.network.NoticeOpenInventory;
import cc.thonly.polydex2rei.network.PolydexScreenPayload;
import cc.thonly.polydex2rei.network.StackActionPayload;
import cc.thonly.polydex2rei.plugin.Polydex2REIPlugin;
import cc.thonly.polydex2rei.plugin.Polydex2REICategory;
import cc.thonly.polydex2rei.plugin.Polydex2REIDisplay;
import cc.thonly.polydex2rei.util.ClientPolymerItemUtils;
import cc.thonly.polydex2rei.client.PolydexTabNavigation;
import eu.pb4.polydex.impl.PolydexImpl;
import eu.pb4.polydex.api.v1.recipe.PolydexPageUtils;
import io.netty.buffer.Unpooled;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.gui.screen.DisplayScreen;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import me.shedaniel.rei.api.common.plugins.PluginManager;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import me.shedaniel.rei.plugin.common.BuiltinPlugin;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.level.GameType;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public class MigrationGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        context.runOnClient(client -> {
            // Force transformation of lazy REI classes as well as startup classes.
            String[] targets = {
                    "me.shedaniel.rei.RoughlyEnoughItemsNetwork",
                    "me.shedaniel.rei.impl.common.registry.displays.ServerDisplayRegistryImpl",
                    "me.shedaniel.rei.impl.client.ClientHelperImpl$ViewSearchBuilderImpl",
                    "me.shedaniel.rei.impl.client.ClientHelperImpl",
                    "me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen",
                    "me.shedaniel.rei.impl.client.gui.screen.CompositeDisplayViewingScreen",
                    "me.shedaniel.rei.impl.client.gui.screen.DefaultDisplayViewingScreen",
                    "me.shedaniel.rei.impl.client.gui.widget.DisplayedEntryWidget",
                    "me.shedaniel.rei.impl.client.gui.widget.EntryWidget",
                    "me.shedaniel.rei.plugin.client.entry.ItemEntryDefinition",
                    "me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl"
            };
            for (String target : targets) {
                try {
                    Class.forName(target, false, getClass().getClassLoader());
                } catch (ClassNotFoundException e) {
                    throw new AssertionError("Missing mixin target " + target, e);
                }
            }

        });

        try (var world = context.worldBuilder().create()) {
            world.getServer().waitFor(server -> PolydexImpl.getEntry(new ItemStack(Items.STONE)) != null);
            context.runOnClient(client -> {
                ItemStack stack = new ItemStack(Items.STONE);
                require(ClientPolymerItemUtils.getRealItemId(stack).equals("minecraft:stone"), "Vanilla item identity changed");
                CompoundTag original = new CompoundTag();
                original.putString("id", "example:custom_item");
                CompoundTag custom = new CompoundTag();
                custom.put(ClientPolymerItemUtils.POLYMER_ID, original);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
                require(ClientPolymerItemUtils.isPolyItem(stack), "Polymer item no longer detected");
                require(ClientPolymerItemUtils.getRealItemId(stack).equals("example:custom_item"), "Polymer identity lost");
                RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), client.getConnection().registryAccess());
                try {
                    for (Action action : Action.values()) {
                        StackActionPayload payload = new StackActionPayload(action, "example:custom_item");
                        StackActionPayload.CODEC.encode(buffer, payload);
                        require(buffer.readInt() == action.getId(), "Action wire format changed");
                        require(buffer.readUtf().equals(payload.id()), "Item ID wire format changed");
                        buffer.clear();
                        StackActionPayload.CODEC.encode(buffer, payload);
                        require(StackActionPayload.CODEC.decode(buffer).equals(payload), "Payload round trip failed");
                        buffer.clear();
                    }
                    NoticeOpenInventory.CODEC.encode(buffer, new NoticeOpenInventory());
                    require(NoticeOpenInventory.CODEC.decode(buffer) != null, "Inventory notification codec failed");
                    buffer.clear();
                    var screenState = new PolydexScreenPayload(17, true, new StackActionPayload(Action.LEFT, "polyfactory:steel_gear"));
                    PolydexScreenPayload.CODEC.encode(buffer, screenState);
                    require(PolydexScreenPayload.CODEC.decode(buffer).equals(screenState), "Screen state codec failed");
                } finally {
                    buffer.release();
                }
            });
            world.getServer().runOnServer(server -> {
                Identifier id = Identifier.parse("polyfactory:steel_gear");
                require(BuiltInRegistries.ITEM.containsKey(id), "PolyFactory test item missing");
                var entry = PolydexImpl.getEntry(new ItemStack(BuiltInRegistries.ITEM.getValue(id)));
                require(entry != null && !entry.outputPages().isEmpty(), "PolyFactory recipes missing");
                require(!entry.ingredientPages().isEmpty(), "PolyFactory usages missing");
            });
            for (String itemId : new String[]{"minecraft:stone", "polyfactory:steel_gear"}) {
                for (Action action : new Action[]{Action.LEFT, Action.RIGHT}) {
                    context.runOnClient(client -> Polydex2REI.sendQueryPolydexPagePacket(action, itemId));
                    context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>
                            && client.player.containerMenu != client.player.inventoryMenu);
                    context.runOnClient(client -> client.player.closeContainer());
                    context.waitFor(client -> client.player.containerMenu == client.player.inventoryMenu);
                }
            }
            context.waitFor(client -> !EntryRegistry.getInstance().isReloading()
                    && EntryRegistry.getInstance().getEntryStacks().anyMatch(entry -> entry.getValue() instanceof ItemStack item
                    && ClientPolymerItemUtils.getRealItemId(item).equals("polyfactory:steel_gear")));
            context.waitFor(client -> !PluginManager.areAnyReloading()
                    && DisplayRegistry.getInstance().getAll().values().stream()
                    .flatMap(java.util.Collection::stream).anyMatch(MigrationGameTest::makesDiamondChestplate));
            context.runOnClient(client -> {
                EntryStack<?> chestplate = EntryRegistry.getInstance().getEntryStacks()
                        .filter(entry -> entry.getValue() instanceof ItemStack item && item.is(Items.DIAMOND_CHESTPLATE))
                        .findFirst().orElseThrow();
                var builder = ViewSearchBuilder.builder().addRecipesFor(chestplate);
                var categories = builder.buildMapInternal();
                require(categories.values().stream().flatMap(java.util.Collection::stream)
                        .anyMatch(spec -> makesDiamondChestplate(spec.provideInternalDisplay())),
                        "Diamond chestplate crafting recipe removed from REI lookup");
                ViewSearchBuilder.builder().addUsagesFor(EntryStacks.of(Items.DIRT)).buildMapInternal();
                var query = (Polydex2REIDisplay) categories.get(Polydex2REICategory.INSTANCE)
                        .getFirst().provideInternalDisplay();
                require(query.itemId().equals("minecraft:diamond_chestplate") && query.action() == Action.LEFT,
                        "Unrelated lookup changed the recipe view's Polydex query");
                var repeatedLookup = ViewSearchBuilder.builder().addRecipesFor(chestplate).buildMapInternal();
                require(repeatedLookup.values().stream().flatMap(java.util.Collection::stream)
                        .anyMatch(spec -> makesDiamondChestplate(spec.provideInternalDisplay())),
                        "Unrelated lookup hides diamond chestplate crafting recipe");
                require(builder.open(), "Diamond chestplate crafting screen did not open");
            });
            context.waitForScreen(AbstractDisplayViewingScreen.class);
            context.waitTicks(5);
            context.runOnClient(client -> require(client.gui.screen() instanceof AbstractDisplayViewingScreen
                    && client.player.containerMenu == client.player.inventoryMenu,
                    "Native crafting view was replaced by the Polydex container"));
            context.setScreen(() -> null);
            context.waitFor(client -> DisplayRegistry.getInstance().get(BuiltinPlugin.CRAFTING).stream()
                    .anyMatch(display -> display.getDisplayLocation()
                            .filter(id -> id.equals(Identifier.parse("polyfactory:steel_gear"))).isPresent()), 600);
            context.runOnClient(client -> {
                var gear = EntryRegistry.getInstance().getEntryStacks()
                        .filter(entry -> entry.getValue() instanceof ItemStack item
                                && ClientPolymerItemUtils.getRealItemId(item).equals("polyfactory:steel_gear"))
                        .findFirst().orElseThrow();
                var output = DisplayRegistry.getInstance().get(BuiltinPlugin.CRAFTING).stream()
                        .filter(display -> display.getDisplayLocation()
                                .filter(id -> id.equals(Identifier.parse("polyfactory:steel_gear"))).isPresent())
                        .flatMap(display -> display.getOutputEntries().stream())
                        .flatMap(java.util.Collection::stream)
                        .findFirst().orElseThrow(() -> new AssertionError("Steel gear crafting display missing"));
                ItemStack displayItem = (ItemStack) output.getValue();
                require(ClientPolymerItemUtils.getRealItemId(displayItem).equals("polyfactory:steel_gear"),
                        "Native recipe sync lost Polymer identity");
                require(EntryStacks.equalsFuzzy(gear, output), "Steel gear visual stack does not match native crafting output");
            });
            verifyNativeRecipe(context, "polyfactory:steel_gear", Action.LEFT, BuiltinPlugin.CRAFTING, "polyfactory:steel_gear");
            verifyNativeRecipe(context, "polyfactory:steel_ingot", Action.LEFT, BuiltinPlugin.SMELTING, "polyfactory:steel_ingot");
            verifyNativeRecipe(context, "polyfactory:steel_ingot", Action.LEFT, BuiltinPlugin.BLASTING, "polyfactory:steel_ingot_blasting");
            verifyNativeRecipe(context, "polyfactory:steel_alloy_mixture", Action.RIGHT, BuiltinPlugin.SMELTING, "polyfactory:steel_ingot");
            verifyNativeRecipe(context, "polyfactory:netherite_drill_head", Action.LEFT, BuiltinPlugin.SMITHING,
                    "polyfactory:netherite_drill_head_smithing");
            var factoryEntries = context.computeOnClient(client -> EntryRegistry.getInstance().getEntryStacks()
                    .filter(entry -> entry.getValue() instanceof ItemStack item
                            && ClientPolymerItemUtils.getRealItemId(item).startsWith("polyfactory:"))
                    .toList());
            require(!factoryEntries.isEmpty(), "PolyFactory entries missing");
            for (var factoryEntry : factoryEntries) {
                context.runOnClient(client -> {
                    String itemId = ClientPolymerItemUtils.getRealItemId((ItemStack) factoryEntry.getValue());
                    for (Action action : new Action[]{Action.LEFT, Action.RIGHT}) {
                        var builder = ViewSearchBuilder.builder();
                        if (action == Action.LEFT) {
                            builder.addRecipesFor(factoryEntry);
                        } else {
                            builder.addUsagesFor(factoryEntry);
                        }
                        for (var category : builder.buildMapInternal().entrySet()) {
                            if (category.getKey() == Polydex2REICategory.INSTANCE) continue;
                            for (var spec : category.getValue()) {
                                Display display = spec.provideInternalDisplay();
                                var ingredients = action == Action.LEFT ? display.getOutputEntries() : display.getInputEntries();
                                if (ingredients.isEmpty()) continue;
                                require(ingredients.stream().flatMap(java.util.Collection::stream)
                                        .anyMatch(candidate -> candidate.getValue() instanceof ItemStack item
                                                && ClientPolymerItemUtils.getRealItemId(item).equals(itemId)),
                                        "Unrelated native " + action + " result for " + itemId);
                            }
                        }
                    }
                });
            }
            Polydex2REI.LOGGER.info("Verified recipe and usage identity for {} PolyFactory entries", factoryEntries.size());
            verifyPolydexLoading(context, world.getServer());
            verifyTabNavigation(context, world.getServer(), Action.LEFT);
            verifyTabNavigation(context, world.getServer(), Action.RIGHT);
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().openMenu(
                    new SimpleMenuProvider((id, inventory, player) -> ChestMenu.threeRows(id, inventory),
                            Component.literal("Unrelated chest"))));
            context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>
                    && client.player.containerMenu != client.player.inventoryMenu);
            var chestPosition = context.computeOnClient(client -> ScreenRegistry.getInstance().getScreenBounds(client.gui.screen()));
            moveCursorInGui(context, chestPosition.getCenterX(), Math.max(0, chestPosition.y - 14));
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.runOnClient(client -> require(client.gui.screen() instanceof AbstractContainerScreen<?>,
                    "REI tabs leaked into an unrelated container"));
            context.runOnClient(client -> client.player.closeContainer());
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void verifyNativeRecipe(ClientGameTestContext context, String itemId, Action action,
                                           CategoryIdentifier<?> categoryId, String recipeId) {
        context.runOnClient(client -> {
            var item = EntryRegistry.getInstance().getEntryStacks()
                    .filter(entry -> entry.getValue() instanceof ItemStack stack
                            && ClientPolymerItemUtils.getRealItemId(stack).equals(itemId))
                    .findFirst().orElseThrow(() -> new AssertionError("Missing REI item " + itemId));
            var builder = ViewSearchBuilder.builder();
            if (action == Action.LEFT) {
                builder.addRecipesFor(item);
            } else {
                builder.addUsagesFor(item);
            }
            var displays = builder.buildMapInternal().entrySet().stream()
                    .filter(entry -> entry.getKey().getCategoryIdentifier().equals(categoryId))
                    .flatMap(entry -> entry.getValue().stream());
            require(displays.anyMatch(spec -> spec.provideInternalDisplay().getDisplayLocation()
                            .filter(id -> id.equals(Identifier.parse(recipeId))).isPresent()),
                    "Missing native recipe " + recipeId + " for " + itemId + " in " + categoryId);
            // Crafting should be selected naturally on the first Steel Gear click.
            if (!categoryId.equals(BuiltinPlugin.CRAFTING)) {
                builder.setPreferredOpenedCategory(categoryId);
            }
            require(builder.open(), "Native recipe screen did not open for " + itemId);
        });
        context.waitForScreen(AbstractDisplayViewingScreen.class);
        context.waitTicks(5);
        context.runOnClient(client -> {
            require(client.gui.screen() instanceof AbstractDisplayViewingScreen screen
                            && screen.getCurrentCategoryId().equals(categoryId)
                            && client.player.containerMenu == client.player.inventoryMenu,
                    "Native recipe category was replaced for " + itemId);
        });
        context.setScreen(() -> null);
    }

    private static boolean makesDiamondChestplate(Display display) {
        return display.getOutputEntries().stream().flatMap(java.util.Collection::stream)
                .anyMatch(entry -> entry.getValue() instanceof ItemStack item && item.is(Items.DIAMOND_CHESTPLATE));
    }

    private static void verifyPolydexLoading(ClientGameTestContext context, TestServerContext serverContext) {
        var query = new Polydex2REIDisplay(Action.LEFT, "polyfactory:steel_gear");
        for (boolean cancel : new boolean[]{false, true}) {
            serverContext.runOnServer(server -> setPolydexReady(false));
            try {
                context.runOnClient(client -> {
                    client.gui.setScreen(new InventoryScreen(client.player));
                    var item = EntryRegistry.getInstance().getEntryStacks()
                            .filter(entry -> entry.getValue() instanceof ItemStack stack
                                    && ClientPolymerItemUtils.getRealItemId(stack).equals(query.itemId()))
                            .findFirst().orElseThrow();
                    require(ViewSearchBuilder.builder().addRecipesFor(item)
                            .setPreferredOpenedCategory(Polydex2REIPlugin.CATEGORY).open(), "Loading view failed to open");
                });
                context.waitFor(client -> PolydexTabNavigation.getStatus(query)
                        .equals(Component.translatable("polydex2rei.status.loading")));
                context.waitTicks(45);
                context.runOnClient(client -> require(client.gui.screen() instanceof AbstractDisplayViewingScreen
                                && client.player.containerMenu == client.player.inventoryMenu,
                        "Polydex opened before its recipe index was ready"));
                if (cancel) {
                    context.runOnClient(client -> {
                        var screen = (AbstractDisplayViewingScreen) client.gui.screen();
                        while (!screen.getCurrentCategoryId().equals(BuiltinPlugin.CRAFTING)) screen.nextCategory();
                    });
                    context.waitTicks(25);
                } else {
                    Polydex2REI.LOGGER.info("Polydex loading screenshot: {}", context.takeScreenshot("polydex-loading"));
                }
            } finally {
                serverContext.runOnServer(server -> setPolydexReady(true));
            }
            if (cancel) {
                context.waitTicks(45);
                context.runOnClient(client -> require(client.gui.screen() instanceof AbstractDisplayViewingScreen screen
                                && screen.getCurrentCategoryId().equals(BuiltinPlugin.CRAFTING)
                                && client.player.containerMenu == client.player.inventoryMenu,
                        "Cancelled Polydex request reopened after loading"));
            } else {
                context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>
                        && client.player.containerMenu != client.player.inventoryMenu);
                context.runOnClient(client -> client.player.closeContainer());
            }
            context.setScreen(() -> null);
        }
    }

    private static void setPolydexReady(boolean ready) {
        // Deterministically exercise a long index rebuild without waiting minutes for a large pack.
        try {
            var field = PolydexImpl.class.getDeclaredField("isReady");
            field.setAccessible(true);
            field.setBoolean(null, ready);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }

    private static void verifyTabNavigation(ClientGameTestContext context, TestServerContext serverContext, Action action) {
        boolean creative = action == Action.RIGHT;
        serverContext.runOnServer(server -> server.getPlayerList().getPlayers().getFirst()
                .setGameMode(creative ? GameType.CREATIVE : GameType.SURVIVAL));
        context.waitFor(client -> client.player.getAbilities().instabuild == creative);
        var origin = context.computeOnClient(client -> {
            var screen = creative
                    ? new CreativeModeInventoryScreen(client.player, client.level.enabledFeatures(), false)
                    : new InventoryScreen(client.player);
            client.gui.setScreen(screen);
            return screen;
        });
        var categories = context.computeOnClient(client -> {
            var item = EntryRegistry.getInstance().getEntryStacks()
                    .filter(entry -> entry.getValue() instanceof ItemStack stack
                            && ClientPolymerItemUtils.getRealItemId(stack).equals("polyfactory:steel_gear"))
                    .findFirst().orElseThrow();
            var builder = ViewSearchBuilder.builder().setPreferredOpenedCategory(Polydex2REIPlugin.CATEGORY);
            if (action == Action.LEFT) builder.addRecipesFor(item); else builder.addUsagesFor(item);
            var ids = builder.buildMapInternal().keySet().stream().map(category -> category.getCategoryIdentifier()).toList();
            require(builder.open(), "Polydex view failed to open");
            return ids;
        });
        for (int cycle = 0; cycle < 3; cycle++) {
            context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>
                    && client.player.containerMenu != client.player.inventoryMenu);
            context.waitTicks(5);
            if (cycle == 0 && action == Action.LEFT) {
                Polydex2REI.LOGGER.info("Polydex tabs screenshot: {}", context.takeScreenshot("polydex-tabs"));
                String title = context.computeOnClient(client -> client.gui.screen().getTitle().getString());
                // Browsing Polydex can replace the server menu without sending another bridge request.
                serverContext.runOnServer(server -> PolydexPageUtils.openUsagesListUi(
                        server.getPlayerList().getPlayers().getFirst(),
                        PolydexImpl.getEntry(new ItemStack(Items.IRON_INGOT)), () -> {}));
                context.waitFor(client -> client.gui.screen() instanceof AbstractContainerScreen<?>
                        && !client.gui.screen().getTitle().getString().equals(title));
                context.waitTicks(3);
            }
            if (cycle == 1 && action == Action.LEFT) {
                context.getInput().resizeWindow(960, 640);
                context.waitTicks(3);
                Polydex2REI.LOGGER.info("Expanded Polydex tabs screenshot: {}", context.takeScreenshot("polydex-tabs-expanded"));
            }
            // REI calculates tab hitboxes during rendering, which need not happen on every test tick.
            var rendered = new java.util.concurrent.atomic.AtomicBoolean();
            context.runOnClient(client -> ScreenEvents.afterExtract(client.gui.screen()).register(
                    (screen, graphics, mouseX, mouseY, delta) -> rendered.set(true)));
            context.waitFor(client -> rendered.get());
            var bounds = context.computeOnClient(client -> ScreenRegistry.getInstance().getScreenBounds(client.gui.screen()));
            var tabBounds = bounds.y >= 28
                    ? new me.shedaniel.math.Rectangle(bounds.x, bounds.y - 28, bounds.width, 28)
                    : new me.shedaniel.math.Rectangle(4, Math.max(18, bounds.y), Math.max(34, bounds.x - 8), 28);
            int tabSize = context.computeOnClient(client -> ConfigObject.getInstance().isUsingCompactTabs() ? 24 : 28);
            int craftingIndex = categories.indexOf(BuiltinPlugin.CRAFTING);
            require(craftingIndex >= 0 && categories.size() <= 5, "Crafting tab unavailable for navigation test");
            double x = tabBounds.getCenterX() - categories.size() * tabSize / 2.0 + craftingIndex * tabSize + tabSize / 2.0;
            double y = tabBounds.y + 14;
            moveCursorInGui(context, x, y);
            if (cycle == 1 && action == Action.LEFT) {
                // Cursor at the tab edge makes its tooltip overlap the adjacent tab.
                moveCursorInGui(context, x + tabSize / 2.0 - 2, y);
                Polydex2REI.LOGGER.info("Polydex tab tooltip screenshot: {}", context.takeScreenshot("polydex-tab-tooltip"));
                moveCursorInGui(context, x, y);
            }
            // Dispatch through Fabric without moving the desktop cursor or depending on native event polling.
            context.runOnClient(client -> {
                client.mouseHandler.resyncMousePosition();
                double cursorX = client.mouseHandler.xpos();
                double cursorY = client.mouseHandler.ypos();
                var screen = client.gui.screen();
                require(!PolydexTabNavigation.getExclusionZones(screen).isEmpty(), "Polydex lost its tab navigation");
                require(!ScreenMouseEvents.allowMouseClick(screen).invoker().allowMouseClick(screen,
                                new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0))),
                        "Crafting tab click was not consumed");
                require(Math.abs(client.mouseHandler.xpos() - cursorX) < 1
                                && Math.abs(client.mouseHandler.ypos() - cursorY) < 1,
                        "Switching from Polydex moved the cursor");
            });
            context.waitForScreen(AbstractDisplayViewingScreen.class);
            context.waitFor(client -> client.player.containerMenu == client.player.inventoryMenu);
            context.waitTicks(5);
            context.runOnClient(client -> {
                require(client.gui.screen() instanceof DisplayScreen display
                            && display.getCurrentCategoryId().equals(BuiltinPlugin.CRAFTING),
                        "Tab click failed to restore the native crafting view");
            });
            serverContext.runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                require(player.containerMenu == player.inventoryMenu, "Polydex menu remained open on the server");
            });
            if (cycle < 2) {
                context.runOnClient(client -> {
                    var display = (DisplayScreen) client.gui.screen();
                    for (int i = 0; i < categories.size() && !display.getCurrentCategoryId().equals(Polydex2REIPlugin.CATEGORY); i++) {
                        display.nextCategory();
                    }
                });
            }
        }
        context.getInput().pressKey(InputConstants.KEY_ESCAPE);
        context.waitTicks(5);
        context.runOnClient(client -> require(client.gui.screen() == origin,
                "Escape after switching from Polydex failed to restore " + origin.getClass().getSimpleName()
                        + "; got " + client.gui.screen()));
        context.setScreen(() -> null);
        context.waitTicks(2);
    }

    private static void moveCursorInGui(ClientGameTestContext context, double x, double y) {
        var position = context.computeOnClient(client -> new double[]{
                x * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
                y * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()});
        context.getInput().setCursorPos(position[0], position[1]);
    }
}
