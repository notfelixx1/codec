package monoton.control.events.client;

import monoton.control.Manager;
import monoton.module.api.Module;

import java.util.ArrayList;
import java.util.List;

import static monoton.utils.IMinecraft.mc;

public class EventManager {

    private static final List<Object> listeners = new ArrayList<>();

    public static void call(final Event event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (event.isCancel()) {
            return;
        }

        callEvent(event);
    }

    private static void callEvent(Event event) {
        for (final Module module : Manager.FUNCTION_MANAGER.getFunctions()) {
            if (!module.isState()) continue;
            module.onEvent(event);
        }

        for (Object listener : listeners) {
            try {
                for (var method : listener.getClass().getDeclaredMethods()) {
                    if (method.getParameterCount() == 1 &&
                            method.getParameters()[0].getType().isAssignableFrom(event.getClass())) {

                        method.setAccessible(true);
                        method.invoke(listener, event);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void register(Object listener) {
        listeners.add(listener);
    }
}
