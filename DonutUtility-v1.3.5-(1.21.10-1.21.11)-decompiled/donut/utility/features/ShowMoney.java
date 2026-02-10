package donut.utility.features;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import donut.utility.features.RateLimiter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_742;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;

@Environment(value=EnvType.CLIENT)
public class ShowMoney {
    private final Map<String, Long> moneyCache = new ConcurrentHashMap<String, Long>();
    private final Set<String> pendingRequests = ConcurrentHashMap.newKeySet();
    private final Set<String> failedRequests = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> moneyLeaderboardCache = new ConcurrentHashMap<String, Integer>();
    private final Map<LeaderboardType, Map<String, Integer>> allLeaderboardsCache = new ConcurrentHashMap<LeaderboardType, Map<String, Integer>>();
    private Long localPlayerBalance = null;
    private boolean localPlayerPending = false;
    private boolean leaderboardLoaded = false;
    private boolean leaderboardLoading = false;
    private static final String LEADERBOARD_URL = "https://raw.githubusercontent.com/stashya/donutsmp-leaderboard/main/all_leaderboards.json";
    private static final int COLOR_TOP_10 = -43691;
    private static final int COLOR_TOP_100 = -43521;
    private static final int COLOR_TOP_1000 = -5635841;
    private static final int COLOR_RANK_TEXT = -1;
    private final HttpClient httpClient;
    private final Gson gson = new Gson();

    public ShowMoney() {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
        for (LeaderboardType type : LeaderboardType.values()) {
            this.allLeaderboardsCache.put(type, new ConcurrentHashMap());
        }
    }

    public void onTick(class_310 client) {
        if (client.field_1687 == null || client.field_1724 == null) {
            return;
        }
        if (ModConfig.apiKey.isEmpty()) {
            return;
        }
        RateLimiter.tick();
        if (!this.leaderboardLoaded && !this.leaderboardLoading) {
            this.fetchLeaderboard();
        }
        String localPlayerName = client.field_1724.method_7334().name();
        if (this.localPlayerBalance == null && !this.localPlayerPending && !this.failedRequests.contains(localPlayerName) && RateLimiter.tryAcquire()) {
            this.fetchPlayerMoney(localPlayerName, true);
        }
        for (class_742 player : client.field_1687.method_18456()) {
            if (player == null || player == client.field_1724) continue;
            if (!RateLimiter.hasCapacity()) break;
            String playerName = player.method_7334().name();
            if (this.moneyCache.containsKey(playerName) || this.pendingRequests.contains(playerName) || this.failedRequests.contains(playerName) || !RateLimiter.tryAcquire()) continue;
            this.fetchPlayerMoney(playerName, false);
        }
    }

    public boolean isBusy() {
        return !this.pendingRequests.isEmpty() || this.localPlayerPending;
    }

    public Long getLocalPlayerBalance() {
        return this.localPlayerBalance;
    }

    public boolean isLocalPlayerBalanceLoading() {
        return this.localPlayerPending;
    }

    public void refreshLocalPlayerBalance() {
        this.localPlayerBalance = null;
        this.localPlayerPending = false;
    }

