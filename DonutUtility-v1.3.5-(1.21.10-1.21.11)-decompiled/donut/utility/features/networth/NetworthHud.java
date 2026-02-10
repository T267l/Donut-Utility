package donut.utility.features.networth;

import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import donut.utility.features.networth.NetworthCalculator;
import donut.utility.features.networth.NetworthConfigScreen;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;

@Environment(value=EnvType.CLIENT)
public class NetworthHud {
    private static final int LABEL_COLOR = -1;
    private static final int VALUE_COLOR = -11141291;
    private static final int BG_COLOR = Integer.MIN_VALUE;
    private static final int PADDING = 4;

    private NetworthHud() {
    }

    public static void render(class_332 context) {
        String valueText;
        if (!ModConfig.networthHudEnabled) {
            return;
        }
        if (!DonutUtilityClient.isOnDonutSMP(class_310.method_1551())) {
            return;
        }
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) {
            return;
        }
        if (client.field_1755 != null && !(client.field_1755 instanceof NetworthConfigScreen)) {
            return;
        }
        if (ModConfig.networthHudX <= 0) {
            ModConfig.networthHudX = (int)((double)client.method_22683().method_4486() * 0.7) - NetworthHud.getWidth() / 2;
        }
        if (ModConfig.networthHudY <= 0) {
            ModConfig.networthHudY = (int)((double)client.method_22683().method_4502() * 0.4);
        }
        class_327 textRenderer = client.field_1772;
        NetworthCalculator calculator = DonutUtilityClient.getNetworthCalculator();
        if (calculator != null && calculator.hasScan()) {
            NetworthCalculator.NetworthResult result = calculator.getLastResult();
            valueText = result.getFormattedTotal();
        } else {
            Long balance = DonutUtilityClient.getShowMoney().getLocalPlayerBalance();
            valueText = balance != null ? NetworthHud.formatPrice(balance) : "...";
        }
        String label = "Networth: ";
        String fullText = label + valueText;
        int textWidth = textRenderer.method_1727(fullText);
        Objects.requireNonNull(textRenderer);
        int textHeight = 9;
        int x = ModConfig.networthHudX;
        int y = ModConfig.networthHudY;
        context.method_25294(x - 4, y - 4, x + textWidth + 4, y + textHeight + 4, Integer.MIN_VALUE);
        context.method_51433(textRenderer, label, x, y, -1, true);
        int labelWidth = textRenderer.method_1727(label);
        context.method_51433(textRenderer, valueText, x + labelWidth, y, -11141291, true);
    }

    public static int getWidth() {
        class_310 client = class_310.method_1551();
        String text = "Networth: 999.9M$";
        return client.field_1772.method_1727(text) + 8;
    }

    public static int getHeight() {
        class_310 client = class_310.method_1551();
        Objects.requireNonNull(client.field_1772);
        return 9 + 8;
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
