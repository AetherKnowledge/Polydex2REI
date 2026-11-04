package cc.thonly.polydex2rei.client;

import cc.thonly.polydex2rei.Polydex2REI;
import cc.thonly.polydex2rei.network.PolydexScreenPayload;
import cc.thonly.polydex2rei.network.StackActionPayload;
import cc.thonly.polydex2rei.plugin.Polydex2REIDisplay;
import cc.thonly.polydex2rei.plugin.Polydex2REIPlugin;
import dev.architectury.utils.value.IntValue;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.REIRuntime;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.impl.client.gui.screen.AbstractDisplayViewingScreen;
import me.shedaniel.rei.impl.client.REIRuntimeImpl;
import me.shedaniel.rei.impl.client.gui.widget.TabContainerWidget;
import me.shedaniel.rei.impl.display.DisplaySpec;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Keeps REI's category navigation available while the server owns the Polydex menu. */
public final class PolydexTabNavigation {
    private static Session session;

    private PolydexTabNavigation() {
    }

    public static void open(AbstractDisplayViewingScreen source,
                            Map<DisplayCategory<?>, List<DisplaySpec>> categories, Polydex2REIDisplay query) {
        StackActionPayload request = new StackActionPayload(query.action(), query.itemId());
        if (session != null && session.source == source && session.query.equals(request)
                && (session.pending || session.menuOpen)) {
            return;
        }
        session = new Session(source, categories, request);
        Polydex2REI.sendQueryPolydexPagePacket(query.action(), query.itemId());
    }

    public static void receive(PolydexScreenPayload payload) {
        Minecraft client = Minecraft.getInstance();
        Session current = session;
        if (current == null || !current.query.equals(payload.query()) || client.player == null) {
            return;
        }
        if (payload.open()) {
            if (current.returning) return;
            current.pending = false;
            current.menuOpen = true;
            current.containerId = payload.containerId();
            if (client.gui.screen() instanceof AbstractContainerScreen<?> screen
                    && screen.getMenu().containerId == payload.containerId()) {
                attach(screen);
            }
        } else {
            if (payload.containerId() < 0 && current.pending) {
                current.pending = false;
                boolean loading = payload.containerId() == PolydexScreenPayload.LOADING;
                current.retryTicks = loading ? 20 : 0;
                current.status = Component.translatable(loading ? "polydex2rei.status.loading" : "polydex2rei.status.unavailable");
                return;
            }
            if (current.containerId != payload.containerId()) return;
            session = null;
            if (payload.containerId() < 0) return;
            // A delayed close callback must not replace a restored REI view or a different menu.
            if (!current.returning && client.player.containerMenu == client.player.inventoryMenu) {
                client.gui.setScreen(new InventoryScreen(client.player));
            }
        }
    }

    public static void afterInit(Minecraft client, Screen screen, int width, int height) {
        if (session != null && session.menuOpen && screen instanceof AbstractContainerScreen<?> container) {
            attach(container);
        } else if (session != null && !session.pending && !session.returning
                && !(screen instanceof AbstractDisplayViewingScreen) && !(screen instanceof InventoryScreen)) {
            session = null;
        }
    }

    public static void tick(Minecraft client) {
        if (client.player == null) {
            reset();
        } else if (session != null && session.menuOpen
                && (!(client.gui.screen() instanceof AbstractContainerScreen<?>)
                || client.player.containerMenu == client.player.inventoryMenu)) {
            session.menuOpen = false;
        }
        if (session != null && session.returning && !(client.gui.screen() instanceof AbstractDisplayViewingScreen)) {
            reset();
        }
        Session current = session;
        if (current != null && current.retryTicks > 0) {
            if (client.gui.screen() != current.source
                    || !current.source.getCurrentCategoryId().equals(Polydex2REIPlugin.CATEGORY)) {
                reset();
            } else if (--current.retryTicks == 0) {
                current.pending = true;
                Polydex2REI.sendQueryPolydexPagePacket(current.query.action(), current.query.id());
            }
        }
    }

    public static Component getStatus(Polydex2REIDisplay display) {
        Session current = session;
        if (current != null && current.query.action() == display.action() && current.query.id().equals(display.itemId())) {
            return current.status;
        }
        return Component.translatable("polydex2rei.status.opening");
    }

    public static void reset() {
        session = null;
    }

