package web;

import spi.settings.PropertiesSpecification;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UtopiaWSPropertiesConfig implements PropertiesSpecification {
    public static final String WEB_SERVICE_API = "Core.WebServiceAPI.Enabled";

    private final Map<String, String> defaults = new HashMap<>();

    public UtopiaWSPropertiesConfig() {
        defaults.put(WEB_SERVICE_API, "false");
    }

    @Override
    public Path getFilePath() {
        return Paths.get("ws.properties");
    }

    @Override
    public Map<String, String> getDefaults() {
        return Collections.unmodifiableMap(defaults);
    }
}
