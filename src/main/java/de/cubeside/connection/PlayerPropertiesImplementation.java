package de.cubeside.connection;

import com.google.common.base.Preconditions;
import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.event.GlobalPlayerDisconnectedEvent;
import de.cubeside.connection.event.GlobalPlayerPropertyChangedEvent;
import de.cubeside.connection.event.GlobalServerConnectedEvent;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

class PlayerPropertiesImplementation implements PlayerPropertiesAPI {

    private final static int MESSAGE_SET_PROPERTY = 1;
    private final static int MESSAGE_DELETE_PROPERTY = 2;
    private final static int MESSAGE_MULTISET_PROPERTIES = 3;

    private final GlobalClientMod plugin;
    private final MinecraftServer server;

    private final static String CHANNEL = "GlobalClient.playerProperties";

    private final HashMap<UUID, HashMap<String, String>> playerProperties;

    public PlayerPropertiesImplementation(GlobalClientMod plugin, MinecraftServer server) {
        this.plugin = plugin;
        this.server = server;
        this.playerProperties = new HashMap<>();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onGlobalPlayerDisconnected(GlobalPlayerDisconnectedEvent e) {
        if (e.hasJustLeftTheNetwork()) {
            playerProperties.remove(e.getPlayer().getUniqueId());
        }
    }

    @SubscribeEvent
    public void onGlobalServerConnected(GlobalServerConnectedEvent e) {
        // send all properties
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeByte(MESSAGE_MULTISET_PROPERTIES);
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                UUID uuid = p.getUUID();
                HashMap<String, String> properties = playerProperties.get(uuid);
                if (properties != null) {
                    dos.writeBoolean(true);
                    dos.writeLong(uuid.getMostSignificantBits());
                    dos.writeLong(uuid.getLeastSignificantBits());
                    dos.writeInt(properties.size());
                    for (Entry<String, String> entry : properties.entrySet()) {
                        dos.writeUTF(entry.getKey());
                        dos.writeUTF(entry.getValue());
                    }
                }
            }
            dos.writeBoolean(false);
            dos.close();
        } catch (IOException ex) {
            throw new Error("impossible");
        }
        e.getServer().sendData(CHANNEL, baos.toByteArray());
    }

    @SubscribeEvent
    public void onGlobalData(GlobalDataEvent e) {
        if (e.getChannel().equals(CHANNEL)) {
            DataInputStream dis = new DataInputStream(e.getData());
            try {
                int type = dis.readByte();
                if (type == MESSAGE_SET_PROPERTY) {
                    UUID uuid = readUUID(dis);
                    GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                    String property = dis.readUTF();
                    String value = dis.readUTF();
                    HashMap<String, String> properties = playerProperties.get(uuid);
                    if (properties == null) {
                        properties = new HashMap<>();
                        playerProperties.put(uuid, properties);
                    }
                    properties.put(property, value);
                    MinecraftForge.EVENT_BUS.post(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, value));
                } else if (type == MESSAGE_DELETE_PROPERTY) {
                    UUID uuid = readUUID(dis);
                    GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                    String property = dis.readUTF();
                    HashMap<String, String> properties = playerProperties.get(uuid);
                    if (properties != null) {
                        properties.remove(property);
                        if (properties.isEmpty()) {
                            playerProperties.remove(uuid);
                        }
                        MinecraftForge.EVENT_BUS.post(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, null));
                    }
                } else if (type == MESSAGE_MULTISET_PROPERTIES) {
                    while (dis.readBoolean()) {
                        UUID uuid = readUUID(dis);
                        GlobalPlayer target = plugin.getConnectionAPI().getPlayer(uuid);
                        int propertiesCount = dis.readInt();
                        if (propertiesCount > 0) {
                            HashMap<String, String> properties = playerProperties.get(uuid);
                            if (properties == null) {
                                properties = new HashMap<>();
                                playerProperties.put(uuid, properties);
                            }
                            for (int i = 0; i < propertiesCount; i++) {
                                String property = dis.readUTF();
                                String value = dis.readUTF();
                                properties.put(property, value);
                                MinecraftForge.EVENT_BUS.post(new GlobalPlayerPropertyChangedEvent(e.getSource(), target, property, null));
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                GlobalClientMod.LOGGER.error("Could not parse PlayerProperties message", ex);
            }
        }
    }

    private UUID readUUID(DataInputStream dis) throws IOException {
        long msb = dis.readLong();
        long lsb = dis.readLong();
        return new UUID(msb, lsb);
    }

    @Override
    public boolean hasProperty(GlobalPlayer player, String property) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(property, "property");
        HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
        return properties != null && properties.containsKey(property);
    }

    @Override
    public String getPropertyValue(GlobalPlayer player, String property) {
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(property, "property");
        HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
        return properties == null ? null : properties.get(property);
    }

    @Override
    public Map<String, String> getAllProperties(GlobalPlayer player) {
        Preconditions.checkNotNull(player, "player");
        HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
        return properties == null ? Collections.emptyMap() : Collections.unmodifiableMap(properties);
    }

    @Override
    public void setPropertyValue(GlobalPlayer player, String property, String value) {
        // Preconditions.checkState(Bukkit.isPrimaryThread(), "not on main thread!");
        Preconditions.checkNotNull(player, "player");
        Preconditions.checkNotNull(property, "property");
        Preconditions.checkArgument(player.isOnAnyServer(), "player is not online");
        HashMap<String, String> properties = playerProperties.get(player.getUniqueId());
        if (value == null) {
            if (properties != null) {
                if (properties.remove(property) != null) {
                    if (properties.isEmpty()) {
                        playerProperties.remove(player.getUniqueId());
                    }
                    // send remove
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    try {
                        dos.writeByte(MESSAGE_DELETE_PROPERTY);
                        dos.writeLong(player.getUniqueId().getMostSignificantBits());
                        dos.writeLong(player.getUniqueId().getLeastSignificantBits());
                        dos.writeUTF(property);
                        dos.close();
                    } catch (IOException ex) {
                        throw new Error("impossible");
                    }
                    plugin.getConnectionAPI().sendData(CHANNEL, baos.toByteArray(), true);
                }
            }
        } else { // value != null
            if (properties == null) {
                properties = new HashMap<>();
                playerProperties.put(player.getUniqueId(), properties);
            }
            properties.put(property, value);
            // send set
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            try {
                dos.writeByte(MESSAGE_SET_PROPERTY);
                dos.writeLong(player.getUniqueId().getMostSignificantBits());
                dos.writeLong(player.getUniqueId().getLeastSignificantBits());
                dos.writeUTF(property);
                dos.writeUTF(value);
                dos.close();
            } catch (IOException ex) {
                throw new Error("impossible");
            }
            plugin.getConnectionAPI().sendData(CHANNEL, baos.toByteArray(), true);
        }
        MinecraftForge.EVENT_BUS.post(new GlobalPlayerPropertyChangedEvent(plugin.getConnectionAPI().getThisServer(), player, property, value));
    }
}
