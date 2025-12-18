package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventMotion;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.settings.imp.ModeSetting;
import monoton.utils.math.ViaUtil;
import monoton.utils.misc.TimerUtil;
import monoton.utils.other.OtherUtil;
import monoton.utils.world.InventoryUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CEntityActionPacket;
import net.minecraft.network.play.server.SEntityMetadataPacket;
import monoton.module.api.Module;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.tileentity.ShulkerBoxTileEntity;
import net.minecraft.tileentity.TileEntity;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import static net.minecraft.client.Minecraft.player;

@Annotation(
        name = "HighJump",
        type = TypeList.Movement
)
public class HighJump extends Module {
    public static ModeSetting mode = new ModeSetting("Режим", "Элитра", "Элитра", "Шалкер");

    public HighJump() {
        this.addSettings(mode);
    }

    private final TimerUtil timerUtil = new TimerUtil();
    int oldItem = -1;

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (mode.is("Элитра")) {
                if (mc.player.isElytraFlying()) {
                    mc.gameSettings.keyBindSneak.setPressed(true);
                } else {
                    mc.gameSettings.keyBindSneak.setPressed(false);
                }
                int timeSwap = 550;
                mc.gameSettings.keyBindForward.setPressed(false);
                mc.gameSettings.keyBindRight.setPressed(false);
                mc.gameSettings.keyBindBack.setPressed(false);
                mc.gameSettings.keyBindLeft.setPressed(false);

                for (int i = 0; i < 9; ++i) {
                    if (mc.player.inventory.getStackInSlot(i).getItem() == Items.ELYTRA) {
                        if (mc.player.fallDistance < 4.0F) {
                            if (!mc.player.isOnGround()) {
                                if (!mc.player.isInWater()) {
                                    if (!mc.player.isInLava()) {
                                        for (Entity entity : mc.world.getAllEntities()) {
                                            if (entity instanceof FireworkRocketEntity) {
                                                mc.player.setMotion(0, 1.525, 0);
                                                if (timerUtil.hasTimeElapsed(200, true)) {
                                                    toggle();
                                                }
                                            }
                                        }
                                        if (!mc.player.collidedHorizontally && this.timerUtil.hasTimeElapsed((long) timeSwap)) {
                                            mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                                            mc.player.connection.sendPacket(new CEntityActionPacket(mc.player, CEntityActionPacket.Action.START_FALL_FLYING));
                                            mc.playerController.windowClick(0, 6, i, ClickType.SWAP, mc.player);
                                            this.oldItem = i;
                                            if (this.timerUtil.hasTimeElapsed((long) timeSwap)) {
                                                if (InventoryUtils.getItemSlot(Items.FIREWORK_ROCKET) != -1) {
                                                    InventoryUtils.inventorySwapClickFF(Items.FIREWORK_ROCKET, false);
                                                } else {
                                                    OtherUtil.sendMessage(TextFormatting.WHITE + "У вас не были найдены" + TextFormatting.RED + " фейерверки");
                                                }
                                                this.timerUtil.reset();
                                            }
                                        }
                                    }
                                }
                            } else {
                                mc.player.jump();
                            }
                        }
                    }
                }
            } else if (mode.is("Шалкер")) {
                if (mc.player.isOnGround()) {
                    for (TileEntity tile : mc.world.loadedTileEntityList) {
                        if (tile instanceof ShulkerBoxTileEntity) {
                            if (mc.player.getDistanceSq(tile.getPos().getX() + 0.5, tile.getPos().getY() + 0.5, tile.getPos().getZ() + 0.5) <= 2.5) {
                                if (((ShulkerBoxTileEntity) tile).getProgress(1.0f) > 0.0f && ((ShulkerBoxTileEntity) tile).getProgress(1.0f) != 2.0f) {
                                    mc.player.motion.y = 2.33f;
                                }
                            }
                        }
                    }
                }
            }
        }

        if (event instanceof EventMotion e) {
            if (mode.is("Элитра")) {
                e.setPitch(-89);
                mc.player.rotationPitchHead = -89;
            }
        }

        if (event instanceof EventPacket e) {
            if (e.getPacket() instanceof SEntityMetadataPacket) {
                int var9 = ((SEntityMetadataPacket) e.getPacket()).getEntityId();
                if (var9 == mc.player.getEntityId()) {
                    if (!mc.player.isElytraFlying()) {
                        e.setCancel(true);
                    }
                }
            }
        }

        return false;
    }

    public void onDisable() {
        super.onDisable();
    }
}