    public static List<Rectangle> getExclusionZones(Screen screen) {
        if (session == null || !session.menuOpen || session.screen != screen || session.tabBounds == null) {
            return List.of();
        }
        return List.of(session.tabBounds);
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics,
                              int mouseX, int mouseY, float delta) {
        Session current = session;
        if (current != null && current.menuOpen && current.screen == screen) {
            // Render with the container, before REI's final tooltip pass.
            for (var widget : current.tabs.widgets()) widget.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    private static void attach(AbstractContainerScreen<?> screen) {
        Session current = session;
        if (current == null || !current.menuOpen || screen.getMenu() == Minecraft.getInstance().player.inventoryMenu) {
            return;
        }
        current.screen = screen;
        TabContainerWidget tabs = new TabContainerWidget();
        current.tabs = tabs;
        Rectangle container = ScreenRegistry.getInstance().getScreenBounds(screen);
        // Short windows have no space above a full-height container. Keep tabs beside its header instead.
        Rectangle bounds = container.y >= 28
                ? new Rectangle(container.x, container.y - 28, container.width, 28)
                : new Rectangle(4, Math.max(18, container.y), Math.max(34, container.x - 8), 28);
        List<DisplayCategory<?>> categories = new ArrayList<>(current.categories.keySet());
        int selected = 0;
        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).getCategoryIdentifier().equals(Polydex2REIPlugin.CATEGORY)) selected = i;
        }
        final int selectedIndex = selected;
        IntValue page = new IntValue() {
            private int value = -1;
            @Override public void accept(int value) { this.value = value; }
            @Override public int getAsInt() { return value; }
        };
        IntValue selection = new IntValue() {
            @Override public void accept(int index) { returnToCategory(current, categories.get(index).getCategoryIdentifier()); }
            @Override public int getAsInt() { return selectedIndex; }
        };
        tabs.init(bounds, bounds, categories, page, selection, () -> {});
        tabs.updateScroll(categories, selectedIndex, 0);
        int exclusionTop = Math.max(0, bounds.y - 18);
        current.tabBounds = new Rectangle(bounds.x, exclusionTop, bounds.width, bounds.getMaxY() - exclusionTop + 4);
        REIRuntime.getInstance().getOverlay().ifPresent(overlay -> overlay.queueReloadOverlay());
        ScreenMouseEvents.allowMouseClick(screen).register((ignored, event) -> {
            if (session == current && current.menuOpen && current.screen == screen) {
                for (var widget : tabs.widgets()) {
                    if (widget.mouseClicked(event, false)) return false;
                }
                return !bounds.contains(event.x(), event.y());
            }
            return true;
        });
        ScreenMouseEvents.allowMouseScroll(screen).register((ignored, x, y, horizontal, vertical) -> {
            if (session == current && current.menuOpen && current.screen == screen) {
                for (var widget : tabs.widgets()) {
                    if (widget.mouseScrolled(x, y, horizontal, vertical)) return false;
                }
                return !bounds.contains(x, y);
            }
            return true;
        });
        ScreenMouseEvents.allowMouseRelease(screen).register((ignored, event) ->
                session != current || !current.menuOpen || current.screen != screen || !bounds.contains(event.x(), event.y()));
    }

    private static void returnToCategory(Session current, CategoryIdentifier<?> category) {
        Minecraft client = Minecraft.getInstance();
        if (session != current || !current.menuOpen || category.equals(Polydex2REIPlugin.CATEGORY)
                || client.gui.screen() != current.screen || client.player == null) {
            return;
        }
        current.returning = true;
        current.menuOpen = false;
        // Close the server menu without a null screen, which grabs and recenters the mouse.
        client.player.connection.send(new ServerboundContainerClosePacket(client.player.containerMenu.containerId));
        client.player.containerMenu = client.player.inventoryMenu;
        for (int i = 0; i < current.categories.size() && !current.source.getCurrentCategoryId().equals(category); i++) {
            current.source.nextCategory();
        }
        client.gui.setScreen(current.source);
        // Temporary server menus replace REI's global return target while Polydex is open.
        REIRuntimeImpl.getInstance().setPreviousScreen(current.returnScreen);
    }

    private static final class Session {
        private final AbstractDisplayViewingScreen source;
        private final Map<DisplayCategory<?>, List<DisplaySpec>> categories;
        private final StackActionPayload query;
        private final Screen returnScreen;
        private boolean pending = true;
        private boolean menuOpen;
        private boolean returning;
        private int containerId = -1;
        private int retryTicks;
        private Component status = Component.translatable("polydex2rei.status.opening");
        private AbstractContainerScreen<?> screen;
        private TabContainerWidget tabs;
        private Rectangle tabBounds;

        private Session(AbstractDisplayViewingScreen source, Map<DisplayCategory<?>, List<DisplaySpec>> categories,
                        StackActionPayload query) {
            this.source = source;
            this.categories = new LinkedHashMap<>(categories);
            this.query = query;
            this.returnScreen = REIRuntime.getInstance().getPreviousScreen();
        }
    }
}
