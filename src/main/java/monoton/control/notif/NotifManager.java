package monoton.control.notif;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import monoton.module.TypeList;
import monoton.utils.IMinecraft;
import monoton.utils.anim.Animation;
import monoton.utils.anim.Direction;
import monoton.utils.anim.impl.DecelerateAnimation;
import monoton.utils.font.Fonts;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.animation.AnimationMath;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.ITextComponent;

import java.util.concurrent.CopyOnWriteArrayList;

import static monoton.ui.clickgui.Panel.getColorByName;
import static monoton.utils.render.ColorUtils.rgba;

public class NotifManager {

    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList<>();

    public void add(String text, String content, int time, TypeList category) {
        notifications.add(new Notification(text, content, time, category, null));
    }

    public void add(String text, String content, int time, TypeList category, ITextComponent component) {
        notifications.add(new Notification(text, content, time, category, component));
    }

    public void draw(MatrixStack stack) {
        float yOffset = 0;
        for (int i = notifications.size() - 1; i >= 0; i--) {
            Notification notification = notifications.get(i);
            if (i < notifications.size() - 5) {
                notification.animation.setDirection(Direction.BACKWARDS);
                notification.animation.setDuration(300); // Faster animation for excess notifications
                notification.yAnimation.setDirection(Direction.BACKWARDS);
                notification.yAnimation.setDuration(300); // Faster animation for excess notifications
            } else {
                if (System.currentTimeMillis() - notification.getTime() > notification.time2 * 1000L - 100) {
                    notification.animation.setDirection(Direction.BACKWARDS);
                } else {
                    notification.animation.setDirection(Direction.FORWARDS);
                    notification.yAnimation.setDirection(Direction.FORWARDS);
                }
                if (System.currentTimeMillis() - notification.getTime() > notification.time2 * 1000L) {
                    notification.yAnimation.setDirection(Direction.BACKWARDS);
                }
            }

            notification.alpha = MathHelper.clamp((float) notification.animation.getOutput(), 0.1f, 1.0f);

            if (notification.animation.finished(Direction.BACKWARDS) && notification.yAnimation.finished(Direction.BACKWARDS)) {
                notifications.remove(notification);
                continue;
            }

            float x = (IMinecraft.mc.getMainWindow().scaledWidth() / 2) - (Fonts.intl[14].getWidth(notification.getText()) + 10) / 2;
            float baseY = IMinecraft.mc.getMainWindow().scaledHeight() / 2 + 37;
            notification.yAnimation.setEndPoint(yOffset);
            notification.yAnimation.setDuration(50);
            float y = baseY + yOffset;
            notification.setX(x);
            notification.setY(AnimationMath.fast(notification.getY(), y, 20));
            notification.draw(stack);
            yOffset += 17;
        }
    }

    private class Notification {
        private final TypeList category;
        @Getter
        @Setter
        private float x, y = (float) IMinecraft.mc.getMainWindow().scaledHeight() - 10;

        @Getter
        private String text;
        @Getter
        private String content;
        private final ITextComponent component;
        private boolean isState;
        private boolean state;
        @Getter
        private long time = System.currentTimeMillis();

        public Animation animation = new DecelerateAnimation(450, 1, Direction.FORWARDS);
        public Animation yAnimation = new DecelerateAnimation(450, 1, Direction.FORWARDS);
        float alpha;
        int time2 = 3;

        public Notification(String text, String content, int time, TypeList category, ITextComponent component) {
            this.text = text;
            this.content = content;
            this.time2 = time;
            this.category = category;
            this.component = component;
        }

        public float draw(MatrixStack stack) {
            float width = Fonts.intl[13].getWidth(text) + 14;
            int textColor = getColorByName("textColor");
            int iconColor = getColorByName("iconColor");
            int fonColor = getColorByName("fonColor");
            int fonduoColor = getColorByName("fonduoColor");

            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x - 5 + 0.3f, y - 2f, 14, 14, new Vector4f(3.5f, 3.5f, 0f, 0f), ColorUtils.reAlphaInt(fonduoColor, (int) (224 * alpha)), alpha);
            RenderUtilka.Render2D.drawBlurredRoundedRectangle(x + 14 - 5 - 0.3f, y - 2f, width + 8 - 14, 14, new Vector4f(0f, 0f, 3.5f, 3.5f), ColorUtils.reAlphaInt(fonColor, (int) (200 * alpha)), alpha);

            String icon = getIconForCategory(category);

            if (this.text.contains("enabled")) {
                Fonts.icon[15].drawCenteredString(stack, icon, x + 2.5f, y + 4f, ColorUtils.reAlphaInt(iconColor, (int) (255 * alpha)));
            } else if (this.text.contains("disabled")) {
                Fonts.icon[15].drawCenteredString(stack, icon, x + 2.5f, y + 4f, ColorUtils.reAlphaInt(iconColor, (int) (110 * alpha)));
            } else if (component != null) {
                ItemStack itemStack = getItemStackFromComponent(component);
                if (itemStack != null && !itemStack.isEmpty()) {
                    drawItemStack(itemStack, x - 2.5f, y + 0.5f, null, true);
                } else {
                    Fonts.icon[15].drawCenteredString(stack, "a", x + 2.5f, y + 4f, ColorUtils.reAlphaInt(iconColor, (int) (255 * alpha)));
                }
            } else {
                Fonts.icon[15].drawCenteredString(stack, "a", x + 2.5f, y + 4f, ColorUtils.reAlphaInt(iconColor, (int) (255 * alpha)));
            }

            if (this.text.contains("enabled")) {
                Fonts.intl[13].drawString(stack, text, x + 12.5f, y + 4f, ColorUtils.reAlphaInt(textColor, (int) (255 * alpha)));
            } else if (this.text.contains("disabled")) {
                Fonts.intl[13].drawString(stack, text, x + 12.5f, y + 4f, ColorUtils.reAlphaInt(textColor, (int) (110 * alpha)));
            } else {
                Fonts.intl[13].drawText(stack, component, x + 12.5f, y + 4f);
            }
            return 17;
        }

        public static void drawItemStack(ItemStack stack, double x, double y, String altText, boolean withoutOverlay) {
            RenderSystem.pushMatrix();
            RenderSystem.scalef(0.6f, 0.6f, 1.0f); // Scale to 75% of original size
            RenderSystem.translated(x * (1.0 / 0.6f), y * (1.0 / 0.6f), 0.0); // Adjust position for new scale
            IMinecraft.mc.getItemRenderer().renderItemAndEffectIntoGUI(stack, 0, 0);
            if (!withoutOverlay) {
                IMinecraft.mc.getItemRenderer().renderItemOverlayIntoGUI(IMinecraft.mc.fontRenderer, stack, 0, 0, altText);
            }
            RenderSystem.popMatrix();
        }

        private ItemStack getItemStackFromComponent(ITextComponent component) {
            try {
                for (int i = 0; i < IMinecraft.mc.player.inventory.getSizeInventory(); i++) {
                    ItemStack stack = IMinecraft.mc.player.inventory.getStackInSlot(i);
                    if (stack.getDisplayName().equals(component)) {
                        return stack;
                    }
                }
            } catch (Exception e) {
            }
            return ItemStack.EMPTY;
        }
    }

    private String getIconForCategory(TypeList category) {
        return switch (category) {
            case Combat -> "b";
            case Movement -> "c";
            case Render -> "d";
            case Player -> "e";
            case Misc -> "o";
        };
    }
}