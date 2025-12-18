package monoton.module.impl.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import monoton.control.events.client.Event;
import monoton.control.events.game.EventKey;
import monoton.control.Manager;
import monoton.module.api.Module;
import monoton.module.api.Annotation;
import monoton.module.TypeList;
import monoton.module.settings.imp.BindSetting;
import monoton.utils.other.OtherUtil;
import monoton.utils.misc.TimerUtil;
import ru.kotopushka.compiler.sdk.annotations.Compile;

@Annotation(name = "ClickFriend", type = TypeList.Player, desc = "Добавляет друга при нажатие на клавишу")
public class ClickFriend extends Module {
    private final TimerUtil timerUtil = new TimerUtil();

    private BindSetting clickKey = new BindSetting("Кнопка", -98);

    public ClickFriend() {
        addSettings(clickKey);
    }
    
    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventKey e) {
            if (e.key == clickKey.getKey()) {
                handleKeyPressEvent();
            }
        }
        return false;
    }
    
    private void handleKeyPressEvent() {
        if (timerUtil.hasTimeElapsed(50L) && mc.pointedEntity instanceof PlayerEntity) {
            String entityName = mc.pointedEntity.getName().getString();
            if (Manager.FRIEND_MANAGER.isFriend(entityName)) {
                Manager.FRIEND_MANAGER.removeFriend(entityName);
                displayRemoveFriendMessage(entityName);
            } else {
                Manager.FRIEND_MANAGER.addFriend(entityName);
                displayAddFriendMessage(entityName);
            }
            timerUtil.reset();
        }
    }
    
    private void displayRemoveFriendMessage(String friendName) {
        OtherUtil.sendMessage(TextFormatting.RESET + "Удалил " + TextFormatting.RED + friendName + TextFormatting.RESET + " из друзей");
    }
    
    private void displayAddFriendMessage(String friendName) {
        OtherUtil.sendMessage(TextFormatting.RESET + "Добавил " + TextFormatting.GREEN + friendName + TextFormatting.RESET + " в друзья");
    }
}