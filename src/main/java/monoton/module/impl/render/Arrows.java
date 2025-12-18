package monoton.module.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import monoton.module.impl.combat.Aura;
import monoton.utils.render.animation.AnimationMath;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.control.Manager;
import monoton.control.party.PartyHandler;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.anim.animations.Easing;
import monoton.utils.anim.animations.TimeAnim;
import monoton.utils.font.Fonts;
import monoton.utils.move.MoveUtil;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import org.joml.Vector4i;

import java.awt.*;
import java.util.Map;

import static monoton.ui.clickgui.Panel.selectedColor;

@Annotation(name = "Arrows", type = TypeList.Render, desc = "Создаёт стрелочки к игрокам в мире и зелёные стрелочки к игрокам в пати")
public class Arrows extends Module {
    public final SliderSetting size3 = new SliderSetting("Размер", 17, 15, 20, 1);
    private final SliderSetting size2 = new SliderSetting("Радиус", 58, 30, 110, 2);
    private final BooleanOption dinam = new BooleanOption("Динамический", true);
    private final BooleanOption coloreget = new BooleanOption("Обозначать цель", true);
    private final BooleanOption party = new BooleanOption("Показывать пати", true);
    private final TimeAnim animationStep = new TimeAnim(Easing.EASE_OUT_EXPO, 1000L);
    private final MultiBoxSetting elements = new MultiBoxSetting("Показывать",
            new BooleanOption("Дистанция", true),
            new BooleanOption("Ник", true),
            new BooleanOption("Здоровье", false));
    public final BooleanOption clormode = new BooleanOption("Здоровье от темы", true).setVisible(() -> elements.get("Здоровье"));

    private final TimeAnim alphaAnimation = new TimeAnim(Easing.EASE_IN_OUT_SINE, 1000L);
    private PlayerEntity lastTarget = null;
    private long lastTargetTime = 0;
    private float animatedYaw;
    private float animatedPitch;

