package monoton.module.impl.player;

import monoton.control.events.client.Event;
import monoton.control.events.packet.EventPacket;
import monoton.control.events.player.EventUpdate;
import monoton.module.TypeList;
import monoton.module.api.Annotation;
import monoton.module.api.Module;
import monoton.module.settings.imp.ModeSetting;
import monoton.module.settings.imp.SliderSetting;
import monoton.utils.IMinecraft;
import monoton.utils.misc.TimerUtil;
import monoton.utils.other.JoinerUtils;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.util.Hand;
import net.minecraft.util.text.TextFormatting;

@Annotation(name = "ServerJoiner", type = TypeList.Player, desc = "Автоматически заходит на сервер")
public class ServerJoiner extends Module {
    public final ModeSetting mode = new ModeSetting("Режим", "ReallyWorld", "ReallyWorld", "Spooky дуэли");
    private final SliderSetting griefSelection = new SliderSetting("Гриферский мир", 1, 1, 54, 1).setVisible(() -> mode.is("ReallyWorld"));
    private final SliderSetting speed = new SliderSetting("Скорость", 3, 1, 10, 1).setVisible(() -> mode.is("ReallyWorld"));
    private final TimerUtil timerUtil = new TimerUtil();

    public ServerJoiner() {
        addSettings(mode, griefSelection, speed);
    }

    @Override
    protected void onEnable() {
        JoinerUtils.selectCompass();
        IMinecraft.mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        super.onEnable();
    }

    @Override
    public boolean onEvent(Event event) {
        if (event instanceof EventUpdate) {
            handleEventUpdate();
        }
        if (event instanceof EventPacket eventPacket) {
            if (eventPacket.getPacket() instanceof SJoinGamePacket) {
                try {
                    if (IMinecraft.mc.ingameGUI.getTabList().header == null) {
                        return false;
                    }
                    String string = TextFormatting.getTextWithoutFormattingCodes(IMinecraft.mc.ingameGUI.getTabList().header.getString());
                    if (!string.contains("Lobby")) {
                        return false;
                    }
                } catch (Exception ignored) {
                }
            }
            if (eventPacket.getPacket() instanceof SChatPacket packet) {
                String message = TextFormatting.getTextWithoutFormattingCodes(packet.getChatComponent().getString());
                if (message.contains("К сожалению сервер переполнен") || message.contains("Подождите 20 секунд!") || message.contains("большой поток игроков")) {
                    JoinerUtils.selectCompass();
                    IMinecraft.mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                }
            }
        }
        return false;
    }

    private void handleEventUpdate() {
        if (IMinecraft.mc.currentScreen == null) {
            if (IMinecraft.mc.player.ticksExisted < 3) {
                IMinecraft.mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
            }
        } else if (IMinecraft.mc.currentScreen instanceof ChestScreen) {
            try {
                ContainerScreen container = (ContainerScreen) IMinecraft.mc.currentScreen;
                for (int i = 0; i < container.getContainer().inventorySlots.size(); i++) {
                    String s = container.getContainer().inventorySlots.get(i).getStack().getDisplayName().getString();
                    if (mode.is("ReallyWorld")) {
                        int numberGrief = griefSelection.getValue().intValue();
                        if (s.contains("ГРИФЕРСКОЕ ВЫЖИВАНИЕ") || s.contains("ГРИФ #" + numberGrief + " (1.16.5+)")) {
                            if (timerUtil.hasTimeElapsed(speed.getValue().intValue())) {
                                IMinecraft.mc.playerController.windowClick(IMinecraft.mc.player.openContainer.windowId, i, 0, ClickType.PICKUP, IMinecraft.mc.player);
                                timerUtil.reset();
                            }
                        }
                    } else if (mode.is("Spooky дуэли")) {
                        if (s.contains("Дуэли")) {
                            if (timerUtil.hasTimeElapsed(speed.getValue().intValue())) {
                                IMinecraft.mc.playerController.windowClick(IMinecraft.mc.player.openContainer.windowId, i, 0, ClickType.PICKUP, IMinecraft.mc.player);
                                timerUtil.reset();
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }
}