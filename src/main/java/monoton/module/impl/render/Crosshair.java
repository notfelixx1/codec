package monoton.module.impl.render;

import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ColorSetting;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import net.minecraft.client.MainWindow;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.TypeList;
import monoton.utils.render.ColorUtils;
import monoton.utils.render.RenderUtilka;
import monoton.utils.render.animation.AnimationMath;
import org.joml.Vector4i;

@Annotation(name = "Crosshair", type = TypeList.Render, desc = "Меняет ваш прицел")
public class Crosshair extends Module {
    private final ModeSetting rotationMode = new ModeSetting("Мод", "Старый", "Старый", "Новый");

    private float currentCircleAnimation = 0.0F;
    public final SliderSetting size = new SliderSetting("Размер", 1.5f, 0.1f, 7, 0.1f).setVisible(() -> rotationMode.is("Старый"));
    public final SliderSetting size2 = new SliderSetting("Ширина", 2f, 2f, 5, 0.1f).setVisible(() -> rotationMode.is("Старый"));
    public final BooleanOption colored = new BooleanOption("Красный при наведение", true);
    public ColorSetting color = new ColorSetting("Цвет", ColorUtils.rgba(128, 115, 225, 255)).setVisible(() -> rotationMode.is("Старый"));

    private float circleAnimation = 0.0F;
    private float circleAnimation2 = 0.0F;
    private float circleAnimation3 = 0.0F;

    public Crosshair() {
        addSettings(rotationMode, color, size, size2, colored);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender) {
            renderCrosshair();
        }
        return false;
    }

    private void renderCrosshair() {
        if (mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON) {
            return;
        }

        MainWindow mainWindow = mc.getMainWindow();
        float x = mainWindow.getScaledWidth() / 2.0F;
        float y = mainWindow.getScaledHeight() / 2.0F;

        float cooldown = mc.player.getCooledAttackStrength(1.0F);
        float endRadius = MathHelper.clamp(cooldown * 360, 0, 360);
        float endRadius2 = MathHelper.clamp(cooldown * 1, 0, 1);
        float endRadius3 = MathHelper.clamp(cooldown * 2, 0, 1);

        circleAnimation = AnimationMath.lerp(circleAnimation, -endRadius, 4);
        circleAnimation2 = AnimationMath.lerp(circleAnimation2, endRadius2, 4);
        circleAnimation3 = AnimationMath.lerp(circleAnimation3, endRadius3, 1);

        int mainColor = color.get();

        int color3 = new java.awt.Color(197, 49, 70, 255).getRGB();

        float attackStrength = mc.player.getCooledAttackStrength(1.0F);
        float targetRadius = MathHelper.clamp(attackStrength * 360, 0, 360);
        currentCircleAnimation = AnimationMath.lerp(currentCircleAnimation, -targetRadius, 4);
        MainWindow window = mc.getMainWindow();
        float centerX = (float) window.scaledWidth() / 2.0F;
        float centerY = (float) window.scaledHeight() / 2.0F;

        int circleColor;
        if (mc.objectMouseOver != null && mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
            circleColor = colored.get() ? color3 : color.get();
        } else {
            circleColor = color.get();
        }

        Vector4i outlineColor;
        if (mc.objectMouseOver.getType() == RayTraceResult.Type.ENTITY) {
            if (colored.get()) {
                outlineColor = new Vector4i(ColorUtils.setAlpha(color3, 140), ColorUtils.setAlpha(color3, 140), ColorUtils.setAlpha(color3, 140), ColorUtils.setAlpha(color3, 140));
            } else {
                outlineColor = new Vector4i(ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140));
            }
        } else {
            outlineColor = new Vector4i(ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140), ColorUtils.setAlpha(-1, 140));
        }
        switch (rotationMode.getIndex()) {
            case 0 -> {
                RenderUtilka.Render2D.drawCircle(x, y, 0, 360, size.getValue().floatValue() + 2.0f, size2.getValue().floatValue(),
                        false, mainColor);
                RenderUtilka.Render2D.drawCircle(x, y, 0, circleAnimation, size.getValue().floatValue() + 2.0f, size2.getValue().floatValue(), false, circleColor);
            }
            case 1 -> {
                RenderUtilka.Render2D.drawRoundOutline(centerX - 3.5f, centerY - 3.5f, 7, 7, 3, 0f, ColorUtils.rgba(0, 0, 0, 0), outlineColor);
            }
        }
    }
}