    public Arrows() {
        addSettings(elements, size3, size2, party, coloreget, clormode, dinam);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender render && render.isRender2D()) {
            render2D(render);
        }
        return false;
    }

    private void render2D(EventRender render) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTargetTime > 15000) {
            lastTarget = null;
        }

        Entity currentTarget = Aura.getTarget();

        if (currentTarget instanceof PlayerEntity && currentTarget != lastTarget) {
            lastTarget = (PlayerEntity) currentTarget;
            lastTargetTime = currentTime;
        }

        float size = size2.getValue().floatValue();
        if (mc.currentScreen instanceof InventoryScreen) {
            size += size2.getValue().floatValue() + 10f;
        }
        if (MoveUtil.isMoving() && dinam.get()) {
            size += 10.0f;
        }
        this.animationStep.run(size);

        for (Entity entity : mc.world.getPlayers()) {
            if (!(entity instanceof PlayerEntity)) {
                continue;
            }
            PlayerEntity player = (PlayerEntity) entity;

            if (player == mc.player || !player.botEntity || player == Minecraft.player) {
                continue;
            }

            double x = player.lastTickPosX + (player.getPosX() - player.lastTickPosX) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getX();
            double z = player.lastTickPosZ + (player.getPosZ() - player.lastTickPosZ) * mc.getRenderPartialTicks() - mc.getRenderManager().info.getProjectedView().getZ();
            renderArrow(render, x, z, player, currentTime);
        }

        if (party.get() && PartyHandler.isInParty()) {
            Map<String, double[]> partyCoords = PartyHandler.getPartyMemberCoordinates();
            for (Map.Entry<String, double[]> entry : partyCoords.entrySet()) {
                String username = entry.getKey();
                double[] coords = entry.getValue();
                double x = coords[0] - mc.getRenderManager().info.getProjectedView().getX();
                double z = coords[1] - mc.getRenderManager().info.getProjectedView().getZ();
                renderArrowForPartyMember(render, x, z, username, currentTime);
            }
        }
    }
    private float health = 0;

    private void renderArrow(EventRender render, double x, double z, PlayerEntity player, long currentTime) {
        double cos = MathHelper.cos((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double sin = MathHelper.sin((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double rotY = -(z * cos - x * sin);
        double rotX = -(x * cos + z * sin);
        float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);
        MainWindow mainWindow = mc.getMainWindow();
        double x2 = this.animationStep.getValue() * MathHelper.cos((float) Math.toRadians(angle)) + mainWindow.getScaledWidth() / 2.0f;
        double y2 = this.animationStep.getValue() * MathHelper.sin((float) Math.toRadians(angle)) + mainWindow.getScaledHeight() / 2.0f;
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.translated(x2 + this.animatedYaw, y2 + this.animatedPitch, 0.0);
        GlStateManager.rotatef(angle + 90.0f, 0.0f, 0.0f, 1.0f);
        int firstColor2 = selectedColor;

        int color;
        if (coloreget.get() && player == lastTarget && currentTime - lastTargetTime <= 15000) {
            if (!alphaAnimation.isFinished()) {
                alphaAnimation.run(1.0);
            } else {
                alphaAnimation.reset();
                alphaAnimation.run(1.0);
            }

            double progress = alphaAnimation.getProgress();

            int redFirstColor2 = (firstColor2 >> 16) & 0xFF;
            int greenFirstColor2 = (firstColor2 >> 8) & 0xFF;
            int blueFirstColor2 = firstColor2 & 0xFF;

            int red = (int) (255 + (redFirstColor2 - 255) * progress);
            int green = (int) (77 + (greenFirstColor2 - 77) * progress);
            int blue = (int) (79 + (blueFirstColor2 - 79) * progress);
            color = ColorUtils.rgba(red, green, blue, 255);
        } else {
            color = Manager.FRIEND_MANAGER.isFriend(player.getName().getString()) ? ColorUtils.rgba(0, 255, 0, 255) : firstColor2;
        }

        int colors2 = (player.getHealth() > 15 ? new Color(5, 190, 36, 255).getRGB() : (player.getHealth() > 8 ? new Color(170, 124, 4, 255).getRGB() : new Color(170, 4, 4, 255).getRGB()));
        int colors3 = new Color(47, 47, 47, 179).getRGB();
        this.health = AnimationMath.fast(health, player.getHealth() / player.getMaxHealth(), 12);
        this.health = MathHelper.clamp(this.health, 0, 1);
        if (elements.get("Здоровье")) {
            RenderUtilka.Render2D.drawRoundedCorner(1.5f - (size3.getValue().floatValue() / 2), 21.0f - (size3.getValue().floatValue() / 2), 1.5f, size3.getValue().floatValue() / 2 + 1, 0, colors3);
            RenderUtilka.Render2D.drawRoundedCorner(1.5f - (size3.getValue().floatValue() / 2), 21.0f - (size3.getValue().floatValue() / 2) * health, 1.5f, (size3.getValue().floatValue() / 2 + 1) * health, 0, clormode.get() ? ColorUtils.setAlpha(selectedColor, 255) : ColorUtils.setAlpha(colors2, 255));
        }

        RenderUtilka.Render2D.drawImageAlph(new ResourceLocation("monoton/images/arrows.png"), 1.5f - (size3.getValue().floatValue() / 2), 26.0f, size3.getValue().floatValue(), size3.getValue().floatValue(), new Vector4i(ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200)));
        int dist = Math.min(99, (int) mc.player.getDistance(player));
        String distText = dist + "m";
        String nameText;

        if (Manager.FUNCTION_MANAGER.nameProtect.state && Manager.FUNCTION_MANAGER.nameProtect.friends.get()) {
            nameText = (Manager.FRIEND_MANAGER.isFriend(player.getName().getString()) ? "monotondlc.xyz" : player.getName().getString());
        } else {
            nameText = player.getName().getString();
        }

        if (nameText.length() > 7) {
            nameText = nameText.substring(0, 7);
        }

        if (elements.get("Ник")) {
            Fonts.intl[11].drawString(render.matrixStack, nameText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.intl[11].getWidth(nameText) / 2) + 4, 25.0f, -1);
        }

        if (elements.get("Дистанция")) {
            if (elements.get("Ник")) {
                Fonts.intl[11].drawString(render.matrixStack, distText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.intl[11].getWidth(distText) / 2) + 4, 30.5f, -1);
            } else {
                Fonts.intl[11].drawString(render.matrixStack, distText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.intl[11].getWidth(distText) / 2) + 4, 25.0f, -1);
            }
        }

        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }

    private void renderArrowForPartyMember(EventRender render, double x, double z, String username, long currentTime) {
        double cos = MathHelper.cos((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double sin = MathHelper.sin((float) (mc.getRenderManager().info.getYaw() * (Math.PI / 180)));
        double rotY = -(z * cos - x * sin);
        double rotX = -(x * cos + z * sin);
        float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);
        MainWindow mainWindow = mc.getMainWindow();
        double partyRadius = this.animationStep.getValue() + 30.0f;
        double x2 = partyRadius * MathHelper.cos((float) Math.toRadians(angle)) + mainWindow.getScaledWidth() / 2.0f;
        double y2 = partyRadius * MathHelper.sin((float) Math.toRadians(angle)) + mainWindow.getScaledHeight() / 2.0f;
        GlStateManager.pushMatrix();
        GlStateManager.disableBlend();
        GlStateManager.translated(x2 + this.animatedYaw, y2 + this.animatedPitch, 0.0);
        GlStateManager.rotatef(angle + 90.0f, 0.0f, 0.0f, 1.0f);

        int color = ColorUtils.rgba(0, 255, 0, 255);

        RenderUtilka.Render2D.drawImageAlph(new ResourceLocation("monoton/images/arrows.png"), 1.5f - (size3.getValue().floatValue() / 2), 26.0f, size3.getValue().floatValue(), size3.getValue().floatValue(), new Vector4i(ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200), ColorUtils.setAlpha(color, 200)));

        double distX = x + mc.getRenderManager().info.getProjectedView().getX() - mc.player.getPosX();
        double distZ = z + mc.getRenderManager().info.getProjectedView().getZ() - mc.player.getPosZ();
        int dist = (int) Math.sqrt(distX * distX + distZ * distZ);
        String distText = dist + "m";
        String nameText = username.length() > 8 ? username.substring(0, 8) : username;

        Fonts.intl[11].drawString(render.matrixStack, nameText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.intl[11].getWidth(nameText) / 2) + 4, 25.0f, -1);
        Fonts.intl[11].drawString(render.matrixStack, distText, 1.5f - (size3.getValue().floatValue() / 3f) - (Fonts.intl[11].getWidth(distText) / 2) + 4, 30.5f, -1);

        GlStateManager.enableBlend();
        GlStateManager.popMatrix();
    }
}