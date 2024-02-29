package de.cubeside.connection;

import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.util.ConnectionStringUtil;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

class PlayerMessageImplementation implements PlayerMessageAPI {

    private final static int MESSAGE_CHAT = 1;
    private final static int MESSAGE_CHAT_COMPONENTS = 2;
    private final static int MESSAGE_ACTION_BAR = 3;
    private final static int MESSAGE_TITLE = 4;

    // private final GlobalClientMod plugin;
    private final MinecraftServer server;

    private final static String CHANNEL = "GlobalClient.chat";

    public PlayerMessageImplementation(GlobalClientMod plugin, MinecraftServer server) {
        // this.plugin = plugin;
        this.server = server;
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGlobalData(GlobalDataEvent e) {
        if (e.getChannel().equals(CHANNEL)) {
            DataInputStream dis = new DataInputStream(e.getData());
            try {
                GlobalPlayer target = e.getTargetPlayer();
                if (target != null) {
                    ServerPlayer player = server.getPlayerList().getPlayer(target.getUniqueId());
                    if (player != null) {
                        int type = dis.readByte();
                        if (type == MESSAGE_CHAT) {
                            String message = dis.readUTF();
                            player.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), new UUID(0, 0));
                        } else if (type == MESSAGE_CHAT_COMPONENTS) {
                            MutableComponent message = Component.Serializer.fromJson(dis.readUTF());
                            player.sendMessage(message, new UUID(0, 0));
                        } else if (type == MESSAGE_ACTION_BAR) {
                            String message = dis.readUTF();
                            player.displayClientMessage(null, false);
                            player.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), new UUID(0, 0)); //TODO ACTIONBAR
                        } else if (type == MESSAGE_TITLE) {
                            int flags = dis.readByte();
                            String title = ((flags & 1) != 0) ? dis.readUTF() : null;
                            String subtitle = ((flags & 2) != 0) ? dis.readUTF() : null;
                            int fadeInTicks = dis.readInt();
                            int durationTicks = dis.readInt();
                            int fadeOutTicks = dis.readInt();
                            // times, subtitle, title
                            sendTitleToPlayer(player, title, subtitle, fadeInTicks, durationTicks, fadeOutTicks);
                        }
                    }
                }
            } catch (IOException ex) {
                GlobalClientMod.LOGGER.error("Could not parse PlayerMessage message", ex);
            }
        }
    }

    private void sendTitleToPlayer(ServerPlayer player, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks) {
        // times, subtitle, title
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeInTicks, durationTicks, fadeOutTicks));
        player.connection.send(new ClientboundSetSubtitleTextPacket(ConnectionStringUtil.parseLegacyColoredString(subtitle)));
        player.connection.send(new ClientboundSetTitleTextPacket(ConnectionStringUtil.parseLegacyColoredString(title)));
    }

    @Override
    public void sendMessage(GlobalPlayer player, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_CHAT);
            dos.writeUTF(message);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        ServerPlayer p = server.getPlayerList().getPlayer(player.getUniqueId());
        if (p != null) {
            p.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), new UUID(0, 0));
        }
    }

    @Override
    public void sendMessage(GlobalPlayer player, Component message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_CHAT_COMPONENTS);
            dos.writeUTF(Component.Serializer.toJson(message));
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        ServerPlayer p = server.getPlayerList().getPlayer(player.getUniqueId());
        if (p != null) {
            p.sendMessage(message, new UUID(0, 0));
        }
    }

    public static void main(String[] args) {
        System.out.println(FormattedText.of("§3hi!"));
    }

    @Override
    public void sendActionBarMessage(GlobalPlayer player, String message) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_ACTION_BAR);
            dos.writeUTF(message);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        ServerPlayer p = server.getPlayerList().getPlayer(player.getUniqueId());
        if (p != null) {
            p.sendMessage(ConnectionStringUtil.parseLegacyColoredString(message), new UUID(0, 0)); //TODO Actionbar
        }
    }

    @Override
    public void sendTitleBarMessage(GlobalPlayer player, String title, String subtitle, int fadeInTicks, int durationTicks, int fadeOutTicks) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_TITLE);
            int flags = (title != null ? 1 : 0) | (subtitle != null ? 2 : 0);
            dos.writeByte(flags);
            if (title != null) {
                dos.writeUTF(title);
            }
            if (subtitle != null) {
                dos.writeUTF(subtitle);
            }
            dos.writeInt(fadeInTicks);
            dos.writeInt(durationTicks);
            dos.writeInt(fadeOutTicks);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        player.sendData(CHANNEL, baos.toByteArray());
        ServerPlayer p = server.getPlayerList().getPlayer(player.getUniqueId());
        if (p != null) {
            sendTitleToPlayer(p, title, subtitle, fadeInTicks, durationTicks, fadeOutTicks);
        }
    }

}
