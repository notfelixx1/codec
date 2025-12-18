package monoton.module.impl.render;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.control.events.render.EventRender;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.MultiBoxSetting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;

@Annotation(name = "CustomModel", type = TypeList.Render, desc = "Изменяет модельку игрока")
public class CustomModel extends Module {
    public final ModeSetting mode = new ModeSetting("Мод", "Заяц", "Заяц", "Соник", "Зелёная броня");

    public final MultiBoxSetting entity = new MultiBoxSetting("Выбор целей",
            new BooleanOption("Игроки", false),
            new BooleanOption("Себя", true),
            new BooleanOption("Друзья", true)
    ).setVisible(() -> !mode.is("Зелёная броня"));

    public CustomModel() {
        addSettings(mode, entity);
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventRender && mode.is("Зелёная броня")) {
            try {
                for(PlayerEntity entity : mc.world.getPlayers()) {
                    if (entity != mc.player && Manager.FRIEND_MANAGER.isFriend(entity.getName().getString())) {
                        ItemStack itemStack = Items.LEATHER_BOOTS.getDefaultInstance();
                        ItemStack itemStack1 = Items.LEATHER_LEGGINGS.getDefaultInstance();
                        ItemStack itemStack2 = Items.LEATHER_CHESTPLATE.getDefaultInstance();
                        ItemStack itemStack3 = Items.LEATHER_HELMET.getDefaultInstance();
                        CompoundNBT nbt = itemStack.getOrCreateTag();
                        CompoundNBT displayTag = nbt.getCompound("display");
                        displayTag.putInt("color", 65280);
                        nbt.put("display", displayTag);
                        itemStack.setTag(nbt);
                        entity.inventory.armorInventory.set(0, itemStack);
                        CompoundNBT nbt1 = itemStack1.getOrCreateTag();
                        CompoundNBT displayTag1 = nbt1.getCompound("display");
                        displayTag1.putInt("color", 65280);
                        nbt1.put("display", displayTag);
                        itemStack1.setTag(nbt1);
                        entity.inventory.armorInventory.set(1, itemStack1);
                        CompoundNBT nbt2 = itemStack2.getOrCreateTag();
                        CompoundNBT displayTag2 = nbt2.getCompound("display");
                        displayTag2.putInt("color", 65280);
                        nbt2.put("display", displayTag);
                        itemStack2.setTag(nbt2);
                        entity.inventory.armorInventory.set(2, itemStack2);
                        CompoundNBT nbt3 = itemStack3.getOrCreateTag();
                        CompoundNBT displayTag3 = nbt3.getCompound("display");
                        displayTag3.putInt("color", 65280);
                        nbt3.put("display", displayTag);
                        itemStack3.setTag(nbt3);
                        entity.inventory.armorInventory.set(3, itemStack3);
                    }
                }
            } catch (Exception var16) {
            }
        }

        return false;
    }
}
