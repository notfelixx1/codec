package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.events.player.EventTravel;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BindSetting;
import monoton.module.settings.imp.ModeSetting;
import monoton.utils.move.MoveUtil;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static net.minecraft.client.Minecraft.player;

@Annotation(name = "WaterSpeed", type = TypeList.Movement, desc = "Ускоряет ваше движение в воде")
public class WaterSpeed extends Module {
    public final ModeSetting mode = new ModeSetting("Мод", "Matrix", "Matrix", "Grim", "MetaHvH");
    public BindSetting boostkey = new BindSetting("Кнопка буста", -1).setVisible(() -> mode.is("MetaHvH"));
    private long boostEndTime = 0;
    private boolean isBoosting = false;
    private boolean boostPressed = false;
    private final float s20 = 0.7015F;
    private final float s0 = 0.595F;
    private final float s15 = 0.6499F;
    private final float s25 = 0.749F;

    public WaterSpeed() {
        addSettings(mode, boostkey);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            if (e.key == boostkey.getKey()) {
                boostPressed = true;
            }
        }

        if (boostPressed) {
            isBoosting = true;
            boostEndTime = System.currentTimeMillis() + 900;
            boostPressed = false;
        }

        if (isBoosting && System.currentTimeMillis() > boostEndTime) {
            isBoosting = false;
        }
        if (mode.is("Grim")) {
            if (mc.gameSettings.keyBindJump.isKeyDown()) {
                if (mc.player.isInWater()) {
                    double waterLevel;
                    BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
                    try {
                        waterLevel = mc.player.world.getFluidState(playerPos).getActualHeight(mc.player.world, playerPos);
                    } catch (NoSuchMethodError e) {
                        if (mc.player.world.getBlockState(playerPos).getMaterial() == Material.WATER) {
                            waterLevel = playerPos.x + 1.0;
                        } else {
                            waterLevel = playerPos.y;
                        }
                    }

                    double playerEyeY = mc.player.getPosY() + mc.player.getEyeHeight();
                    if (playerEyeY >= waterLevel - 0.2 && playerEyeY <= waterLevel + 0.2) {
                        mc.player.motionY = 0.2;
                        MoveUtil.setMotion(MoveUtil.getMotion() * 2.0);
                    }
                } else if (mc.player.isInWater()) {
                    mc.player.motionY = Math.max(mc.player.motionY, 0.03);
                }
            }
        }
        if (mode.is("Matrix")) {
            if (event instanceof EventTravel move) {
                if (player.collidedVertically || player.collidedHorizontally) {
                    return false;
                }

                if (player.isSwimming() && player.isInWater()) {
                    float speed = 0.5f / 10F;
                    if (mc.gameSettings.keyBindJump.isPressed()) {
                        player.motion.y += 0.05f;
                    }
                    if (mc.gameSettings.keyBindSneak.isPressed()) {
                        player.motion.y -= 0.05f;
                    }
                    MoveUtil.setMotion(MoveUtil.getMotion());
                    move.speed += speed;
                }
            }
        }
        if (mode.is("MetaHvH")) {
            if (event instanceof EventTravel move) {
                if (player.collidedVertically || player.collidedHorizontally) {
                    return false;
                }

                if ((player.isInWater() || player.isInLava()) && player.isSwimming()) {
                    ItemStack offHandItem = player.getHeldItemOffhand();
                    EffectInstance speedEffect = player.getActivePotionEffect(Effects.SPEED);
                    EffectInstance deEffect = player.getActivePotionEffect(Effects.SLOWNESS);
                    String itemName = offHandItem.getDisplayName().getString();
                    float appliedSpeed = 0;

                    if (speedEffect != null) {
                        if (speedEffect.getAmplifier() == 2) {
                            if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                                appliedSpeed = s20 * 1.14F;
                            } else if (itemName.contains("Талисман Венома")) {
                                appliedSpeed = s25 * 1.14F;
                            } else if (itemName.contains("Талисман Картеля")) {
                                appliedSpeed = s15 * 1.14F;
                            } else {
                                appliedSpeed = s0 * 1.14F;
                            }
                        } else if (speedEffect.getAmplifier() == 1) {
                            if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                                appliedSpeed = s20;
                            } else if (itemName.contains("Талисман Венома")) {
                                appliedSpeed = s25;
                            } else if (itemName.contains("Талисман Картеля")) {
                                appliedSpeed = s15;
                            } else {
                                appliedSpeed = s0;
                            }
                        }
                    } else {
                        if (itemName.contains("Шар Геракла 2") || itemName.contains("Шар CHAMPION") || itemName.contains("Шар Аида 2") || itemName.contains("Шар GOD") || itemName.contains("КУБИК-РУБИК") || itemName.contains("Шар BUNNY")) {
                            appliedSpeed = s20 * 0.68F;
                        } else if (itemName.contains("Талисман Венома")) {
                            appliedSpeed = s25 * 0.68F;
                        } else if (itemName.contains("Талисман Картеля")) {
                            appliedSpeed = s15 * 0.68F;
                        } else {
                            appliedSpeed = s0 * 0.68F;
                        }
                    }

                    if (deEffect != null) {
                        appliedSpeed *= 0.85f;
                    }

                    if (isBoosting) {
                        appliedSpeed *= 1.75F;
                    }

                    MoveUtil.setSpeed(appliedSpeed);
                }
            }
        }
        return false;
    }
}