    public void onRender(WorldRenderContext context) {
        class_310 client = class_310.method_1551();
        if (client.field_1687 == null || client.field_1724 == null) {
            return;
        }
        if (ModConfig.apiKey.isEmpty()) {
            return;
        }
        class_243 cameraPos = context.worldState().field_63082.field_63078;
        class_4184 camera = client.field_1773.method_19418();
        float tickDelta = client.method_61966().method_60637(false);
        for (class_742 player : client.field_1687.method_18456()) {
            if (player == null || player == client.field_1724 && (client.field_1690.method_31044().method_31034() || !ModConfig.showSelf)) continue;
            String playerName = player.method_7334().name();
            Long money = player == client.field_1724 ? this.localPlayerBalance : this.moneyCache.get(playerName);
            double x = class_3532.method_16436((double)tickDelta, (double)player.field_6038, (double)player.method_23317());
            double y = class_3532.method_16436((double)tickDelta, (double)player.field_5971, (double)player.method_23318());
            double z = class_3532.method_16436((double)tickDelta, (double)player.field_5989, (double)player.method_23321());
            double renderY = y + (double)player.method_17682() + 0.5 + ModConfig.yOffset;
            double relX = x - cameraPos.field_1352;
            double relY = renderY - cameraPos.field_1351;
            double relZ = z - cameraPos.field_1350;
            List<LeaderboardEntry> leaderboardEntries = this.getPlayerLeaderboardEntries(playerName);
            float lineHeight = 0.3f;
            int lineIndex = 0;
            for (LeaderboardEntry entry : leaderboardEntries) {
                if (entry.type == LeaderboardType.MONEY) continue;
                String text = entry.type.getDisplayName() + " #" + entry.rank;
                double entryY = relY + (double)((float)(leaderboardEntries.size() - lineIndex) * lineHeight);
                this.renderLeaderboardLabel(camera, text, relX, entryY, relZ, entry.type.getColor());
                ++lineIndex;
            }
            if (money == null) continue;
            Integer moneyRank = this.moneyLeaderboardCache.get(playerName.toLowerCase());
            int moneyColor = moneyRank != null && moneyRank <= 10 ? -43691 : (moneyRank != null && moneyRank <= 100 ? -43521 : (moneyRank != null && moneyRank <= 1000 ? -5635841 : (money >= 1000000000L ? ModConfig.billionaireColor : ModConfig.normalColor)));
            String moneyText = this.formatMoney(money);
            String rankText = moneyRank != null ? " #" + moneyRank : null;
            double moneyY = relY + (double)((float)(leaderboardEntries.size() - lineIndex) * lineHeight);
            this.renderWorldSpaceNametag(camera, moneyText, rankText, relX, moneyY, relZ, moneyColor);
        }
    }

    private void renderLeaderboardLabel(class_4184 camera, String text, double x, double y, double z, int color) {
        class_310 client = class_310.method_1551();
        class_327 textRenderer = client.field_1772;
        float textWidth = textRenderer.method_1727(text);
        class_4587 matrices = new class_4587();
        matrices.method_22903();
        matrices.method_22904(x, y, z);
        matrices.method_22907((Quaternionfc)camera.method_23767());
        float baseScale = 0.025f;
        float finalScale = baseScale * (float)ModConfig.scale;
        matrices.method_22905(finalScale, -finalScale, finalScale);
        float offsetX = -textWidth / 2.0f;
        Matrix4f matrix = matrices.method_23760().method_23761();
        class_4597.class_4598 immediate = client.method_22940().method_23000();
        int bgColor = 0x40000000;
        textRenderer.method_27521(text, offsetX, 0.0f, 0, false, matrix, (class_4597)immediate, class_327.class_6415.field_33993, bgColor, 0xF000F0);
        textRenderer.method_27521(text, offsetX, 0.0f, color, true, matrix, (class_4597)immediate, class_327.class_6415.field_33993, 0, 0xF000F0);
        immediate.method_22993();
        matrices.method_22909();
    }

