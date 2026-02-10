package donut.utility.features.networth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import donut.utility.DonutUtilityClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class SpawnerPriceCache {
    private static final String PRICES_URL = "https://raw.githubusercontent.com/stashya/donutsmp-leaderboard/main/spawner_prices.json";
    private static final Map<String, Long> priceCache = new ConcurrentHashMap<String, Long>();
    private static boolean loaded = false;
    private static boolean loading = false;
    private static final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final Gson gson = new Gson();

    private SpawnerPriceCache() {
    }

    public static void fetchPrices() {
        if (loaded || loading) {
            return;
        }
        loading = true;
        DonutUtilityClient.LOGGER.info("[SpawnerPriceCache] Fetching spawner prices from GitHub...");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(PRICES_URL)).header("accept", "application/json").timeout(Duration.ofSeconds(15L)).GET().build();
        ((CompletableFuture)httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() == 200) {
                try {
                    JsonObject json = gson.fromJson((String)response.body(), JsonObject.class);
                    JsonObject prices = json.getAsJsonObject("prices");
                    if (prices != null && prices.has("buying")) {
                        JsonObject buying = prices.getAsJsonObject("buying");
                        for (String spawnerType : buying.keySet()) {
                            JsonObject priceData = buying.getAsJsonObject(spawnerType);
                            String priceStr = priceData.get("price").getAsString();
                            long price = SpawnerPriceCache.parsePrice(priceStr);
                            if (price <= 0L) continue;
                            priceCache.put(spawnerType.toLowerCase(), price);
                            DonutUtilityClient.LOGGER.info("[SpawnerPriceCache] {} Spawner = {}", (Object)spawnerType, (Object)SpawnerPriceCache.formatPrice(price));
                        }
                    }
                    loaded = true;
                    DonutUtilityClient.LOGGER.info("[SpawnerPriceCache] Loaded {} spawner prices", (Object)priceCache.size());
                }
                catch (Exception e) {
                    DonutUtilityClient.LOGGER.error("[SpawnerPriceCache] Failed to parse prices: {}", (Object)e.getMessage());
                }
            } else {
                DonutUtilityClient.LOGGER.warn("[SpawnerPriceCache] Failed to fetch prices: HTTP {}", (Object)response.statusCode());
            }
            loading = false;
        })).exceptionally(e -> {
            DonutUtilityClient.LOGGER.error("[SpawnerPriceCache] Fetch failed: {}", (Object)e.getMessage());
            loading = false;
            return null;
        });
    }

    private static long parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) {
            return 0L;
        }
        if ((priceStr = priceStr.toLowerCase().trim()).contains("-")) {
            priceStr = priceStr.split("-")[0].trim();
        }
        try {
            double multiplier = 1.0;
            if (priceStr.endsWith("k")) {
                multiplier = 1000.0;
                priceStr = priceStr.substring(0, priceStr.length() - 1);
            } else if (priceStr.endsWith("m")) {
                multiplier = 1000000.0;
                priceStr = priceStr.substring(0, priceStr.length() - 1);
            } else if (priceStr.endsWith("b")) {
                multiplier = 1.0E9;
                priceStr = priceStr.substring(0, priceStr.length() - 1);
            }
            double value = Double.parseDouble(priceStr);
            return (long)(value * multiplier);
        }
        catch (NumberFormatException e) {
            DonutUtilityClient.LOGGER.warn("[SpawnerPriceCache] Could not parse price: {}", (Object)priceStr);
            return 0L;
        }
    }

    public static long getPrice(String spawnerType) {
        if (spawnerType == null) {
            return 0L;
        }
        String key = spawnerType.toLowerCase().trim();
        Long price = priceCache.get(key);
        if (price != null) {
            return price;
        }
        if (key.endsWith(" spawner") && (price = priceCache.get(key = key.substring(0, key.length() - 8).trim())) != null) {
            return price;
        }
        for (Map.Entry<String, Long> entry : priceCache.entrySet()) {
            String cacheKey = entry.getKey();
            if (key.contains(cacheKey) || cacheKey.contains(key)) {
                return entry.getValue();
            }
            if (!key.contains("piglin") && !key.contains("pigman") || !cacheKey.contains("piglin") && !cacheKey.contains("pigman")) continue;
            return entry.getValue();
        }
        return 0L;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static String formatPrice(long price) {
        if (price <= 0L) {
            return "N/A";
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

    public static void clear() {
        priceCache.clear();
        loaded = false;
    }
}
