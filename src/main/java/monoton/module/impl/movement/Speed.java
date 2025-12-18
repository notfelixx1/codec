package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.move.MoveUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.AxisAlignedBB;

import static net.minecraft.client.Minecraft.player;

@Annotation(name = "Speed", type = TypeList.Movement, desc = "Ускоряет ваше движение")
public class Speed extends Module {
    public ModeSetting mode = new ModeSetting("Режим", "Collision", new String[]{"Collision", "Matrix", "MetaHvH", "HolyWorld"});
    public SliderSetting speed3 = new SliderSetting("Скорocть", 0.36f, 0.10f, 0.7f, 0.01f).setVisible(() -> mode.is("Matrix"));
    public SliderSetting speed4 = new SliderSetting("Скорость", 1.1f, 0.5f, 2, 0.05f).setVisible(() -> mode.is("Collision"));
    public SliderSetting speed5 = new SliderSetting("Скорocть", 1.35f, 1.1f, 4, 0.05f, 1.4f).setVisible(() -> mode.is("HolyWorld"));
    public SliderSetting colison = new SliderSetting("Дистанция", 0.244f, 0.2f, 0.95f, 0.01f).setVisible(() -> mode.is("HolyWorld"));
    public BooleanOption autojump = new BooleanOption("Авто прыжок", true).setVisible(() -> mode.is("Matrix") || mode.is("MetaHvH"));

    public Speed() {
        this.addSettings(mode, autojump, speed3, speed4, speed5, colison);
    }

    public boolean onEvent(Event event) {
        if (!mc.player.abilities.isFlying) {
            if (event instanceof EventUpdate && (mode.is("Collision") || mode.is("Matrix") || mode.is("MetaHvH") || mode.is("HolyWorld"))) {
                if (mode.is("Matrix")) {
                    if (!mc.player.isElytraFlying() && !mc.player.isInWater() && !player.areEyesInFluid(FluidTags.WATER) && !mc.player.isOnGround()) {
                        MoveUtil.setSpeed(speed3.getValue().floatValue());
                    }
                    if (autojump.get()) {
                        if (MoveUtil.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.player.jump();
                        }
                    }
                }
                if (mode.is("MetaHvH")) {
                    float currentSpeed;
                    EffectInstance spd = mc.player.getActivePotionEffect(Effects.SPEED);
                    if (spd != null) {
                        if (spd.getAmplifier() == 0) {
                            currentSpeed = (0.358f) * 1.2630f;
                        } else if (spd.getAmplifier() == 1) {
                            currentSpeed = (0.358f) * 1.4530f;
                        } else if (spd.getAmplifier() >= 2) {
                            currentSpeed = (0.358f) * 1.6520f;
                        } else {
                            currentSpeed = 0.358f;
                        }
                    } else {
                        currentSpeed = 0.358f;
                    }
                    if (!mc.player.isElytraFlying() && !mc.player.isInWater() && !player.areEyesInFluid(FluidTags.WATER) && !mc.player.isOnGround()) {
                        MoveUtil.setSpeed(currentSpeed);
                    }
                    if (autojump.get()) {
                        if (MoveUtil.isMoving() && mc.player.isOnGround() && !mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.player.jump();
                        }
                    }
                }
                if (mode.is("Collision")) {
                    AxisAlignedBB aabb = mc.player.getBoundingBox().grow(0.1);
                    boolean canBoost = mc.world.getEntitiesWithinAABB(LivingEntity.class, aabb).size() > 1;
                    if (canBoost && !mc.player.isOnGround()) {
                        mc.player.jumpMovementFactor = speed4.getValue().floatValue() / 10;
                    }
                }
                if (mode.is("HolyWorld")) {
                    int collisions = 0;
                    AxisAlignedBB expandedBox = mc.player.getBoundingBox().grow(colison.getValue().floatValue());
                    boolean canBoost = mc.world.getEntitiesWithinAABB(LivingEntity.class, expandedBox).size() > 1;

                    if (canBoost) {
                        collisions++;
                    }

                    double[] motion = MoveUtil.forward(speed5.getValue().floatValue() * 2.05f * 0.01 * collisions);
                    mc.player.addVelocity(motion[0], 0.0, motion[1]);
                }
            }

        }
        return false;
    }

    public void onDisable() {
        mc.timer.timerSpeed = 1;
        super.onDisable();
    }
}