    private void renderWorldSpaceNametag(class_4184 camera, String moneyText, String rankText, double x, double y, double z, int moneyColor) {
        class_310 client = class_310.method_1551();
        class_327 textRenderer = client.field_1772;
        float moneyWidth = textRenderer.method_1727(moneyText);
        float rankWidth = rankText != null ? (float)textRenderer.method_1727(rankText) : 0.0f;
        float totalWidth = moneyWidth + rankWidth;
        class_4587 matrices = new class_4587();
        matrices.method_22903();
        matrices.method_22904(x, y, z);
        matrices.method_22907((Quaternionfc)camera.method_23767());
        float baseScale = 0.025f;
        float finalScale = baseScale * (float)ModConfig.scale;
        matrices.method_22905(finalScale, -finalScale, finalScale);
        float offsetX = -totalWidth / 2.0f;
        Matrix4f matrix = matrices.method_23760().method_23761();
        class_4597.class_4598 immediate = client.method_22940().method_23000();
        int bgColor = 0x40000000;
        String fullText = moneyText + (rankText != null ? rankText : "");
        textRenderer.method_27521(fullText, offsetX, 0.0f, 0, false, matrix, (class_4597)immediate, class_327.class_6415.field_33993, bgColor, 0xF000F0);
        textRenderer.method_27521(moneyText, offsetX, 0.0f, moneyColor, true, matrix, (class_4597)immediate, class_327.class_6415.field_33993, 0, 0xF000F0);
        if (rankText != null) {
            textRenderer.method_27521(rankText, offsetX + moneyWidth, 0.0f, -1, true, matrix, (class_4597)immediate, class_327.class_6415.field_33993, 0, 0xF000F0);
        }
        immediate.method_22993();
        matrices.method_22909();
    }

