package de.cubeside.connection;

import java.util.Map;

public interface PlayerPropertiesAPI {
    public boolean hasProperty(GlobalPlayer player, String property);

    public String getPropertyValue(GlobalPlayer player, String property);

    public Map<String, String> getAllProperties(GlobalPlayer player);

    public void setPropertyValue(GlobalPlayer player, String property, String value);

    public static PlayerPropertiesAPI getInstance() {
        return GlobalClientMod.getInstance().getPropertiesAPI();
    }
}
