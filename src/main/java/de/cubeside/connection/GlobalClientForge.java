package de.cubeside.connection;

import de.cubeside.connection.event.GlobalDataEvent;
import de.cubeside.connection.event.GlobalPlayerDisconnectedEvent;
import de.cubeside.connection.event.GlobalPlayerJoinedEvent;
import de.cubeside.connection.event.GlobalServerConnectedEvent;
import de.cubeside.connection.event.GlobalServerDisconnectedEvent;
import java.util.ArrayDeque;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class GlobalClientForge extends GlobalClient {
    // private final GlobalClientMod plugin;
    private boolean stoppingServer;

    protected final ArrayDeque<Runnable> tasks = new ArrayDeque<>();
    protected final Object sync = new Object();
    protected boolean running = true;
    private MinecraftServer minecraftServer;

    private static GlobalClientForge instance;

    public GlobalClientForge(GlobalClientMod connectionPlugin, MinecraftServer minecraftServer) {
        super(null);

        // plugin = connectionPlugin;
        this.minecraftServer = minecraftServer;

        MinecraftForge.EVENT_BUS.register(this);

        instance = this;
    }

    @Override
    public void setServer(String host, int port, String account, String password) {
        GlobalClientForge.super.setServer(host, port, account, password);
        for (ServerPlayer p : minecraftServer.getPlayerList().getPlayers()) {
            onPlayerOnline(p.getUUID(), p.getName().getString(), System.currentTimeMillis());
        }
    }

    @Override
    protected void runInMainThread(Runnable r) {
        if (!stoppingServer) {
            minecraftServer.execute(r);
        }
    }

    @Override
    protected void processData(GlobalServer source, String channel, GlobalPlayer targetPlayer, GlobalServer targetServer, byte[] data) {
        // GlobalClientMod.LOGGER.debug("processData: " + channel);
        MinecraftForge.EVENT_BUS.post(new GlobalDataEvent(source, targetPlayer, channel, data));
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerLoggedInEvent e) {
        Player p = e.getEntity();
        // GlobalClientMod.LOGGER.debug("Player join: " + p.getName().getString());
        GlobalPlayer existing = getPlayer(p.getUUID());
        if (existing == null || !existing.isOnServer(getThisServer())) {
            onPlayerOnline(p.getUUID(), p.getName().getString(), System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onPlayerQuit(PlayerLoggedOutEvent e) {
        Player p = e.getEntity();
        // GlobalClientMod.LOGGER.debug("Player quit: " + p.getName().getString());
        GlobalPlayer existing = getPlayer(p.getUUID());
        if (existing != null && existing.isOnServer(getThisServer())) {
            onPlayerOffline(p.getUUID());
        }
    }

    @Override
    protected void onPlayerJoined(GlobalServer server, GlobalPlayer player, boolean joinedTheNetwork) {
        // GlobalClientMod.LOGGER.debug("onPlayerJoined: " + player.getName());
        MinecraftForge.EVENT_BUS.post(new GlobalPlayerJoinedEvent(server, player, joinedTheNetwork));
    }

    @Override
    protected void onPlayerDisconnected(GlobalServer server, GlobalPlayer player, boolean leftTheNetwork) {
        // GlobalClientMod.LOGGER.debug("onPlayerDisconnected: " + player.getName());
        MinecraftForge.EVENT_BUS.post(new GlobalPlayerDisconnectedEvent(server, player, leftTheNetwork));
    }

    @Override
    protected void onServerConnected(GlobalServer server) {
        // GlobalClientMod.LOGGER.debug("onServerConnected: " + server.getName());
        MinecraftForge.EVENT_BUS.post(new GlobalServerConnectedEvent(server));
    }

    @Override
    protected void onServerDisconnected(GlobalServer server) {
        // GlobalClientMod.LOGGER.debug("onServerDisconnected: " + server.getName());
        MinecraftForge.EVENT_BUS.post(new GlobalServerDisconnectedEvent(server));
    }

    @Override
    public void shutdown() {
        this.stoppingServer = true;
        super.shutdown();
        synchronized (sync) {
            running = false;
            sync.notifyAll();
        }
    }

    public static GlobalClientForge getInstance() {
        return instance;
    }
}