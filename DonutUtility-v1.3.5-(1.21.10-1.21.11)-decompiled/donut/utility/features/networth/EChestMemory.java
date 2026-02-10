package donut.utility.features.networth;

import donut.utility.DonutUtilityClient;
import donut.utility.features.networth.NetworthCalculator;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1799;
import net.minecraft.class_310;

@Environment(value=EnvType.CLIENT)
public class EChestMemory {
    private static final List<class_1799> ITEMS = new ArrayList<class_1799>();
    private static boolean known = false;

    private EChestMemory() {
    }

    public static boolean isKnown() {
        return known;
    }

    public static void setContents(List<class_1799> contents) {
        class_310 client = class_310.method_1551();
        if (!client.method_18854()) {
            client.execute(() -> EChestMemory.setContentsInternal(contents));
        } else {
            EChestMemory.setContentsInternal(contents);
        }
    }

    private static void setContentsInternal(List<class_1799> contents) {
        boolean wasKnown = known;
        known = true;
        ITEMS.clear();
        for (class_1799 stack : contents) {
            ITEMS.add(stack.method_7972());
        }
        DonutUtilityClient.LOGGER.info("[EChestMemory] Stored {} slots", (Object)ITEMS.size());
        if (!wasKnown && DonutUtilityClient.isNetworthEnabled()) {
            DonutUtilityClient.LOGGER.info("[EChestMemory] First echest capture, triggering networth scan...");
            NetworthCalculator calculator = DonutUtilityClient.getNetworthCalculator();
            if (calculator != null) {
                calculator.onEchestUpdated();
            }
        }
    }

    public static class_1799 getStack(int slot) {
        if (slot < 0 || slot >= ITEMS.size()) {
            return class_1799.field_8037;
        }
        return ITEMS.get(slot);
    }

    public static List<class_1799> getContents() {
        ArrayList<class_1799> copy = new ArrayList<class_1799>(ITEMS.size());
        for (class_1799 stack : ITEMS) {
            copy.add(stack.method_7972());
        }
        return copy;
    }

    public static int getSize() {
        return ITEMS.size();
    }

    public static void clear() {
        known = false;
        ITEMS.clear();
    }
}
