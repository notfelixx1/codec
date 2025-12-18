package monoton.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;
import monoton.utils.discord.rpc.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser var1);
}