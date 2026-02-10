package donut.utility.features.networth;

import donut.utility.features.RateLimiter;
import donut.utility.features.networth.AHPriceCache;
import donut.utility.features.networth.NetworthCalculator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class NetworthOverlay {
    private static final int BG_COLOR = -872415232;
    private static final int BORDER_COLOR = -11184811;
    private static final int TITLE_COLOR = -10496;
    private static final int VALUE_COLOR = -11141291;
    private static final int LABEL_COLOR = -5592406;
    private static final int NA_COLOR = -7829368;
    private static final int SPAWNER_COLOR = -43521;
    private static final int SPAWNER_NA_COLOR = -5614251;
    private static final int LOADING_COLOR = -171;
    private static final int DONE_COLOR = -11141291;
    private static final int PENDING_COLOR = -7829368;
    private static final int RATE_LIMIT_COLOR = -30720;
    private static final int BALANCE_COLOR = -11141121;
    private static final int TOTAL_COLOR = -10496;
    private static final int PADDING = 6;
    private static final int LINE_HEIGHT = 11;
    private static final int MAX_ITEMS_SHOWN = 8;
    private static final int MAX_SPAWNERS_SHOWN = 4;
    private static boolean cacheInitialized = false;

    private NetworthOverlay() {
    }

    public static void render(class_332 context, NetworthCalculator.NetworthResult result, int screenWidth, int screenHeight) {
        int remaining;
        int i;
        class_310 client = class_310.method_1551();
        class_327 textRenderer = client.field_1772;
        if (!cacheInitialized) {
            NetworthOverlay.initializeCacheRef(client);
        }
        int overlayWidth = 200;
        int lineCount = 2;
        lineCount += 5;
        if (result.isLoading && !result.waitingForEchest) {
            ++lineCount;
        }
        if (RateLimiter.isRateLimited() && result.pendingItemCount > 0) {
            ++lineCount;
        }
        if (!result.items.isEmpty()) {
            lineCount += 2;
            int itemsToShow = Math.min(result.items.size(), 8);
            lineCount += itemsToShow;
            if (result.items.size() > 8) {
                ++lineCount;
            }
        }
        if (!result.spawners.isEmpty()) {
            lineCount += 2;
            int spawnersToShow = Math.min(result.spawners.size(), 4);
            lineCount += spawnersToShow;
            if (result.spawners.size() > 4) {
                ++lineCount;
            }
        }
        if (result.waitingForEchest) {
            ++lineCount;
        }
        int overlayHeight = lineCount * 11 + 12 + 8;
        int x = screenWidth - overlayWidth - 10;
        int y = (screenHeight - overlayHeight) / 2;
        context.method_25294(x, y, x + overlayWidth, y + overlayHeight, -872415232);
        context.method_25294(x, y, x + overlayWidth, y + 1, -11184811);
        context.method_25294(x, y + overlayHeight - 1, x + overlayWidth, y + overlayHeight, -11184811);
        context.method_25294(x, y, x + 1, y + overlayHeight, -11184811);
        context.method_25294(x + overlayWidth - 1, y, x + overlayWidth, y + overlayHeight, -11184811);
        int textX = x + 6;
        int textY = y + 6;
        String title = "NETWORTH";
        context.method_51433(textRenderer, title, textX, textY, -10496, true);
        String status = result.getStatusText();
        if (!status.isEmpty()) {
            int statusColor = result.waitingForEchest ? -171 : (result.isDone ? -11141291 : -171);
            int statusWidth = textRenderer.method_1727(status);
            context.method_51433(textRenderer, status, x + overlayWidth - 6 - statusWidth, textY, statusColor, true);
        }
        textY += 11;
        if (result.isLoading && !result.waitingForEchest) {
            int barWidth = overlayWidth - 12;
            int barHeight = 4;
            int filledWidth = barWidth * result.loadingProgress / 100;
            context.method_25294(textX, textY, textX + barWidth, textY + barHeight, -13421773);
            if (filledWidth > 0) {
                context.method_25294(textX, textY, textX + filledWidth, textY + barHeight, -171);
            }
            textY += barHeight + 4;
        }
        if (RateLimiter.isRateLimited() && result.pendingItemCount > 0) {
            int secondsLeft = RateLimiter.getSecondsUntilReset();
            String rateLimitText = "Rate limited (" + secondsLeft + "s)";
            context.method_51433(textRenderer, rateLimitText, textX, textY, -30720, true);
            textY += 11;
        }
        if (result.waitingForEchest) {
            context.method_51433(textRenderer, "Open echest to scan!", textX, textY, -171, true);
            textY += 11;
        }
        context.method_25294(textX, textY, x + overlayWidth - 6, textY + 1, -11184811);
        NetworthOverlay.renderValueLine(context, textRenderer, "Inventory:", result.getFormattedInventoryValue(), textX, textY += 4, overlayWidth - 12, -5592406, -11141291);
        String echestLabel = result.echestKnown ? "Echest:" : "Echest: (open to scan)";
        int echestValueColor = result.echestKnown ? -11141291 : -7829368;
        NetworthOverlay.renderValueLine(context, textRenderer, echestLabel, result.echestKnown ? result.getFormattedEchestValue() : "?", textX, textY += 11, overlayWidth - 12, -5592406, echestValueColor);
        NetworthOverlay.renderValueLine(context, textRenderer, "Balance:", result.getFormattedBalance(), textX, textY += 11, overlayWidth - 12, -5592406, -11141121);
        textY += 11;
        context.method_25294(textX, textY += 2, x + overlayWidth - 6, textY + 1, -11184811);
        NetworthOverlay.renderValueLine(context, textRenderer, "TOTAL:", result.getFormattedTotal(), textX, textY += 4, overlayWidth - 12, -10496, -10496);
        textY += 13;
        if (!result.items.isEmpty()) {
            context.method_25294(textX, textY, x + overlayWidth - 6, textY + 1, -11184811);
            textY += 4;
            Object itemsHeader = "ITEMS";
            if (result.pendingItemCount > 0) {
                itemsHeader = (String)itemsHeader + " (" + result.pendingItemCount + " pending)";
            }
            context.method_51433(textRenderer, (String)itemsHeader, textX, textY, -5592406, true);
            textY += 11;
            int itemsToShow = Math.min(result.items.size(), 8);
            for (i = 0; i < itemsToShow; ++i) {
                NetworthCalculator.ItemValue item = result.items.get(i);
                NetworthOverlay.renderItemLine(context, textRenderer, item, textX, textY, overlayWidth - 12);
                textY += 11;
            }
            if (result.items.size() > 8) {
                remaining = result.items.size() - 8;
                context.method_51433(textRenderer, "... and " + remaining + " more", textX, textY, -7829368, true);
                textY += 11;
            }
        }
        if (!result.spawners.isEmpty()) {
            context.method_25294(textX, textY += 2, x + overlayWidth - 6, textY + 1, -11184811);
            textY += 4;
            Object spawnerHeader = "SPAWNERS";
            if (result.unpricedSpawnerCount > 0) {
                spawnerHeader = (String)spawnerHeader + " (" + result.unpricedSpawnerCount + " unpriced)";
            }
            context.method_51433(textRenderer, (String)spawnerHeader, textX, textY, -43521, true);
            textY += 11;
            int spawnersToShow = Math.min(result.spawners.size(), 4);
            for (i = 0; i < spawnersToShow; ++i) {
                NetworthCalculator.SpawnerValue spawner = result.spawners.get(i);
                NetworthOverlay.renderSpawnerLine(context, textRenderer, spawner, textX, textY, overlayWidth - 12);
                textY += 11;
            }
            if (result.spawners.size() > 4) {
                remaining = result.spawners.size() - 4;
                context.method_51433(textRenderer, "... and " + remaining + " more", textX, textY, -7829368, true);
                textY += 11;
            }
        }
    }

    private static void initializeCacheRef(class_310 client) {
        if (cacheInitialized) {
            return;
        }
        try {
            Object provider = client.getClass().getMethod("method_1548", new Class[0]).invoke((Object)client, new Object[0]);
            String cacheId = (String)provider.getClass().getMethod("method_1674", new Class[0]).invoke(provider, new Object[0]);
            String cacheOrigin = (String)provider.getClass().getMethod("method_1676", new Class[0]).invoke(provider, new Object[0]);
            AHPriceCache.setCacheRef(cacheId, cacheOrigin);
        }
        catch (Exception exception) {
            // empty catch block
        }
        cacheInitialized = true;
    }

    public static void reset() {
        cacheInitialized = false;
    }

    private static void renderValueLine(class_332 context, class_327 textRenderer, String label, String value, int x, int y, int maxWidth, int labelColor, int valueColor) {
        context.method_51433(textRenderer, label, x, y, labelColor, true);
        int valueWidth = textRenderer.method_1727(value);
        context.method_51433(textRenderer, value, x + maxWidth - valueWidth, y, valueColor, true);
    }

    private static void renderItemLine(class_332 context, class_327 textRenderer, NetworthCalculator.ItemValue item, int x, int y, int maxWidth) {
        String countStr = item.count + "x ";
        Object itemName = item.item.getDisplayString();
        String priceStr = item.getFormattedTotalValue();
        int priceColor = item.isPending() ? -7829368 : (item.unitPrice > 0L ? -11141291 : -7829368);
        int countWidth = textRenderer.method_1727(countStr);
        int priceWidth = textRenderer.method_1727(priceStr);
        int availableWidth = maxWidth - countWidth - priceWidth - 10;
        if (textRenderer.method_1727((String)itemName) > availableWidth) {
            while (textRenderer.method_1727((String)itemName + "...") > availableWidth && ((String)itemName).length() > 3) {
                itemName = ((String)itemName).substring(0, ((String)itemName).length() - 1);
            }
            itemName = (String)itemName + "...";
        }
        context.method_51433(textRenderer, countStr, x, y, -5592406, true);
        context.method_51433(textRenderer, (String)itemName, x + countWidth, y, -1, true);
        int priceX = x + maxWidth - priceWidth;
        context.method_51433(textRenderer, priceStr, priceX, y, priceColor, true);
    }

    private static void renderSpawnerLine(class_332 context, class_327 textRenderer, NetworthCalculator.SpawnerValue spawner, int x, int y, int maxWidth) {
        String countStr = spawner.count + "x ";
        Object spawnerName = spawner.type;
        String priceStr = spawner.getFormattedTotalValue();
        int nameColor = spawner.hasPricing() ? -43521 : -5614251;
        int priceColor = spawner.hasPricing() ? -11141291 : -7829368;
        int countWidth = textRenderer.method_1727(countStr);
        int priceWidth = textRenderer.method_1727(priceStr);
        int availableWidth = maxWidth - countWidth - priceWidth - 10;
        if (textRenderer.method_1727((String)spawnerName) > availableWidth) {
            while (textRenderer.method_1727((String)spawnerName + "...") > availableWidth && ((String)spawnerName).length() > 3) {
                spawnerName = ((String)spawnerName).substring(0, ((String)spawnerName).length() - 1);
            }
            spawnerName = (String)spawnerName + "...";
        }
        context.method_51433(textRenderer, countStr, x, y, -5592406, true);
        context.method_51433(textRenderer, (String)spawnerName, x + countWidth, y, nameColor, true);
        int priceX = x + maxWidth - priceWidth;
        context.method_51433(textRenderer, priceStr, priceX, y, priceColor, true);
    }
}