    private void fetchLeaderboard() {
        this.leaderboardLoading = true;
        DonutUtilityClient.LOGGER.info("[ShowMoney] Fetching all leaderboards from GitHub...");
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(LEADERBOARD_URL)).header("accept", "application/json").timeout(Duration.ofSeconds(15L)).GET().build();
        ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (response.statusCode() == 200) {
                try {
                    JsonObject json = this.gson.fromJson((String)response.body(), JsonObject.class);
                    JsonObject leaderboards = json.getAsJsonObject("leaderboards");
                    if (leaderboards != null) {
                        for (String apiName : leaderboards.keySet()) {
                            JsonArray players;
                            LeaderboardType type = LeaderboardType.fromApiName(apiName);
                            if (type == null || (players = leaderboards.getAsJsonArray(apiName)) == null) continue;
                            Map<String, Integer> cache = this.allLeaderboardsCache.get((Object)type);
                            cache.clear();
                            if (type == LeaderboardType.MONEY) {
                                this.moneyLeaderboardCache.clear();
                            }
                            for (int i = 0; i < players.size(); ++i) {
                                JsonObject player = players.get(i).getAsJsonObject();
                                String username = player.get("username").getAsString();
                                if (username == null || username.isEmpty()) continue;
                                String lowerUsername = username.toLowerCase();
                                int rank = i + 1;
                                cache.put(lowerUsername, rank);
                                if (type != LeaderboardType.MONEY) continue;
                                this.moneyLeaderboardCache.put(lowerUsername, rank);
                            }
                            if (!ModConfig.debugLog) continue;
                            DonutUtilityClient.LOGGER.info("[ShowMoney] Loaded {} players for {} leaderboard", (Object)cache.size(), (Object)type.getDisplayName());
                        }
                    }
                    this.leaderboardLoaded = true;
                    DonutUtilityClient.LOGGER.info("[ShowMoney] Leaderboard loading complete");
                }
                catch (Exception e) {
                    DonutUtilityClient.LOGGER.error("[ShowMoney] Failed to parse leaderboards: {}", (Object)e.getMessage());
                }
            } else {
                DonutUtilityClient.LOGGER.warn("[ShowMoney] Failed to fetch leaderboards: HTTP {}", (Object)response.statusCode());
            }
            this.leaderboardLoading = false;
        })).exceptionally(e -> {
            DonutUtilityClient.LOGGER.error("[ShowMoney] Leaderboard fetch failed: {}", (Object)e.getMessage());
            this.leaderboardLoading = false;
            return null;
        });
    }

    private void fetchPlayerMoney(String playerName, boolean isLocalPlayer) {
        if (ModConfig.apiKey.isEmpty()) {
            return;
        }
        if (isLocalPlayer) {
            this.localPlayerPending = true;
        } else {
            this.pendingRequests.add(playerName);
        }
        if (ModConfig.debugLog) {
            DonutUtilityClient.LOGGER.info("[ShowMoney] Fetching money for player: {}{}", (Object)playerName, (Object)(isLocalPlayer ? " (local)" : ""));
        }
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create("https://api.donutsmp.net/v1/stats/" + playerName)).header("accept", "application/json").header("Authorization", ModConfig.apiKey).timeout(Duration.ofSeconds(10L)).GET().build();
        ((CompletableFuture)this.httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept(response -> {
            if (isLocalPlayer) {
                this.localPlayerPending = false;
            } else {
                this.pendingRequests.remove(playerName);
            }
            if (response.statusCode() == 200) {
                try {
                    JsonObject json = this.gson.fromJson((String)response.body(), JsonObject.class);
                    JsonObject result = json.getAsJsonObject("result");
                    String moneyStr = result.get("money").getAsString();
                    double moneyDouble = Double.parseDouble(moneyStr);
                    long money = (long)moneyDouble;
                    if (isLocalPlayer) {
                        this.localPlayerBalance = money;
                        DonutUtilityClient.LOGGER.info("[ShowMoney] Local player balance: {}", (Object)this.formatMoney(money));
                    } else {
                        this.moneyCache.put(playerName, money);
                    }
                    if (ModConfig.debugLog) {
                        DonutUtilityClient.LOGGER.info("[ShowMoney] Got money for {}: {} ({})", playerName, money, this.formatMoney(money));
                    }
                }
                catch (Exception e) {
                    if (ModConfig.debugLog) {
                        DonutUtilityClient.LOGGER.error("[ShowMoney] Failed to parse money for {}: {}", (Object)playerName, (Object)e.getMessage());
                    }
                    this.failedRequests.add(playerName);
                }
            } else if (response.statusCode() == 401) {
                if (!ModConfig.apiKey.isEmpty()) {
                    DonutUtilityClient.LOGGER.warn("[ShowMoney] API key expired/invalid (401), clearing for auto-refresh");
                    ModConfig.apiKey = "";
                    ModConfig.save();
                    this.clearCache();
                    DonutUtilityClient.resetApiKeyCheck();
                }
            } else if (response.statusCode() == 500) {
                if (ModConfig.debugLog) {
                    DonutUtilityClient.LOGGER.warn("[ShowMoney] Could not fetch stats for {} (500 error - player may not exist)", (Object)playerName);
                }
                this.failedRequests.add(playerName);
            } else {
                if (ModConfig.debugLog) {
                    DonutUtilityClient.LOGGER.warn("[ShowMoney] Unexpected response code {} for {}", (Object)response.statusCode(), (Object)playerName);
                }
                this.failedRequests.add(playerName);
            }
        })).exceptionally(e -> {
            if (isLocalPlayer) {
                this.localPlayerPending = false;
            } else {
                this.pendingRequests.remove(playerName);
            }
            this.failedRequests.add(playerName);
            if (ModConfig.debugLog) {
                DonutUtilityClient.LOGGER.error("[ShowMoney] HTTP request failed for {}: {}", (Object)playerName, (Object)e.getMessage());
            }
            return null;
        });
    }

    private String formatMoney(long money) {
        if (money >= 1000000000L) {
            double value = (double)money / 1.0E9;
            return String.format("%.1fB$", value);
        }
        if (money >= 1000000L) {
            double value = (double)money / 1000000.0;
            return String.format("%.1fM$", value);
        }
        if (money >= 1000L) {
            double value = (double)money / 1000.0;
            return String.format("%.1fk$", value);
        }
        return money + "$";
    }

    private List<LeaderboardEntry> getPlayerLeaderboardEntries(String playerName) {
        String lowerName = playerName.toLowerCase();
        ArrayList<LeaderboardEntry> entries = new ArrayList<LeaderboardEntry>();
        for (LeaderboardType type : LeaderboardType.values()) {
            Integer rank;
            Map<String, Integer> cache = this.allLeaderboardsCache.get((Object)type);
            if (cache == null || (rank = cache.get(lowerName)) == null || rank > 10) continue;
            entries.add(new LeaderboardEntry(type, rank));
        }
        entries.sort(Comparator.comparingInt(e -> e.rank));
        return entries;
    }

    public void clearCache() {
        this.moneyCache.clear();
        this.pendingRequests.clear();
        this.failedRequests.clear();
        this.moneyLeaderboardCache.clear();
        for (Map<String, Integer> cache : this.allLeaderboardsCache.values()) {
            cache.clear();
        }
        this.leaderboardLoaded = false;
        this.localPlayerBalance = null;
        this.localPlayerPending = false;
    }

    public Long getCachedBalance(String playerName) {
        return this.moneyCache.get(playerName);
    }

    public Integer getPlayerRank(String playerName) {
        return this.moneyLeaderboardCache.get(playerName.toLowerCase());
    }

    public boolean shouldGlow(String playerName) {
        if (!ModConfig.auraEnabled) {
            return false;
        }
        String lowerName = playerName.toLowerCase();
        for (LeaderboardType type : LeaderboardType.values()) {
            Integer rank;
            Map<String, Integer> cache = this.allLeaderboardsCache.get((Object)type);
            if (cache == null || (rank = cache.get(lowerName)) == null || rank > 100) continue;
            return true;
        }
        return false;
    }

    public int getGlowColor(String playerName) {
        String lowerName = playerName.toLowerCase();
        int bestRank = Integer.MAX_VALUE;
        LeaderboardType bestType = null;
        for (LeaderboardType type : LeaderboardType.values()) {
            Integer rank;
            Map<String, Integer> cache = this.allLeaderboardsCache.get((Object)type);
            if (cache == null || (rank = cache.get(lowerName)) == null || rank >= bestRank) continue;
            bestRank = rank;
            bestType = type;
        }
        if (bestType == null || bestRank > 100) {
            return -1;
        }
        if (bestRank <= 10) {
            return bestType.getColor() & 0xFFFFFF;
        }
        if (bestRank <= 100 && bestType == LeaderboardType.MONEY) {
            return 0xFF55FF;
        }
        return -1;
    }

    @Environment(value=EnvType.CLIENT)
    public static enum LeaderboardType {
        MONEY("Money", -10496),
        KILLS("Kills", -43691),
        DEATHS("Deaths", -43691),
        MOBS_KILLED("Mobs Killed", -43691),
        BROKEN_BLOCKS("Blocks Broken", -171),
        PLACED_BLOCKS("Blocks Placed", -171),
        PLAYTIME("Playtime", -11184641),
        SELL("/Sell", -11141291),
        SHARDS("Shards", -5635841),
        SHOP("/Shop", -11141291);

        private final String displayName;
        private final int color;

        private LeaderboardType(String displayName, int color) {
            this.displayName = displayName;
            this.color = color;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public int getColor() {
            return this.color;
        }

        public static LeaderboardType fromApiName(String apiName) {
            return switch (apiName.toLowerCase()) {
                case "money" -> MONEY;
                case "kills" -> KILLS;
                case "deaths" -> DEATHS;
                case "mobskilled" -> MOBS_KILLED;
                case "brokenblocks" -> BROKEN_BLOCKS;
                case "placedblocks" -> PLACED_BLOCKS;
                case "playtime" -> PLAYTIME;
                case "sell" -> SELL;
                case "shards" -> SHARDS;
                case "shop" -> SHOP;
                default -> null;
            };
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class LeaderboardEntry {
        public final LeaderboardType type;
        public final int rank;

        public LeaderboardEntry(LeaderboardType type, int rank) {
            this.type = type;
            this.rank = rank;
        }
    }
}
