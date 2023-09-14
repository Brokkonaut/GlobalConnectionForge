package de.cubeside.connection;

import net.minecraft.network.chat.Component;

public interface PlayerMessageAPI {
    public void sendMessage(GlobalPlayer player, String message);

    public void sendMessage(GlobalPlayer player, Component message);

    public void sendActionBarMessage(GlobalPlayer player, String message);

    public void sendTitleBarMessage(GlobalPlayer player, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks);

    public static PlayerMessageAPI getInstance() {
        return GlobalClientMod.getInstance().getMessageAPI();
    }
}
