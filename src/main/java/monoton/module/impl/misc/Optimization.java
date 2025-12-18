package monoton.module.impl.misc;

import monoton.control.Manager;
import monoton.control.events.client.Event;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.BooleanOption;
import monoton.module.settings.imp.MultiBoxSetting;
import net.minecraft.block.*;

@Annotation(name = "Optimization", type = TypeList.Misc, desc = "Удаляет всякое что бы оптимизировать вашу игру")
public class Optimization extends Module {
    public final MultiBoxSetting optimizeSelection = new MultiBoxSetting("Оптимизировать", new BooleanOption("Растения", true), new BooleanOption("Партиклы", true), new BooleanOption("Облака", true), new BooleanOption("Графику неба", true), new BooleanOption("Энтити", true));

    public Optimization() {
        addSettings(optimizeSelection);
    }

    @Override
    public boolean onEvent(Event event) {
        if (optimizeSelection.get("Облака")) {
            mc.gameSettings.ofSky = false;
        }
        if (optimizeSelection.get("Графику неба")) {
            mc.gameSettings.ofCustomSky = false;
        }
        if (optimizeSelection.get("Энтити")) {
            mc.gameSettings.entityShadows = false;
        }
        return false;
    }

    public boolean canRender(Block block) {
        return (!(block instanceof TallGrassBlock)
                && !(block instanceof FlowerBlock)
                && !(block instanceof DoublePlantBlock)
                && !(block instanceof DeadBushBlock)
                && !(block instanceof MushroomBlock)
                && !(block instanceof CropsBlock)
                && !(block instanceof NetherSproutsBlock)) || (!optimizeSelection.get(0) || !Manager.FUNCTION_MANAGER.optimization.state);
    }

    public void onDisable() {
        super.onDisable();
        mc.gameSettings.ofSky = true;
        mc.gameSettings.ofCustomSky = true;
        mc.gameSettings.entityShadows = true;
    }
}