package cc.thonly.polydex2rei.client;

import cc.thonly.polydex2rei.plugin.Polydex2REICategory;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Button;
import me.shedaniel.rei.impl.client.gui.widget.TabWidget;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public final class CompactCategoryTabsChecks {
    public static void verify() {
        var container = new Rectangle(72, 10, 176, 220);
        var selected = new AtomicInteger(-1);
        var small = new CompactCategoryTabs(container, 240,
                Collections.nCopies(4, Polydex2REICategory.INSTANCE), 3, selected::set);
        require(small.children().stream().noneMatch(Button.class::isInstance), "Unnecessary compact pagination");
        require(small.getBounds().getMaxX() <= container.x && small.getBounds().getMaxY() <= 236,
                "Compact category strip overlaps the menu or screen edge");
        var overflow = new CompactCategoryTabs(container, 180,
                Collections.nCopies(12, Polydex2REICategory.INSTANCE), 11, selected::set);
        require(overflow.getBounds().getMaxY() <= 176, "Overflow tabs extend below the screen");
        require(overflow.children().stream().filter(TabWidget.class::isInstance).map(TabWidget.class::cast)
                .anyMatch(TabWidget::isSelected), "Selected category should be visible initially");
        require(overflow.mouseScrolled(overflow.getBounds().getCenterX(), 40, 0, 1), "Compact strip did not scroll");
        var first = overflow.children().stream().filter(TabWidget.class::isInstance).map(TabWidget.class::cast)
                .findFirst().orElseThrow();
        require(overflow.mouseClicked(new MouseButtonEvent(first.getBounds().getCenterX(), first.getBounds().getCenterY(),
                        new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false)
                        && selected.get() >= 0 && selected.get() < 11,
                "A scrolled category could not be selected");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
