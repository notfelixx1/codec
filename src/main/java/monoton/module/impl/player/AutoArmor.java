package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.misc.TimerUtil;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.kotopushka.compiler.sdk.annotations.Compile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

@Annotation(name = "AutoArmor", type = TypeList.Player, desc = "Автоматический надевает броню")
public class AutoArmor extends Module {

    private final SliderSetting time = new SliderSetting("Задержка", 200.0F, 0.0F, 1000.0F, 1);

    public static TimerUtil timerUtil = new TimerUtil();

    public AutoArmor() {
        addSettings(time);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            if (mc.player == null || mc.world == null) {
                return false;
            }

            if (timerUtil.hasTimeElapsed((long) time.getValue().intValue())) {
                ItemStack[] armorSlots = new ItemStack[4];
                int[] bestIndexes = new int[4];
                int[] bestValues = new int[4];

                for (int i = 0; i < 4; ++i) {
                    armorSlots[i] = mc.player.inventory.armorItemInSlot(i);
                    bestIndexes[i] = -1;
                    if (this.isItemValid(armorSlots[i]) && armorSlots[i].getItem() instanceof ArmorItem) {
                        bestValues[i] = this.calculateArmorValue((ArmorItem) armorSlots[i].getItem(), armorSlots[i]);
                    } else {
                        bestValues[i] = -1;
                    }
                }

                for (int i = 0; i < 36; ++i) {
                    ItemStack stack = mc.player.inventory.getStackInSlot(i);
                    if (this.isItemValid(stack) && stack.getItem() instanceof ArmorItem) {
                        ArmorItem armor = (ArmorItem) stack.getItem();
                        int type = armor.getEquipmentSlot().getIndex();
                        if (type == 2 && armorSlots[2].getItem() == Items.ELYTRA) {
                            continue;
                        }
                        int value = this.calculateArmorValue(armor, stack);
                        if (value > bestValues[type]) {
                            bestIndexes[type] = i;
                            bestValues[type] = value;
                        }
                    }
                }

                ArrayList<Integer> randomIndexes = new ArrayList(Arrays.asList(0, 1, 2, 3));
                Collections.shuffle(randomIndexes);

                for (int index : randomIndexes) {
                    if (index == 2 && armorSlots[2].getItem() == Items.ELYTRA) {
                        continue;
                    }

                    if (index == 0 && armorSlots[0].getItem() == Items.PLAYER_HEAD) {
                        continue;
                    }

                    int bestIndex = bestIndexes[index];
                    if (bestIndex != -1 && (!this.isItemValid(armorSlots[index]) || mc.player.inventory.getFirstEmptyStack() != -1)) {
                        int slot = bestIndex < 9 ? bestIndex + 36 : bestIndex;
                        if (this.isItemValid(armorSlots[index])) {
                            mc.playerController.windowClick(0, 8 - index, 0, ClickType.QUICK_MOVE, mc.player);
                        }

                        mc.playerController.windowClick(0, slot, 0, ClickType.QUICK_MOVE, mc.player);
                        timerUtil.reset();
                        break;
                    }
                }
            }
        }

        return false;
    }

    private boolean isItemValid(ItemStack stack) {
        return stack != null && !stack.isEmpty();
    }

    private int calculateArmorValue(ArmorItem armor, ItemStack stack) {
        int protectionLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.PROTECTION, stack);
        int damageReductionAmount = armor.getArmorMaterial().getDamageReductionAmount(armor.getEquipmentSlot());
        float toughness = armor.getArmorMaterial().getToughness();
        return armor.getDamageReduceAmount() * 20 + protectionLevel * 12 + (int) (toughness * 2.0F) + damageReductionAmount * 5 >> 3;
    }
}