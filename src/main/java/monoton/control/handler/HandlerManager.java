package monoton.control.handler;

import monoton.control.events.client.EventManager;
import monoton.control.handler.impl.*;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HandlerManager {

    private final List<Object> handlers = new CopyOnWriteArrayList<>();

    public void init() {
        add(new TargetESPHandler());
        add(new PickItemFixHandler());
        add(new AntiCrashMinecraftHandler());
    }

    public void add(Object handler) {
        handlers.add(handler);
        EventManager.register(handler);
    }
}
