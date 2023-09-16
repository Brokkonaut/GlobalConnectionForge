package de.cubeside.connection;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import org.slf4j.Logger;

@Mod(GlobalClientMod.MODID)
public class GlobalClientMod {
    public static final String MODID = "globalconnectionforge";

    public static final Logger LOGGER = LogUtils.getLogger();
    private GlobalClientForge globalClient;

    private PlayerMessageImplementation messageAPI;

    private PlayerPropertiesImplementation propertiesAPI;
    private static GlobalClientMod instance;

    public GlobalClientMod() {
        instance = this;
        ModLoadingContext.get().registerConfig(Type.SERVER, GlobalClientConfig.GENERAL_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerAboutToStartEvent event) {
        globalClient = new GlobalClientForge(this, event.getServer());
        globalClient.setServer(GlobalClientConfig.hostname.get(), GlobalClientConfig.port.get(), GlobalClientConfig.user.get(), GlobalClientConfig.password.get());

        messageAPI = new PlayerMessageImplementation(this, event.getServer());
        propertiesAPI = new PlayerPropertiesImplementation(this, event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (globalClient != null) {
            globalClient.shutdown();
            globalClient = null;
        }
    }

    public GlobalClientForge getConnectionAPI() {
        return globalClient;
    }

    public PlayerMessageImplementation getMessageAPI() {
        return messageAPI;
    }

    public PlayerPropertiesImplementation getPropertiesAPI() {
        return propertiesAPI;
    }

    public static GlobalClientMod getInstance() {
        return instance;
    }
}
