package monoton.utils.discord.rpc.callbacks;

import com.sun.jna.Callback;
import monoton.utils.discord.rpc.utils.DiscordUser;

public interface JoinRequestCallback extends Callback {
    void apply(DiscordUser var1);
}