package monoton.module.impl.player;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.*;
import net.minecraft.util.math.MathHelper;
import monoton.control.events.client.Event;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(
        name = "AutoPilot", type = TypeList.Player, desc = "Наводит ваш прицел на ценные предметы"
)
public class AutoPilot extends Module {
    public MultiBoxSetting element = new MultiBoxSetting("Наводится на",
            new BooleanOption("Шар", true),
            new BooleanOption("Элитра", true),
            new BooleanOption("Осколок", true),
            new BooleanOption("Острота VI", false),
            new BooleanOption("Анти полёт", false),
            new BooleanOption("Аура", true));
    public final BooleanOption fall = new BooleanOption("Искл ауру падение", true).setVisible(() -> element.get("Аура"));

    public AutoPilot() {
        addSettings(element, fall);
    }

    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            for (Entity entity : mc.world.getAllEntities()) {
                if (entity instanceof ItemEntity itemEntity) {
                    ItemStack itemStack = itemEntity.getItem();
                    String displayName = itemStack.getDisplayName().getString();

                    if (element.get(0) && itemStack.getItem() instanceof SkullItem) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(1) && itemStack.getItem() instanceof ElytraItem) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(2) && itemStack.getItem() == Items.GHAST_TEAR && displayName.contains("Осколок")) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(3) && (itemStack.getItem() instanceof SwordItem || itemStack.getItem() instanceof AxeItem) && EnchantmentHelper.getEnchantmentLevel(Enchantments.SHARPNESS, itemStack) >= 6) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(4) && itemStack.getItem() == Items.FIREWORK_STAR && displayName.contains("Анти Полёт")) {
                        mc.player.rotationYaw = this.rotations(entity)[0];
                        mc.player.rotationPitch = this.rotations(entity)[1];
                    }
                    if (element.get(5)) {
                        if ((itemStack.getItem() == Items.SUNFLOWER && displayName.contains("Аура Охотника")) ||
                                (itemStack.getItem() == Items.CLAY_BALL && displayName.contains("Аура Твёрдости Брони")) ||
                                (itemStack.getItem() == Items.POPPED_CHORUS_FRUIT && displayName.contains("Аура Телепортации")) ||
                                (itemStack.getItem() == Items.GOLD_NUGGET && displayName.contains("Аура Богача")) ||
                                (itemStack.getItem() == Items.WHITE_DYE && displayName.contains("Аура Защиты От Падения") && !fall.getValue()) ||
                                (itemStack.getItem() == Items.GHAST_TEAR && displayName.contains("Аура Защиты От Кристаллов"))) {
                            mc.player.rotationYaw = this.rotations(entity)[0];
                            mc.player.rotationPitch = this.rotations(entity)[1];
                        }
                    }
                }
            }
        }

        return false;
    }

    public float[] rotations(Entity entity) {
        double x = entity.getPosX() - mc.player.getPosX();
        double y = entity.getPosY() - mc.player.getPosY() - 1F;
        double z = entity.getPosZ() - mc.player.getPosZ();
        double u = (double) MathHelper.sqrt(x * x + z * z);
        float u2 = (float) (MathHelper.atan2(z, x) * 57.29577951308232 - 90.0);
        float u3 = (float) (-MathHelper.atan2(y, u) * 57.29577951308232);
        return new float[]{u2, u3};
    }
}