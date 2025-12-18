package monoton.module.impl.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.util.text.*;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.utils.render.RenderUtilka;
import org.joml.Vector4d;
import org.joml.Vector4i;
import org.lwjgl.opengl.GL11;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.utils.font.Fonts;
import monoton.utils.math.PlayerPositionTracker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static monoton.utils.math.PlayerPositionTracker.isInView;
import static monoton.utils.render.ColorUtils.rgba;
import static monoton.utils.render.RenderUtilka.Render2D.*;

@Annotation(name = "ItemEsp", type = TypeList.Render, desc = "Теги предметов")
public class ItemEsp extends Module {

    private static final int MAX_ITEMS = 33;

    private final MultiBoxSetting elements = new MultiBoxSetting("Элементы",
            new BooleanOption("Имена", true),
            new BooleanOption("Иконка", false));

    private final Map<Vector4d, ItemEntity> positions = new ConcurrentHashMap<>();

    public ItemEsp() {
        addSettings(elements);
    }

    @Override
    public boolean onEvent(Event event) {
        if (!(event instanceof EventRender render) || mc.player == null || mc.world == null) {
            return false;
        }

        try {
            if (render.isRender3D()) {
                updateItemsPositions(render.partialTicks);
            }

            if (render.isRender2D()) {
                renderItemElements(render.matrixStack);
            }
        } catch (Exception e) {
        }

        return false;
    }

    private void updateItemsPositions(float partialTicks) {
        positions.clear();
        int count = 0;

        for (Entity entity : mc.world.getAllEntities()) {
            if (count >= MAX_ITEMS) break;

            if (entity instanceof ItemEntity item && isInView(entity)) {
                Vector4d position = PlayerPositionTracker.updatePlayerPositions(item, partialTicks);
                if (position != null) {
                    positions.put(position, item);
                    count++;
                }
            }
        }
    }

    private void renderItemElements(MatrixStack stack) {
        if (positions.isEmpty()) return;

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        final int boxBackgroundColor = rgba(0, 0, 0, 128);
        final Vector4i gradientColors = new Vector4i(-1, -1, -1, -1);

        List<Map.Entry<Vector4d, ItemEntity>> sortedPositions = new ArrayList<>(positions.entrySet());
        sortedPositions.sort((a, b) -> {
            double distA = mc.player.getDistance(a.getValue());
            double distB = mc.player.getDistance(b.getValue());
            return Double.compare(distB, distA);
        });

        for (Map.Entry<Vector4d, ItemEntity> entry : sortedPositions) {
            Vector4d position = entry.getKey();
            ItemEntity item = entry.getValue();

            double x = position.x;
            double y = position.y;
            double endX = position.z;
            double endY = position.w;

            float nameHeightOffset = 0;
            float iconWidthOffset = 0;

            if (elements.get(1)) {
                // Render icon first, positioned to the left
                renderItemIcon(stack, item, x, endX, y);
                iconWidthOffset = 16 * 0.6667f + 2; // Icon width (scaled) plus padding
            }

            if (elements.get(0)) {
                // Render name with offset if icon is present
                renderItemName(stack, item, x, y, endX, iconWidthOffset);
                nameHeightOffset = -10;
            }
        }

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
    }

    private void renderItemName(MatrixStack stack, ItemEntity item, double x, double y, double endX, float iconWidthOffset) {
        if (item == null || item.getItem().isEmpty()) return;

        ITextComponent itemName = item.getItem().getDisplayName();
        if (itemName == null) return;

        String countSuffix = item.getItem().getCount() > 1 ?
                TextFormatting.RED + " x" + item.getItem().getCount() : "";
        String tag = itemName.getString() + countSuffix;

        float tagWidth = Fonts.intl[11].getWidth(tag);
        float posX = (float) (x + ((endX - x) / 2) - tagWidth / 2 + (iconWidthOffset > 0 ? iconWidthOffset / 2 : 0));
        int rectColor = rgba(15, 15, 16, 125);
        RenderUtilka.Render2D.drawRoundedRect(posX - 1, (float) y - 9.6f, tagWidth + 2, 8, 0F, rectColor);
        Fonts.intl[11].drawText(stack, itemName, posX, (float) y - 6f);
        Fonts.intl[11].drawCenteredString(stack, countSuffix, posX + Fonts.intl[11].getWidth(tag) - (Fonts.intl[11].getWidth(countSuffix) / 2), (float) y - 6f, -1);
    }

    private void renderItemIcon(MatrixStack stack, ItemEntity item, double x, double endX, double y) {
        if (item == null || item.getItem().isEmpty()) return;

        ITextComponent itemName = item.getItem().getDisplayName();
        if (itemName == null) return;

        String countSuffix = item.getItem().getCount() > 1 ?
                TextFormatting.RED + " x" + item.getItem().getCount() : "";
        String tag = itemName.getString() + countSuffix;

        float tagWidth = Fonts.intl[11].getWidth(tag);


        float scale = 0.5967f;

        float itemPosX = (float) (x + ((endX - x) / 2) - (elements.get(1) ? tagWidth / 2 + 5 : 7));
        float itemPosY = (float) (y - 10.5f);

        RenderUtilka.Render2D.drawRoundedRect(itemPosX, itemPosY, 16 * scale, 16 * scale, 0.5f * scale, rgba(15, 15, 16, 125));

        RenderSystem.pushMatrix();
        RenderSystem.scaled(scale, scale, 1.0);
        RenderSystem.translated(itemPosX / scale, itemPosY / scale, 0.0);
        mc.getItemRenderer().renderItemAndEffectIntoGUI(item.getItem(), 0, 0);
        if (!elements.get(1)) {
            mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, item.getItem(), 0, 0, null);
        }
        RenderSystem.popMatrix();
    }
}