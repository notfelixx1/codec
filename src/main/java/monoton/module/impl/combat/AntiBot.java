package monoton.module.impl.combat;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.control.events.player.EventWorldChanged;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.ArrayList;
import java.util.List;

@Annotation(
        name = "AntiBot",
        type = TypeList.Combat,
        desc = "Удаляет бота с сервера"
)
public class AntiBot extends Module {
    public static List<Entity> isBot = new ArrayList<>();
    public static List<PlayerEntity> entitiesToRemove = new ArrayList<>();

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            for (PlayerEntity entity : mc.world.getPlayers()) {
                if (mc.player != entity
                        && entity.inventory.armorInventory.get(0).getItem() != Items.AIR
                        && entity.inventory.armorInventory.get(1).getItem() != Items.AIR
                        && entity.inventory.armorInventory.get(2).getItem() != Items.AIR
                        && entity.inventory.armorInventory.get(3).getItem() != Items.AIR
                        && entity.inventory.armorInventory.get(0).isEnchantable()
                        && entity.inventory.armorInventory.get(1).isEnchantable()
                        && entity.inventory.armorInventory.get(2).isEnchantable()
                        && entity.inventory.armorInventory.get(3).isEnchantable()
                        && entity.getHeldItemOffhand().getItem() == Items.AIR
                        && (
                        entity.inventory.armorInventory.get(0).getItem() == Items.LEATHER_BOOTS
                                || entity.inventory.armorInventory.get(1).getItem() == Items.LEATHER_LEGGINGS
                                || entity.inventory.armorInventory.get(2).getItem() == Items.LEATHER_CHESTPLATE
                                || entity.inventory.armorInventory.get(3).getItem() == Items.LEATHER_HELMET
                                || entity.inventory.armorInventory.get(0).getItem() == Items.IRON_BOOTS
                                || entity.inventory.armorInventory.get(1).getItem() == Items.IRON_LEGGINGS
                                || entity.inventory.armorInventory.get(2).getItem() == Items.IRON_CHESTPLATE
                                || entity.inventory.armorInventory.get(3).getItem() == Items.IRON_HELMET
                )
                        && entity.getHeldItemMainhand().getItem() != Items.AIR
                        && !entity.inventory.armorInventory.get(0).isDamaged()
                        && !entity.inventory.armorInventory.get(1).isDamaged()
                        && !entity.inventory.armorInventory.get(2).isDamaged()
                        && !entity.inventory.armorInventory.get(3).isDamaged()) {
                    if (!isBot.contains(entity)) {
                        isBot.add(entity);
                        this.removeBotFromWorld(entity);
                    }

                    return false;
                }

                if (isBot.contains(entity)) {
                    isBot.remove(entity);
                }
            }

            entitiesToRemove.clear();
        }
        if (event instanceof EventWorldChanged) {
            entitiesToRemove.clear();
        }
        return false;
    }

    private void removeBotFromWorld(PlayerEntity entity) {
        entitiesToRemove.add(entity);
        mc.world.removeEntity(entity);
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && isBot.contains(entity);
    }

    @Override
    public void onDisable() {
        isBot.clear();
        entitiesToRemove.clear();
        super.onDisable();
    }
}


