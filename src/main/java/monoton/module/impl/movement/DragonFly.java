package monoton.module.impl.movement;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventMove;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.TypeList;
import monoton.utils.move.MoveUtil;
import monoton.utils.other.OtherUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "DragonFly", type = TypeList.Movement, desc = "Ускоряет ваш полёт")
public class DragonFly extends Module {

    public DragonFly() {
        addSettings();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventMove move) {
            handleDragonFly(move);
        }
        return false;
    }

    @Compile
    private void handleDragonFly(EventMove move) {
        if (mc.player.abilities.isFlying) {
            if (OtherUtil.isConnectedToServer("cakeworld")) {
                if (!mc.player.isSneaking() && mc.gameSettings.keyBindJump.isKeyDown()) {
                    if (MoveUtil.isMoving()) {
                        move.motion().y = 1.2f;
                    } else {
                        move.motion().y = 1.2f;
                    }
                }
                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    if (MoveUtil.isMoving()) {
                        move.motion().y = -1.2f;
                    } else {
                        move.motion().y = -1.2f;
                    }
                }
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.109399f);
                } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.109399f);
                } else {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.111f);
                }
            } else {
                if (!mc.player.isSneaking() && mc.gameSettings.keyBindJump.isKeyDown()) {
                    if (MoveUtil.isMoving()) {
                        move.motion().y = 0.49f;
                    } else {
                        move.motion().y = 1.191f;
                    }
                }
                if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    if (MoveUtil.isMoving()) {
                        move.motion().y = -0.49f;
                    } else {
                        move.motion().y = -1.191f;
                    }
                }
                if (mc.gameSettings.keyBindJump.isKeyDown()) {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.095399f);
                } else if (mc.gameSettings.keyBindSneak.isKeyDown()) {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.095399f);
                } else {
                    MoveUtil.MoveEvent.setMoveMotion(move, 1.1725f);
                }
            }
        }
    }
}
