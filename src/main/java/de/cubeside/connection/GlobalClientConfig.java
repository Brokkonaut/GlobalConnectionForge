package de.cubeside.connection;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class GlobalClientConfig {
    public static final ForgeConfigSpec.ConfigValue<String> hostname;
    public static final ConfigValue<Integer> port;
    public static final ForgeConfigSpec.ConfigValue<String> user;
    public static final ForgeConfigSpec.ConfigValue<String> password;

    public static final ForgeConfigSpec GENERAL_SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        hostname = builder.define("hostname", "localhost");
        port = builder.define("port", 25701);
        user = builder.define("user", "CHANGEME");
        password = builder.define("password", "CHANGEME");

        GENERAL_SPEC = builder.build();
    }
}
