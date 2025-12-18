package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.impl.combat.Aura;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.move.MoveUtil;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.concurrent.ThreadLocalRandom;

@Annotation(name = "Jesus", type = TypeList.Movement, desc = "Ходьба по воде и лаве")
public class Jesus extends Module {
    public final ModeSetting mode = new ModeSetting("Мод", "MetaHvH", "MetaHvH", "SlowJump");
    private final SliderSetting speed = new SliderSetting("Скорость", 0.3F, 0.1F, 0.8F, 0.01F).setVisible(() -> mode.is("MetaHvH"));
    private final SliderSetting boostSpeed = new SliderSetting("Скорость буста", 0.55F, 0.3F, 1.5F, 0.01F).setVisible(() -> mode.is("MetaHvH"));
    private final SliderSetting boostTime = new SliderSetting("Время буста", 3F, 1F, 7F, 1F).setVisible(() -> mode.is("MetaHvH"));
    private final BindSetting boostKey = new BindSetting("Ускорение", -1).setVisible(() -> mode.is("MetaHvH"));
    public final BooleanOption nocollision = new BooleanOption("Не приземлятся", true);
    private boolean boostActive = false;
    private int boostTicks = 0;
    private boolean wasSneaking = false;

    public Jesus() {
        addSettings(mode, speed, boostKey, boostSpeed, boostTime, nocollision);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey) {
            if (mode.is("MetaHvH")) {
                EventKey e = (EventKey) event;
                if (e.key == boostKey.getKey()) {
                    boostActive = true;
                    boostTicks = boostTime.getValue().intValue() * 20;
                }
            }
        }

        if (event instanceof EventUpdate) {
            if (mc.player == null || mc.world == null) return false;
            if (mode.is("MetaHvH")) {
                if (mc.player.isInWater() || mc.player.isInLava()) {
                    if (Aura.target != null) {
                        if (!mc.gameSettings.keyBindJump.isKeyDown()) {
                            mc.gameSettings.keyBindJump.setPressed(true);
                        }
                    }
                    if (!nocollision.get() && mc.player.collidedHorizontally) return false;
                    if (!MoveUtil.isMoving()) {
                        mc.player.setMotion(0.0, mc.player.getMotion().y, 0.0);
                    }

                    float currentSpeed;

                    if (boostActive) {
                        if (boostTicks > 0) {
                            currentSpeed = boostSpeed.getValue().floatValue();
                            boostTicks--;
                        } else {
                            boostActive = false;
                            currentSpeed = speed.getValue().floatValue();
                        }
                    } else {
                        EffectInstance spd = mc.player.getActivePotionEffect(Effects.SPEED);
                        if (spd != null) {
                            if (spd.getAmplifier() == 0) {
                                currentSpeed = (speed.getValue().floatValue()) * 1.2630f;
                            } else if (spd.getAmplifier() == 1) {
                                currentSpeed = (speed.getValue().floatValue()) * 1.3530f;
                            } else if (spd.getAmplifier() >= 2) {
                                currentSpeed = (speed.getValue().floatValue()) * 1.5520f;
                            } else {
                                currentSpeed = speed.getValue().floatValue();
                            }
                        } else {
                            currentSpeed = speed.getValue().floatValue();
                        }
                    }

                    EffectInstance slow = mc.player.getActivePotionEffect(Effects.SLOWNESS);

                    if (slow != null) {
                        currentSpeed *= 0.85F;
                    }

                    MoveUtil.setSpeed(currentSpeed);

                    double ySpeed = mc.gameSettings.keyBindJump.isKeyDown() ? 0.019 : 0.0031;

                    mc.player.setMotion(mc.player.getMotion().x, ySpeed, mc.player.getMotion().z);
                } else {
                    boostActive = false;
                    boostTicks = 0;
                    if (wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                        mc.gameSettings.keyBindSneak.setPressed(false);
                        wasSneaking = false;
                    }
                }
            } else {
                if (mc.player.isInWater() || mc.player.isInLava()) {
                    if (!mc.gameSettings.keyBindJump.isKeyDown()) {
                        mc.gameSettings.keyBindJump.setPressed(true);
                    }

                    BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
                    BlockPos abovePos = playerPos.up();
                    double playerY = mc.player.getPosY();
                    int blockY = playerPos.getY();
                    if ((mc.player.isInWater() && mc.world.getBlockState(playerPos).getBlock() == Blocks.WATER ||
                            mc.player.isInLava() && mc.world.getBlockState(playerPos).getBlock() == Blocks.LAVA) &&
                            mc.world.getBlockState(abovePos).getBlock() == Blocks.AIR &&
                            playerY >= blockY + 0.55) {
                        if (!mc.gameSettings.keyBindSneak.isKeyDown()) {
                            mc.gameSettings.keyBindSneak.setPressed(true);
                            wasSneaking = true;
                        }

                        if (!nocollision.get() && mc.player.collidedHorizontally) return false;

                        double ySpeed = 0.045;
                        mc.player.setMotion(mc.player.getMotion().x, ySpeed, mc.player.getMotion().z);
                    } else {
                        if (wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                            mc.gameSettings.keyBindSneak.setPressed(false);
                            wasSneaking = false;
                        }
                    }
                } else {
                    if (mc.player.isOnGround() && wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
                        mc.gameSettings.keyBindSneak.setPressed(false);
                        wasSneaking = false;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onDisable() {
        boostActive = false;
        boostTicks = 0;
        if (wasSneaking && mc.gameSettings.keyBindSneak.isKeyDown()) {
            mc.gameSettings.keyBindSneak.setPressed(false);
            wasSneaking = false;
        }
        super.onDisable();
    }
}