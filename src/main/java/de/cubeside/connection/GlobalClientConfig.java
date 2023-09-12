package de.cubeside.connection;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class GlobalClientConfig {
    public static ForgeConfigSpec.ConfigValue<String> hostname;
    public static ConfigValue<Integer> port;
    public static ForgeConfigSpec.ConfigValue<String> user;
    public static ForgeConfigSpec.ConfigValue<String> password;

    public static final ForgeConfigSpec GENERAL_SPEC;

    static {
        ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
        setupConfig(configBuilder);
        GENERAL_SPEC = configBuilder.build();
    }

    private static void setupConfig(ForgeConfigSpec.Builder builder) {
        hostname = builder.define("hostname", "localhost");
        port = builder.define("port", 25701);
        user = builder.define("user", "CHANGEME");
        password = builder.define("password", "CHANGEME");
    }
}
