package cc.thonly.polydex2rei.client;

import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.impl.client.gui.widget.TabWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** A compact category strip beside menus that have no room for tabs above them. */
final class CompactCategoryTabs extends WidgetWithBounds {
    private static final int ROW_HEIGHT = 28;
    private final Rectangle bounds;
    private final List<DisplayCategory<?>> categories;
    private final int selected;
    private final IntConsumer select;
    private final List<Widget> widgets = new ArrayList<>();
    private final int visibleRows;
    private final boolean overflow;
    private int firstVisible;

    CompactCategoryTabs(Rectangle container, int screenHeight, List<DisplayCategory<?>> categories,
                        int selected, IntConsumer select) {
        this.categories = categories;
        this.selected = selected;
        this.select = select;
        int y = Math.max(4, container.y);
        int availableHeight = screenHeight - y - 4;
        overflow = categories.size() * ROW_HEIGHT + 8 > availableHeight;
        visibleRows = Math.min(categories.size(), Math.max(1, (availableHeight - 8 - (overflow ? 28 : 0)) / ROW_HEIGHT));
        bounds = new Rectangle(Math.max(2, container.x - 36), y, 32,
                visibleRows * ROW_HEIGHT + 8 + (overflow ? 28 : 0));
        firstVisible = Math.min(selected / visibleRows * visibleRows, categories.size() - visibleRows);
        rebuild();
    }

    private void rebuild() {
        widgets.clear();
        widgets.add(Widgets.createRecipeBase(bounds));
        if (overflow) {
            widgets.add(Widgets.createButton(new Rectangle(bounds.x + 4, bounds.y + 4, 24, 12), Component.literal("↑"))
                    .onClick(button -> scroll(-visibleRows)).enabled(firstVisible > 0)
                    .tooltipLine(Component.translatable("text.rei.previous_page")));
            widgets.add(Widgets.createButton(new Rectangle(bounds.x + 4, bounds.getMaxY() - 16, 24, 12), Component.literal("↓"))
                    .onClick(button -> scroll(visibleRows)).enabled(firstVisible + visibleRows < categories.size())
                    .tooltipLine(Component.translatable("text.rei.next_page")));
        }
        for (int row = 0; row < visibleRows; row++) {
            int index = firstVisible + row;
            DisplayCategory<?> category = categories.get(index);
            int top = bounds.y + 4 + (overflow ? 14 : 0) + row * ROW_HEIGHT;
            TabWidget tab = TabWidget.create(0, 24, bounds.x + 4, top + 24, 0, 166, widget -> {
                if (index == selected) return false;
                Widgets.produceClickSound();
                select.accept(index);
                return true;
            });
            tab.setRenderer(category, category.getIcon(), category.getTitle(), index == selected);
            widgets.add(tab);
        }
    }

    private void scroll(int amount) {
        firstVisible = Mth.clamp(firstVisible + amount, 0, categories.size() - visibleRows);
        rebuild();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        for (Widget widget : widgets) {
            if (widget.mouseClicked(event, doubleClick)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (Widget widget : widgets) {
            if (widget.mouseReleased(event)) return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (!bounds.contains(x, y) || vertical == 0) return false;
        if (overflow) {
            scroll(vertical > 0 ? -1 : 1);
        } else {
            select.accept(Math.floorMod(selected + (vertical > 0 ? -1 : 1), categories.size()));
        }
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        for (Widget widget : widgets) widget.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public Rectangle getBounds() {
        return bounds;
    }

    @Override
    public List<Widget> children() {
        return widgets;
    }
}
