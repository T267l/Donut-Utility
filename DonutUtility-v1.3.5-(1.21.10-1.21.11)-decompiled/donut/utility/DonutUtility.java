package donut.utility;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonutUtility
implements ModInitializer {
    public static final String MOD_ID = "donututility";
    public static final Logger LOGGER = LoggerFactory.getLogger("donututility");

    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
    }
}
