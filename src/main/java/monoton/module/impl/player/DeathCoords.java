package monoton.module.impl.player;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.util.text.TextFormatting;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.TypeList;
import monoton.utils.other.OtherUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;


@Annotation(name = "DeathCoords", type = TypeList.Player, desc = "Пишет вам кординаты смерти")
public class DeathCoords extends Module {

    
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            checkDeathCoordinates();
        }
        return false;
    }


    
    public void checkDeathCoordinates() {
        if (isPlayerDead()) {
            int positionX = mc.player.getPosition().getX();
            int positionY = mc.player.getPosition().getY();
            int positionZ = mc.player.getPosition().getZ();

            if (mc.player.deathTime < 1) {
                printDeathCoordinates(positionX, positionY, positionZ);
            }
        }
    }

    
    private boolean isPlayerDead() {
        return mc.player.getHealth() < 1.0f && mc.currentScreen instanceof DeathScreen;
    }

    
    private void printDeathCoordinates(int x, int y, int z) {
        String message = "Координаты смерти: " + TextFormatting.RED + "X: " + x + " Y: " + y + " Z: " + z + TextFormatting.RESET;
        OtherUtil.sendMessage(message);
    }
}
