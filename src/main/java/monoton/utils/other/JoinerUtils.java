package monoton.utils.other;

import monoton.utils.IMinecraft;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import monoton.utils.world.InventoryUtils;


public class JoinerUtils {
    public static void selectCompass() {
        if (IMinecraft.mc.player.getHeldItemMainhand().getItem() != Items.COMPASS) {
            for (int i = 0; i < 9; i++) {
                if (IMinecraft.mc.player.inventory.getStackInSlot(i).getItem() == Items.COMPASS) {
                    IMinecraft.mc.player.inventory.currentItem = i;
                    break;
                }
            }
        }
    }
}