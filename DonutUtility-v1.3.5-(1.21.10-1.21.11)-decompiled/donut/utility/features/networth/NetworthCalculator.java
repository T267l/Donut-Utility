package donut.utility.features.networth;

import donut.utility.DonutUtilityClient;
import donut.utility.features.networth.AHPriceCache;
import donut.utility.features.networth.EChestMemory;
import donut.utility.features.networth.InventoryScanner;
import donut.utility.features.networth.ItemData;
import donut.utility.features.networth.SpawnerPriceCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class NetworthCalculator {
    private InventoryScanner.ScanResult lastScan;
    private NetworthResult lastResult;
    private boolean needsRecalculation = true;
    private boolean scanRequested = false;
    private boolean retrySent = false;

    public void scan() {
        if (!SpawnerPriceCache.isLoaded() && !SpawnerPriceCache.isLoading()) {
            SpawnerPriceCache.fetchPrices();
        }
        this.lastScan = InventoryScanner.scanAll();
        AHPriceCache.prefetchPrices(this.lastScan);
        this.needsRecalculation = true;
        this.scanRequested = true;
        this.retrySent = false;
        AHPriceCache.setOnPricesComplete(this::onCalculationComplete);
    }

    private void onCalculationComplete() {
        if (this.retrySent) {
            return;
        }
        this.retrySent = true;
        NetworthResult result = this.calculate();
        AHPriceCache.sendRetryRequest(result.totalNetworth, true);
        DonutUtilityClient.LOGGER.info("[Networth] Calculation complete, retry sent with networth: {}", (Object)result.getFormattedTotal());
    }

    public void onDisconnect() {
        Long b;
        if (this.retrySent) {
            return;
        }
        this.retrySent = true;
        long balance = 0L;
        balance = this.lastResult != null ? this.lastResult.balance : ((b = DonutUtilityClient.getShowMoney().getLocalPlayerBalance()) != null ? b : 0L);
        AHPriceCache.sendRetryRequest(balance, false);
    }

    public void onEchestUpdated() {
        AHPriceCache.clear();
        this.scan();
    }

    public void requestRecalculation() {
        AHPriceCache.clear();
        this.needsRecalculation = true;
        this.retrySent = false;
        if (this.lastScan != null) {
            AHPriceCache.prefetchPrices(this.lastScan);
            AHPriceCache.setOnPricesComplete(this::onCalculationComplete);
        }
    }

    public void clearState() {
        this.lastScan = null;
        this.lastResult = null;
        this.needsRecalculation = true;
        this.scanRequested = false;
        this.retrySent = false;
    }

    public NetworthResult calculate() {
        int echestCount;
        int inventoryCount;
        long unitPrice;
        int count;
        if (this.lastScan == null) {
            NetworthResult empty = new NetworthResult();
            empty.echestKnown = EChestMemory.isKnown();
            empty.waitingForEchest = !EChestMemory.isKnown();
            return empty;
        }
        boolean allLoaded = AHPriceCache.allPricesLoaded(this.lastScan);
        NetworthResult result = new NetworthResult();
        result.isLoading = !allLoaded;
        result.isDone = allLoaded && this.scanRequested;
        result.echestKnown = EChestMemory.isKnown();
        result.waitingForEchest = false;
        result.loadingProgress = AHPriceCache.getProgress();
        Long balance = DonutUtilityClient.getShowMoney().getLocalPlayerBalance();
        result.balance = balance != null ? balance : 0L;
        result.balanceLoading = DonutUtilityClient.getShowMoney().isLocalPlayerBalanceLoading();
        for (Map.Entry<ItemData, Integer> entry : this.lastScan.itemCounts.entrySet()) {
            ItemData item = entry.getKey();
            count = entry.getValue();
            unitPrice = AHPriceCache.getPrice(item);
            inventoryCount = this.lastScan.inventoryItemCounts.getOrDefault(item, 0);
            echestCount = this.lastScan.echestItemCounts.getOrDefault(item, 0);
            ItemValue itemValue = new ItemValue();
            itemValue.item = item;
            itemValue.count = count;
            itemValue.unitPrice = unitPrice;
            if (unitPrice > 0L) {
                itemValue.totalValue = unitPrice * (long)count;
                result.itemsValue += itemValue.totalValue;
                ++result.pricedItemCount;
                result.inventoryItemsValue += unitPrice * (long)inventoryCount;
                result.echestItemsValue += unitPrice * (long)echestCount;
            } else if (unitPrice == -1L) {
                itemValue.totalValue = 0L;
                ++result.unpricedItemCount;
            } else {
                itemValue.totalValue = 0L;
                ++result.pendingItemCount;
            }
            result.items.add(itemValue);
        }
        for (Map.Entry<Object, Integer> entry : this.lastScan.spawnerCounts.entrySet()) {
            String spawnerType = (String)entry.getKey();
            count = entry.getValue();
            unitPrice = SpawnerPriceCache.getPrice(spawnerType);
            inventoryCount = this.lastScan.inventorySpawnerCounts.getOrDefault(spawnerType, 0);
            echestCount = this.lastScan.echestSpawnerCounts.getOrDefault(spawnerType, 0);
            SpawnerValue spawner = new SpawnerValue();
            spawner.type = spawnerType;
            spawner.count = count;
            spawner.unitPrice = unitPrice;
            if (unitPrice > 0L) {
                spawner.totalValue = unitPrice * (long)count;
                result.spawnersValue += spawner.totalValue;
                ++result.pricedSpawnerCount;
                result.inventorySpawnersValue += unitPrice * (long)inventoryCount;
                result.echestSpawnersValue += unitPrice * (long)echestCount;
            } else {
                spawner.totalValue = 0L;
                ++result.unpricedSpawnerCount;
            }
            result.spawners.add(spawner);
        }
        result.inventoryValue = result.inventoryItemsValue + result.inventorySpawnersValue;
        result.echestValue = result.echestItemsValue + result.echestSpawnersValue;
        result.itemsValue += result.spawnersValue;
        result.totalNetworth = result.inventoryValue + result.echestValue + result.balance;
        result.items.sort((a, b) -> {
            if (a.unitPrice == -2L && b.unitPrice != -2L) {
                return 1;
            }
            if (b.unitPrice == -2L && a.unitPrice != -2L) {
                return -1;
            }
            return Long.compare(b.totalValue, a.totalValue);
        });
        result.spawners.sort((a, b) -> Long.compare(b.totalValue, a.totalValue));
        this.lastResult = result;
        this.needsRecalculation = false;
        return result;
    }

    public NetworthResult getLastResult() {
        return this.calculate();
    }

    public boolean hasScan() {
        return this.lastScan != null;
    }

    @Environment(value=EnvType.CLIENT)
    public static class NetworthResult {
        public boolean isLoading = false;
        public boolean isDone = false;
        public boolean echestKnown = false;
        public boolean waitingForEchest = false;
        public boolean balanceLoading = false;
        public int loadingProgress = 0;
        public long inventoryValue = 0L;
        public long echestValue = 0L;
        public long balance = 0L;
        public long totalNetworth = 0L;
        public long inventoryItemsValue = 0L;
        public long inventorySpawnersValue = 0L;
        public long echestItemsValue = 0L;
        public long echestSpawnersValue = 0L;
        public long itemsValue = 0L;
        public long spawnersValue = 0L;
        public int pricedItemCount = 0;
        public int unpricedItemCount = 0;
        public int pendingItemCount = 0;
        public int pricedSpawnerCount = 0;
        public int unpricedSpawnerCount = 0;
        public List<ItemValue> items = new ArrayList<ItemValue>();
        public List<SpawnerValue> spawners = new ArrayList<SpawnerValue>();

        public String getFormattedTotal() {
            return NetworthResult.formatPrice(this.totalNetworth);
        }

        public String getFormattedInventoryValue() {
            return NetworthResult.formatPrice(this.inventoryValue);
        }

        public String getFormattedEchestValue() {
            return NetworthResult.formatPrice(this.echestValue);
        }

        public String getFormattedBalance() {
            if (this.balanceLoading) {
                return "...";
            }
            return NetworthResult.formatPrice(this.balance);
        }

        public String getStatusText() {
            if (this.waitingForEchest) {
                return "Waiting for echest...";
            }
            if (this.isDone && !this.isLoading) {
                return "Done!";
            }
            if (this.isLoading) {
                return "Loading... " + this.loadingProgress + "%";
            }
            return "";
        }

        private static String formatPrice(long price) {
            if (price >= 1000000000L) {
                return String.format("%.1fB$", (double)price / 1.0E9);
            }
            if (price >= 1000000L) {
                return String.format("%.1fM$", (double)price / 1000000.0);
            }
            if (price >= 1000L) {
                return String.format("%.1fk$", (double)price / 1000.0);
            }
            return price + "$";
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class ItemValue {
        public ItemData item;
        public int count;
        public long unitPrice;
        public long totalValue;

        public String getFormattedUnitPrice() {
            return AHPriceCache.formatPrice(this.unitPrice);
        }

        public String getFormattedTotalValue() {
            if (this.unitPrice == -2L) {
                return "...";
            }
            if (this.unitPrice <= 0L) {
                return "N/A";
            }
            return AHPriceCache.formatPrice(this.totalValue);
        }

        public boolean isPending() {
            return this.unitPrice == -2L;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static class SpawnerValue {
        public String type;
        public int count;
        public long unitPrice;
        public long totalValue;

        public String getFormattedUnitPrice() {
            return SpawnerPriceCache.formatPrice(this.unitPrice);
        }

        public String getFormattedTotalValue() {
            if (this.unitPrice <= 0L) {
                return "N/A";
            }
            return SpawnerPriceCache.formatPrice(this.totalValue);
        }

        public boolean hasPricing() {
            return this.unitPrice > 0L;
        }
    }
}
