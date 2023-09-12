package de.cubeside.connection;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
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

    public GlobalClientMod() {
        ModLoadingContext.get().registerConfig(Type.SERVER, GlobalClientConfig.GENERAL_SPEC);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        globalClient = new GlobalClientForge(this, event.getServer());
        globalClient.setServer(GlobalClientConfig.hostname.get(), GlobalClientConfig.port.get(), GlobalClientConfig.user.get(), GlobalClientConfig.password.get());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        if (globalClient != null) {
            globalClient.shutdown();
            globalClient = null;
        }
    }
}
