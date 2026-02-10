package donut.utility;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import donut.utility.DonutUtilityClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(value=EnvType.CLIENT)
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("donututility.json");
    public static boolean enabled = true;
    public static String apiKey = "";
    public static boolean showSelf = true;
    public static boolean debugLog = false;
    public static double scale = 1.0;
    public static double yOffset = 0.3;
    public static int normalColor = -11141291;
    public static int billionaireColor = -10496;
    public static boolean networthHudEnabled = true;
    public static int networthHudX = -1;
    public static int networthHudY = -1;
    public static boolean showSelfBalance = true;
    public static boolean auraEnabled = true;

    public static void load() {
        try {
            if (Files.exists(CONFIG_PATH, new LinkOption[0])) {
                String json = Files.readString(CONFIG_PATH);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null) {
                    enabled = data.enabled;
                    apiKey = data.apiKey;
                    showSelf = data.showSelf;
                    debugLog = data.debugLog;
                    scale = data.scale;
                    yOffset = data.yOffset;
                    normalColor = data.normalColor;
                    billionaireColor = data.billionaireColor;
                    networthHudEnabled = data.networthHudEnabled;
                    networthHudX = data.networthHudX;
                    networthHudY = data.networthHudY;
                    showSelfBalance = data.showSelfBalance;
                    auraEnabled = data.auraEnabled;
                }
                DonutUtilityClient.LOGGER.info("[DonutUtility] Config loaded!");
            } else {
                ModConfig.save();
                DonutUtilityClient.LOGGER.info("[DonutUtility] Created default config file.");
            }
        }
        catch (IOException e) {
            DonutUtilityClient.LOGGER.error("[DonutUtility] Failed to load config: {}", (Object)e.getMessage());
        }
    }

    public static void save() {
        try {
            ConfigData data = new ConfigData();
            data.enabled = enabled;
            data.apiKey = apiKey;
            data.showSelf = showSelf;
            data.debugLog = debugLog;
            data.scale = scale;
            data.yOffset = yOffset;
            data.normalColor = normalColor;
            data.billionaireColor = billionaireColor;
            data.networthHudEnabled = networthHudEnabled;
            data.networthHudX = networthHudX;
            data.networthHudY = networthHudY;
            data.showSelfBalance = showSelfBalance;
            data.auraEnabled = auraEnabled;
            Files.createDirectories(CONFIG_PATH.getParent(), new FileAttribute[0]);
            Files.writeString(CONFIG_PATH, (CharSequence)GSON.toJson(data), new OpenOption[0]);
        }
        catch (IOException e) {
            DonutUtilityClient.LOGGER.error("[DonutUtility] Failed to save config: {}", (Object)e.getMessage());
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class ConfigData {
        boolean enabled = true;
        String apiKey = "";
        boolean showSelf = true;
        boolean debugLog = false;
        double scale = 1.0;
        double yOffset = 0.3;
        int normalColor = -11141291;
        int billionaireColor = -10496;
        boolean networthHudEnabled = true;
        int networthHudX = -1;
        int networthHudY = -1;
        boolean showSelfBalance = true;
        boolean auraEnabled = true;

        private ConfigData() {
        }
    }
}
