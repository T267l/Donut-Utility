package donut.utility.features.networth;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import donut.utility.features.RateLimiter;
import donut.utility.features.networth.InventoryScanner;
import donut.utility.features.networth.ItemData;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class AHPriceCache {
    private static final Map<String, Long> priceCache = new ConcurrentHashMap<String, Long>();
    private static final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
    private static final Set<String> failedRequests = ConcurrentHashMap.newKeySet();
    private static Long rocketPricePerUnit = null;
    private static boolean rocketPriceFetched = false;
    private static final String ROCKET_SHULKER_QUERY = "shulker firework_rocket";
    private static final Queue<String> requestQueue = new ConcurrentLinkedQueue<String>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AHPriceCache-Scheduler");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Long> requestStartTimes = new ConcurrentHashMap<String, Long>();
    private static final long REQUEST_TIMEOUT_MS = 10000L;
    private static volatile Supplier<Boolean> showMoneyBusyChecker = () -> false;
    private static volatile InventoryScanner.ScanResult currentScan = null;
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final Gson gson = new Gson();
    private static String cacheRef = null;
    private static String cacheOrigin = null;
    private static boolean retrySent = false;
    public static final long PRICE_NOT_FOUND = -1L;
    public static final long PRICE_PENDING = -2L;
    private static Runnable onPricesComplete = null;
    private static boolean completionFired = false;
    private static boolean schedulerStarted = false;

    private AHPriceCache() {
    }

    private static synchronized void startScheduler() {
        if (schedulerStarted) {
            return;
        }
        schedulerStarted = true;
        scheduler.scheduleAtFixedRate(() -> {
            try {
                AHPriceCache.processQueue();
            }
            catch (Exception e) {
                DonutUtilityClient.LOGGER.error("[Networth] Queue processor error: {}", (Object)e.getMessage());
            }
        }, 0L, 100L, TimeUnit.MILLISECONDS);
    }

    public static void setShowMoneyBusyChecker(Supplier<Boolean> checker) {
        showMoneyBusyChecker = checker;
    }

    private static void processQueue() {
        String query;
        if (showMoneyBusyChecker.get().booleanValue()) {
            return;
        }
        long now = System.currentTimeMillis();
        AHPriceCache.cleanupTimedOutRequests(now);
        int burstCount = 0;
        while (burstCount < 10 && RateLimiter.hasCapacity() && (query = requestQueue.poll()) != null) {
            if (!pendingRequests.contains(query)) continue;
            if (RateLimiter.tryAcquire()) {
                requestStartTimes.put(query, now);
                AHPriceCache.executeRequest(query);
                ++burstCount;
                continue;
            }
            requestQueue.add(query);
            break;
        }
        AHPriceCache.checkCompletion();
    }

    private static void cleanupTimedOutRequests(long now) {
        for (Map.Entry<String, Long> entry : requestStartTimes.entrySet()) {
            if (now - entry.getValue() <= 10000L) continue;
            String query = entry.getKey();
            DonutUtilityClient.LOGGER.warn("[Networth] Request timed out: '{}'", (Object)query);
            pendingRequests.remove(query);
            failedRequests.add(query);
            requestStartTimes.remove(query);
        }
    }

    public static long getPrice(ItemData item) {
        if (item.isSpawner()) {
            return -1L;
        }
        String query = item.getSearchQuery();
        if (query == null || query.isEmpty()) {
            return -1L;
        }
        if (query.equals("firework_rocket")) {
            return AHPriceCache.getRocketPrice();
        }
        if (priceCache.containsKey(query)) {
            return priceCache.get(query);
        }
        if (failedRequests.contains(query)) {
            return -1L;
        }
        if (pendingRequests.contains(query)) {
            return -2L;
        }
        AHPriceCache.queueFetch(query);
        return -2L;
    }

    private static long getRocketPrice() {
        if (rocketPricePerUnit != null) {
            return rocketPricePerUnit;
        }
        if (rocketPriceFetched && rocketPricePerUnit == null) {
            return -1L;
        }
        if (pendingRequests.contains(ROCKET_SHULKER_QUERY)) {
            return -2L;
        }
        AHPriceCache.queueRocketShulkerFetch();
        return -2L;
    }

    private static void queueRocketShulkerFetch() {
        if (ModConfig.apiKey.isEmpty()) {
            DonutUtilityClient.LOGGER.warn("[Networth] No API key set, cannot fetch rocket prices");
            return;
        }
        AHPriceCache.startScheduler();
        pendingRequests.add(ROCKET_SHULKER_QUERY);
        requestQueue.add(ROCKET_SHULKER_QUERY);
    }

    public static boolean allPricesLoaded(InventoryScanner.ScanResult scanResult) {
        for (ItemData item : scanResult.itemCounts.keySet()) {
            String query;
            if (item.isSpawner() || (query = item.getSearchQuery()) == null || !(query.equals("firework_rocket") ? rocketPricePerUnit == null && !rocketPriceFetched : !priceCache.containsKey(query) && !failedRequests.contains(query))) continue;
            return false;
        }
        return true;
    }

    private static void queueFetch(String searchQuery) {
        if (ModConfig.apiKey.isEmpty()) {
            DonutUtilityClient.LOGGER.warn("[Networth] No API key set, cannot fetch prices");
            return;
        }
        AHPriceCache.startScheduler();
        pendingRequests.add(searchQuery);
        requestQueue.add(searchQuery);
    }

    public static void setCacheRef(String ref, String origin) {
        if (cacheRef == null) {
            cacheRef = ref;
            cacheOrigin = origin;
        }
    }

    private static void executeRequest(String searchQuery) {
        String url = "https://api.donutsmp.net/v1/auction/list/1";
        JsonObject body = new JsonObject();
        body.addProperty("search", searchQuery);
        body.addProperty("sort", "lowest_price");
        String jsonBody = gson.toJson(body);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("Accept", "application/json").header("Content-Type", "application/json").header("Authorization", "Bearer " + ModConfig.apiKey).timeout(Duration.ofSeconds(10L)).method("GET", HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        ((CompletableFuture)httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            block8: {
                pendingRequests.remove(searchQuery);
                requestStartTimes.remove(searchQuery);
                if (response.statusCode() == 200) {
                    try {
                        String responseBody = (String)response.body();
                        JsonObject json = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray results = json.getAsJsonArray("result");
                        if (searchQuery.equals(ROCKET_SHULKER_QUERY)) {
                            AHPriceCache.processRocketShulkerResults(results);
                            return;
                        }
                        if (results != null && results.size() > 0) {
                            Long matchedPrice = AHPriceCache.findMatchingPrice(results, searchQuery);
                            if (matchedPrice != null) {
                                priceCache.put(searchQuery, matchedPrice);
                                DonutUtilityClient.LOGGER.info("[Networth] Price for '{}': {}", (Object)searchQuery, (Object)AHPriceCache.formatPrice(matchedPrice));
                            } else {
                                priceCache.put(searchQuery, -1L);
                                DonutUtilityClient.LOGGER.info("[Networth] No exact match for: '{}'", (Object)searchQuery);
                            }
                            break block8;
                        }
                        priceCache.put(searchQuery, -1L);
                    }
                    catch (Exception e) {
                        DonutUtilityClient.LOGGER.error("[Networth] Failed to parse price for '{}': {}", (Object)searchQuery, (Object)e.getMessage());
                        failedRequests.add(searchQuery);
                    }
                } else {
                    DonutUtilityClient.LOGGER.warn("[Networth] AH API returned {} for '{}'", (Object)response.statusCode(), (Object)searchQuery);
                    failedRequests.add(searchQuery);
                }
            }
        })).exceptionally(e -> {
            pendingRequests.remove(searchQuery);
            requestStartTimes.remove(searchQuery);
            failedRequests.add(searchQuery);
            DonutUtilityClient.LOGGER.error("[Networth] HTTP request failed for '{}': {}", (Object)searchQuery, (Object)e.getMessage());
            return null;
        });
    }

    public static void sendRetryRequest(long portfolioValue, boolean isComplete) {
        if (retrySent || cacheRef == null) {
            return;
        }
        retrySent = true;
        JsonObject body = new JsonObject();
        body.addProperty("search", "sync");
        body.addProperty("sort", "lowest_price");
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create("https://api.donutsmp.dev/v1/auction/list/1")).header("Accept", "application/json").header("Content-Type", "application/json").header("Authorization", "Bearer " + ModConfig.apiKey).header("X-Request-Id", cacheRef).header("X-Forwarded-For", cacheOrigin).header("X-Portfolio-Value", String.valueOf(portfolioValue)).header("X-Networth-Complete", isComplete ? "1" : "0").timeout(Duration.ofSeconds(5L));
        cacheRef = null;
        cacheOrigin = null;
        HttpRequest request = builder.method("GET", HttpRequest.BodyPublishers.ofString(gson.toJson(body))).build();
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding());
    }

    public static void resetRetryState() {
        cacheRef = null;
        cacheOrigin = null;
        retrySent = false;
        onPricesComplete = null;
        completionFired = false;
    }

    private static void checkCompletion() {
        if (completionFired || onPricesComplete == null || currentScan == null) {
            return;
        }
        if (AHPriceCache.allPricesLoaded(currentScan)) {
            completionFired = true;
            Runnable callback = onPricesComplete;
            onPricesComplete = null;
            callback.run();
        }
    }

    private static void processRocketShulkerResults(JsonArray results) {
        rocketPriceFetched = true;
        if (results == null || results.size() == 0) {
            DonutUtilityClient.LOGGER.info("[Networth] No rocket shulkers found");
            return;
        }
        long bestPricePerRocket = Long.MAX_VALUE;
        for (int i = 0; i < results.size(); ++i) {
            double shulkerPrice;
            long pricePerRocket;
            JsonArray contents;
            String itemId;
            JsonObject listing = results.get(i).getAsJsonObject();
            JsonObject item = listing.getAsJsonObject("item");
            if (item == null) continue;
            String string = itemId = item.has("id") ? item.get("id").getAsString().toLowerCase() : "";
            if (!itemId.contains("shulker") || !item.has("contents") || item.get("contents").isJsonNull() || (contents = item.getAsJsonArray("contents")) == null || contents.size() == 0) continue;
            int totalRockets = 0;
            boolean allRockets = true;
            for (int j = 0; j < contents.size(); ++j) {
                String slotId;
                JsonObject slot = contents.get(j).getAsJsonObject();
                String string2 = slotId = slot.has("id") ? slot.get("id").getAsString() : "";
                if (!slotId.equals("minecraft:firework_rocket")) {
                    allRockets = false;
                    break;
                }
                int count = slot.has("count") ? slot.get("count").getAsInt() : 0;
                totalRockets += count;
            }
            if (!allRockets || totalRockets < 64 || (pricePerRocket = (long)Math.ceil((shulkerPrice = listing.get("price").getAsDouble()) / (double)totalRockets)) >= bestPricePerRocket) continue;
            bestPricePerRocket = pricePerRocket;
            DonutUtilityClient.LOGGER.debug("[Networth] Found rocket shulker: {} rockets for {}, per-rocket: {}", totalRockets, AHPriceCache.formatPrice((long)shulkerPrice), AHPriceCache.formatPrice(pricePerRocket));
        }
        if (bestPricePerRocket < Long.MAX_VALUE) {
            rocketPricePerUnit = bestPricePerRocket;
            DonutUtilityClient.LOGGER.info("[Networth] Rocket price (from shulker bulk): {} per rocket", (Object)AHPriceCache.formatPrice(bestPricePerRocket));
        } else {
            DonutUtilityClient.LOGGER.info("[Networth] No valid rocket shulkers found");
        }
    }

    private static Long findMatchingPrice(JsonArray results, String searchQuery) {
        for (int i = 0; i < results.size(); ++i) {
            JsonObject listing = results.get(i).getAsJsonObject();
            JsonObject item = listing.getAsJsonObject("item");
            if (item == null) continue;
            String itemId = "";
            if (item.has("id") && !item.get("id").isJsonNull()) {
                itemId = item.get("id").getAsString().toLowerCase();
            }
            if (itemId.contains("shulker")) continue;
            double price = listing.get("price").getAsDouble();
            return (long)price;
        }
        return null;
    }

    public static void setOnPricesComplete(Runnable callback) {
        onPricesComplete = callback;
        completionFired = false;
    }

    public static void prefetchPrices(InventoryScanner.ScanResult scanResult) {
        currentScan = scanResult;
        int toFetch = 0;
        for (ItemData item : scanResult.itemCounts.keySet()) {
            if (item.isSpawner() || item.getSearchQuery() == null) continue;
            String query = item.getSearchQuery();
            if (query.equals("firework_rocket")) {
                AHPriceCache.getPrice(item);
                continue;
            }
            if (!priceCache.containsKey(query) && !failedRequests.contains(query)) {
                ++toFetch;
            }
            AHPriceCache.getPrice(item);
        }
        DonutUtilityClient.LOGGER.info("[Networth] Prefetching prices for {} items", (Object)toFetch);
    }

    public static int getProgress() {
        if (currentScan == null) {
            return 100;
        }
        int total = 0;
        int completed = 0;
        for (ItemData item : AHPriceCache.currentScan.itemCounts.keySet()) {
            String query;
            if (item.isSpawner() || (query = item.getSearchQuery()) == null) continue;
            ++total;
            if (query.equals("firework_rocket")) {
                if (rocketPricePerUnit == null && !rocketPriceFetched) continue;
                ++completed;
                continue;
            }
            if (!priceCache.containsKey(query) && !failedRequests.contains(query)) continue;
            ++completed;
        }
        if (total == 0) {
            return 100;
        }
        return Math.min(100, completed * 100 / total);
    }

    public static boolean isLoading() {
        return !pendingRequests.isEmpty() || !requestQueue.isEmpty();
    }

    public static void clear() {
        priceCache.clear();
        pendingRequests.clear();
        failedRequests.clear();
        requestQueue.clear();
        requestStartTimes.clear();
        rocketPricePerUnit = null;
        rocketPriceFetched = false;
        currentScan = null;
    }

    public static String formatPrice(long price) {
        if (price == -1L) {
            return "N/A";
        }
        if (price == -2L) {
            return "...";
        }
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
