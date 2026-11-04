package cc.thonly.polydex2rei.plugin;

import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import cc.thonly.polydex2rei.client.PolydexTabNavigation;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import java.util.List;

public class Polydex2REICategory implements DisplayCategory<Polydex2REIDisplay> {
    public static final Component TITLE = Component.literal("Polydex");
    public static final Polydex2REICategory INSTANCE = new Polydex2REICategory();

    @Override
    public CategoryIdentifier<? extends Polydex2REIDisplay> getCategoryIdentifier() {
        return Polydex2REIPlugin.CATEGORY;
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public Renderer getIcon() {
        return EntryStacks.of(Items.GRASS_BLOCK);
    }

    @Override
    public List<Widget> setupDisplay(Polydex2REIDisplay display, Rectangle bounds) {
        return List.of(Widgets.createRecipeBase(bounds),
                Widgets.createLabel(new Point(bounds.getCenterX(), bounds.getCenterY() - 4), PolydexTabNavigation.getStatus(display))
                        .noShadow().color(0xFF404040, 0xFFBBBBBB)
                        .onRender((graphics, label) -> label.setMessage(PolydexTabNavigation.getStatus(display))));
    }
}
