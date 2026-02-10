package donut.utility.features.networth;

import donut.utility.DonutUtilityClient;
import donut.utility.features.networth.EChestMemory;
import donut.utility.features.networth.ItemData;
import donut.utility.features.networth.ValuableItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1304;
import net.minecraft.class_1747;
import net.minecraft.class_1792;
import net.minecraft.class_1799;
import net.minecraft.class_2480;
import net.minecraft.class_310;
import net.minecraft.class_9288;
import net.minecraft.class_9334;

@Environment(value=EnvType.CLIENT)
public class InventoryScanner {
    private InventoryScanner() {
    }

    public static ScanResult scanAll() {
        ScanResult result = new ScanResult();
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) {
            return result;
        }
        DonutUtilityClient.LOGGER.info("[Networth] Starting inventory scan...");
        int invItems = 0;
        for (int i = 0; i < 36; ++i) {
            class_1304[] stack = client.field_1724.method_31548().method_5438(i);
            if (stack.method_7960()) continue;
            ++invItems;
            InventoryScanner.processStack((class_1799)stack, result, Source.INVENTORY);
        }
        class_1799 offhand = client.field_1724.method_6079();
        if (!offhand.method_7960()) {
            InventoryScanner.processStack(offhand, result, Source.INVENTORY);
        }
        for (class_1304 slot : new class_1304[]{class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166}) {
            class_1799 armorStack = client.field_1724.method_6118(slot);
            if (armorStack.method_7960()) continue;
            InventoryScanner.processStack(armorStack, result, Source.INVENTORY);
        }
        if (EChestMemory.isKnown()) {
            int echestItems = 0;
            List<class_1799> echestContents = EChestMemory.getContents();
            DonutUtilityClient.LOGGER.info("[Networth] Scanning echest with {} slots", (Object)echestContents.size());
            for (class_1799 stack : echestContents) {
                if (stack.method_7960()) continue;
                ++echestItems;
                InventoryScanner.processStack(stack, result, Source.ECHEST);
            }
            DonutUtilityClient.LOGGER.info("[Networth] Scanned echest: {} non-empty items", (Object)echestItems);
        } else {
            DonutUtilityClient.LOGGER.info("[Networth] Echest not yet opened, skipping");
        }
        DonutUtilityClient.LOGGER.info("[Networth] Scan complete: {} unique item types, {} spawner types", (Object)result.itemCounts.size(), (Object)result.spawnerCounts.size());
        return result;
    }

    private static void processStack(class_1799 stack, ScanResult result, Source source) {
        if (stack == null || stack.method_7960()) {
            return;
        }
        if (InventoryScanner.isShulker(stack)) {
            InventoryScanner.scanShulkerContents(stack, result, source);
        } else {
            InventoryScanner.addToResult(stack, result, source);
        }
    }

    private static boolean isShulker(class_1799 stack) {
        class_1792 class_17922 = stack.method_7909();
        if (class_17922 instanceof class_1747) {
            class_1747 blockItem = (class_1747)class_17922;
            return blockItem.method_7711() instanceof class_2480;
        }
        return false;
    }

    private static void scanShulkerContents(class_1799 shulkerStack, ScanResult result, Source source) {
        class_9288 container = (class_9288)shulkerStack.method_58694(class_9334.field_49622);
        if (container == null) {
            return;
        }
        for (class_1799 stack : container.method_59714()) {
            if (InventoryScanner.isShulker(stack)) continue;
            InventoryScanner.addToResult(stack, result, source);
        }
    }

    private static void addToResult(class_1799 stack, ScanResult result, Source source) {
        ItemData data = new ItemData(stack);
        int count = stack.method_7947();
        if (!ValuableItems.isValuable(data)) {
            return;
        }
        if (data.isSpawner()) {
            String spawnerType = data.getSpawnerType();
            result.spawnerCounts.merge(spawnerType, count, Integer::sum);
            if (source == Source.ECHEST) {
                result.echestSpawnerCounts.merge(spawnerType, count, Integer::sum);
            } else {
                result.inventorySpawnerCounts.merge(spawnerType, count, Integer::sum);
            }
        } else {
            result.itemCounts.merge(data, count, Integer::sum);
            if (source == Source.ECHEST) {
                result.echestItemCounts.merge(data, count, Integer::sum);
            } else {
                result.inventoryItemCounts.merge(data, count, Integer::sum);
            }
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ScanResult {
        public final Map<ItemData, Integer> itemCounts = new HashMap<ItemData, Integer>();
        public final Map<String, Integer> spawnerCounts = new HashMap<String, Integer>();
        public final Map<ItemData, Integer> inventoryItemCounts = new HashMap<ItemData, Integer>();
        public final Map<String, Integer> inventorySpawnerCounts = new HashMap<String, Integer>();
        public final Map<ItemData, Integer> echestItemCounts = new HashMap<ItemData, Integer>();
        public final Map<String, Integer> echestSpawnerCounts = new HashMap<String, Integer>();

        public boolean isEmpty() {
            return this.itemCounts.isEmpty() && this.spawnerCounts.isEmpty();
        }

        public int getTotalItemTypes() {
            return this.itemCounts.size();
        }

        public int getTotalSpawnerTypes() {
            return this.spawnerCounts.size();
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static enum Source {
        INVENTORY,
        ECHEST;

    }
}
