package donut.utility;

import donut.utility.ModConfig;
import donut.utility.features.ShowMoney;
import donut.utility.features.networth.AHPriceCache;
import donut.utility.features.networth.EChestMemory;
import donut.utility.features.networth.NetworthCalculator;
import donut.utility.features.networth.NetworthConfigScreen;
import donut.utility.features.networth.NetworthHud;
import donut.utility.features.networth.NetworthOverlay;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_304;
import net.minecraft.class_310;
import net.minecraft.class_3675;
import net.minecraft.class_437;
import net.minecraft.class_642;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public class DonutUtilityClient
implements ClientModInitializer {
    public static final String MOD_ID = "donututility";
    public static final Logger LOGGER = LoggerFactory.getLogger("donututility");
    public static final String DONUT_SMP_IP = "donutsmp.net";
    private static class_304 showMoneyToggleKey;
    private static class_304 networthToggleKey;
    private static class_304 networthConfigKey;
    private static ShowMoney showMoney;
    private static NetworthCalculator networthCalculator;
    private static boolean networthEnabled;
    private static boolean checkedForApiKey;
    private static int joinDelayTicks;
    private static boolean waitingForApiResponse;

    public void onInitializeClient() {
        LOGGER.info("[DonutUtility] Initializing...");
        ModConfig.load();
        showMoney = new ShowMoney();
        networthCalculator = new NetworthCalculator();
        AHPriceCache.setShowMoneyBusyChecker(() -> showMoney.isBusy());
        class_304.class_11900 category = class_304.class_11900.method_74698((class_2960)class_2960.method_60655((String)MOD_ID, (String)"main"));
        showMoneyToggleKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.donututility.toggle_showmoney", class_3675.class_307.field_1668, 74, category));
        networthToggleKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.donututility.toggle_networth", class_3675.class_307.field_1668, 75, category));
        networthConfigKey = KeyBindingHelper.registerKeyBinding((class_304)new class_304("key.donututility.networth_config", class_3675.class_307.field_1668, 44, category));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DonutUtilityClient.onServerDisconnect());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            String status;
            while (showMoneyToggleKey.method_1436()) {
                ModConfig.enabled = !ModConfig.enabled;
                ModConfig.save();
                String string = status = ModConfig.enabled ? "\u00a7aEnabled" : "\u00a7cDisabled";
                if (client.field_1724 == null) continue;
                client.field_1724.method_7353((class_2561)class_2561.method_43470((String)("\u00a77[\u00a76DonutUtility\u00a77] \u00a7fShowMoney: " + status)), true);
            }
            while (networthToggleKey.method_1436()) {
                if (!DonutUtilityClient.isOnDonutSMP(client)) {
                    if (client.field_1724 == null) continue;
                    client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a77[\u00a76DonutUtility\u00a77] \u00a7cNetworth only works on DonutSMP!"), true);
                    continue;
                }
                networthEnabled = !networthEnabled;
                String string = status = networthEnabled ? "\u00a7aEnabled" : "\u00a7cDisabled";
                if (client.field_1724 != null) {
                    client.field_1724.method_7353((class_2561)class_2561.method_43470((String)("\u00a77[\u00a76DonutUtility\u00a77] \u00a7fNetworth Overlay: " + status)), true);
                }
                if (!networthEnabled) continue;
                if (EChestMemory.isKnown()) {
                    networthCalculator.requestRecalculation();
                    networthCalculator.scan();
                    continue;
                }
                AHPriceCache.clear();
                networthCalculator.clearState();
                if (client.field_1724 == null) continue;
                client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a77[\u00a76DonutUtility\u00a77] \u00a7eOpen your echest to start scanning!"), false);
            }
            while (networthConfigKey.method_1436()) {
                if (!DonutUtilityClient.isOnDonutSMP(client)) {
                    if (client.field_1724 == null) continue;
                    client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a77[\u00a76DonutUtility\u00a77] \u00a7cNetworth only works on DonutSMP!"), true);
                    continue;
                }
                client.method_1507((class_437)new NetworthConfigScreen());
            }
            this.handleAutoApiSetup(client);
            if (ModConfig.enabled && DonutUtilityClient.isOnDonutSMP(client)) {
                showMoney.onTick(client);
            }
        });
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            class_310 client = class_310.method_1551();
            if (ModConfig.enabled && DonutUtilityClient.isOnDonutSMP(client)) {
                showMoney.onRender(context);
            }
        });
        HudElementRegistry.addLast((class_2960)class_2960.method_60655((String)MOD_ID, (String)"networth_hud"), (context, tickDelta) -> NetworthHud.render(context));
        LOGGER.info("[DonutUtility] Loaded! J=ShowMoney, K=Networth, ,=Config");
    }

    private static void onServerDisconnect() {
        if (networthEnabled && networthCalculator != null) {
            networthCalculator.onDisconnect();
        }
        AHPriceCache.resetRetryState();
        NetworthOverlay.reset();
        if (networthCalculator != null) {
            networthCalculator.clearState();
        }
        networthEnabled = false;
    }

    private void handleAutoApiSetup(class_310 client) {
        if (client.field_1687 == null || client.field_1724 == null) {
            checkedForApiKey = false;
            joinDelayTicks = 0;
            EChestMemory.clear();
            return;
        }
        if (!DonutUtilityClient.isOnDonutSMP(client)) {
            return;
        }
        if (!ModConfig.apiKey.isEmpty()) {
            return;
        }
        if (checkedForApiKey) {
            return;
        }
        if (joinDelayTicks < 60) {
            ++joinDelayTicks;
            return;
        }
        checkedForApiKey = true;
        waitingForApiResponse = true;
        client.field_1724.field_3944.method_45730("api");
        LOGGER.info("[DonutUtility] Requesting API key from server...");
    }

    public static boolean isOnDonutSMP(class_310 client) {
        if (client == null) {
            return false;
        }
        class_642 serverInfo = client.method_1558();
        if (serverInfo != null) {
            String address = serverInfo.field_3761.toLowerCase();
            return address.contains(DONUT_SMP_IP);
        }
        return false;
    }

    public static boolean isWaitingForApiResponse() {
        return waitingForApiResponse;
    }

    public static void setWaitingForApiResponse(boolean waiting) {
        waitingForApiResponse = waiting;
    }

    public static void resetApiKeyCheck() {
        checkedForApiKey = false;
        joinDelayTicks = 0;
        LOGGER.info("[DonutUtility] API key check reset, will request new key");
    }

    public static ShowMoney getShowMoney() {
        return showMoney;
    }

    public static NetworthCalculator getNetworthCalculator() {
        return networthCalculator;
    }

    public static boolean isNetworthEnabled() {
        return networthEnabled;
    }

    static {
        networthEnabled = true;
        checkedForApiKey = false;
        joinDelayTicks = 0;
        waitingForApiResponse = false;
    }
}
