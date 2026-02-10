package donut.utility.features.networth;

import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import donut.utility.features.RateLimiter;
import donut.utility.features.ShowMoney;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_437;

@Environment(value=EnvType.CLIENT)
public class NetworthConfigScreen
extends class_437 {
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final String PREVIEW_TEXT = "Networth: 123.4M$";
    private static final int PADDING = 4;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private class_4185 hudToggleButton;
    private class_4185 selfBalanceToggleButton;
    private class_4185 auraToggleButton;
    private class_4185 refreshButton;

    public NetworthConfigScreen() {
        super((class_2561)class_2561.method_43470((String)"DonutUtility Settings"));
    }

    protected void method_25426() {
        if (ModConfig.networthHudX <= 0 || ModConfig.networthHudX > this.field_22789 - 50) {
            ModConfig.networthHudX = (int)((double)this.field_22789 * 0.7) - this.getTextWidth() / 2;
        }
        if (ModConfig.networthHudY <= 0) {
            ModConfig.networthHudY = (int)((double)this.field_22790 * 0.4);
        }
        int centerX = this.field_22789 / 2;
        int bottomY = this.field_22790 - 105;
        this.hudToggleButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)("HUD: " + (ModConfig.networthHudEnabled ? "ON" : "OFF"))), button -> {
            ModConfig.networthHudEnabled = !ModConfig.networthHudEnabled;
            button.method_25355((class_2561)class_2561.method_43470((String)("HUD: " + (ModConfig.networthHudEnabled ? "ON" : "OFF"))));
            ModConfig.save();
        }).method_46434(centerX - 150 - 5, bottomY, 150, 20).method_46431();
        this.method_37063((class_364)this.hudToggleButton);
        this.selfBalanceToggleButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Show Self: " + (ModConfig.showSelfBalance ? "ON" : "OFF"))), button -> {
            ModConfig.showSelf = ModConfig.showSelfBalance = !ModConfig.showSelfBalance;
            button.method_25355((class_2561)class_2561.method_43470((String)("Show Self: " + (ModConfig.showSelfBalance ? "ON" : "OFF"))));
            ModConfig.save();
        }).method_46434(centerX + 5, bottomY, 150, 20).method_46431();
        this.method_37063((class_364)this.selfBalanceToggleButton);
        this.auraToggleButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)("Aura (Spawn Only): " + (ModConfig.auraEnabled ? "ON" : "OFF"))), button -> {
            ModConfig.auraEnabled = !ModConfig.auraEnabled;
            button.method_25355((class_2561)class_2561.method_43470((String)("Aura (Spawn Only): " + (ModConfig.auraEnabled ? "ON" : "OFF"))));
            ModConfig.save();
        }).method_46434(centerX - 150 - 5, bottomY + 25, 150, 20).method_46431();
        this.method_37063((class_364)this.auraToggleButton);
        this.refreshButton = class_4185.method_46430((class_2561)class_2561.method_43470((String)"Refresh"), button -> {
            ShowMoney showMoney;
            if (!RateLimiter.isRateLimited() && RateLimiter.tryAcquire() && (showMoney = DonutUtilityClient.getShowMoney()) != null) {
                showMoney.clearCache();
                class_310 client = class_310.method_1551();
                if (client.field_1724 != null) {
                    client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a77[\u00a76DonutUtility\u00a77] \u00a7aBalance cache cleared! Refreshing..."), true);
                }
            }
        }).method_46434(centerX + 5, bottomY + 25, 150, 20).method_46431();
        this.method_37063((class_364)this.refreshButton);
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_43470((String)"Done"), button -> this.method_25419()).method_46434(centerX - 50, bottomY + 50, 100, 20).method_46431());
    }

    public void method_25420(class_332 context, int mouseX, int mouseY, float delta) {
        context.method_25296(0, 0, this.field_22789, this.field_22790, -1072689136, -804253680);
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        super.method_25420(context, mouseX, mouseY, delta);
        this.updateRefreshButton();
        context.method_27534(this.field_22793, this.field_22785, this.field_22789 / 2, 20, 0xFFFFFF);
        context.method_25300(this.field_22793, "Drag the HUD element to reposition it", this.field_22789 / 2, 40, 0xAAAAAA);
        this.renderHudPreview(context, mouseX, mouseY);
        super.method_25394(context, mouseX, mouseY, delta);
    }

    private void updateRefreshButton() {
        if (this.refreshButton == null) {
            return;
        }
        class_310 client = class_310.method_1551();
        int nearbyPlayers = 0;
        if (client.field_1687 != null) {
            nearbyPlayers = client.field_1687.method_18456().size();
        }
        int remaining = RateLimiter.getRemaining();
        if (RateLimiter.isRateLimited()) {
            int seconds = RateLimiter.getSecondsUntilReset();
            this.refreshButton.method_25355((class_2561)class_2561.method_43470((String)("Refresh (" + seconds + "s)")));
            this.refreshButton.field_22763 = false;
        } else if (remaining < nearbyPlayers) {
            this.refreshButton.method_25355((class_2561)class_2561.method_43470((String)("Refresh (" + remaining + "/" + nearbyPlayers + ")")));
            this.refreshButton.field_22763 = false;
        } else {
            this.refreshButton.method_25355((class_2561)class_2561.method_43470((String)"Refresh"));
            this.refreshButton.field_22763 = true;
        }
    }

    private int getTextWidth() {
        return this.field_22793.method_1727(PREVIEW_TEXT);
    }

    private int getTextHeight() {
        Objects.requireNonNull(this.field_22793);
        return 9;
    }

    private void renderHudPreview(class_332 context, int mouseX, int mouseY) {
        int x = ModConfig.networthHudX;
        int y = ModConfig.networthHudY;
        int textWidth = this.getTextWidth();
        int textHeight = this.getTextHeight();
        boolean hovering = this.isMouseOverHud(mouseX, mouseY);
        int boxLeft = x - 4;
        int boxTop = y - 4;
        int boxRight = x + textWidth + 4;
        int boxBottom = y + textHeight + 4;
        int bgColor = hovering || this.dragging ? -1606138812 : Integer.MIN_VALUE;
        context.method_25294(boxLeft, boxTop, boxRight, boxBottom, bgColor);
        if (hovering || this.dragging) {
            int borderColor = -256;
            context.method_25294(boxLeft, boxTop, boxRight, boxTop + 1, borderColor);
            context.method_25294(boxLeft, boxBottom - 1, boxRight, boxBottom, borderColor);
            context.method_25294(boxLeft, boxTop, boxLeft + 1, boxBottom, borderColor);
            context.method_25294(boxRight - 1, boxTop, boxRight, boxBottom, borderColor);
        }
        context.method_51433(this.field_22793, PREVIEW_TEXT, x, y, -11141291, true);
    }

    private boolean isMouseOverHud(double mouseX, double mouseY) {
        int x = ModConfig.networthHudX;
        int y = ModConfig.networthHudY;
        int textWidth = this.getTextWidth();
        int textHeight = this.getTextHeight();
        int boxLeft = x - 4;
        int boxTop = y - 4;
        int boxRight = x + textWidth + 4;
        int boxBottom = y + textHeight + 4;
        return mouseX >= (double)boxLeft && mouseX < (double)boxRight && mouseY >= (double)boxTop && mouseY < (double)boxBottom;
    }

    public boolean method_25402(class_11909 click, boolean consumed) {
        if (!consumed && click.method_74245() == 0 && this.isMouseOverHud(click.comp_4798(), click.comp_4799())) {
            this.dragging = true;
            this.dragOffsetX = (int)click.comp_4798() - ModConfig.networthHudX;
            this.dragOffsetY = (int)click.comp_4799() - ModConfig.networthHudY;
            return true;
        }
        return super.method_25402(click, consumed);
    }

    public boolean method_25406(class_11909 click) {
        if (this.dragging) {
            this.dragging = false;
            ModConfig.save();
            return true;
        }
        return super.method_25406(click);
    }

    public boolean method_25403(class_11909 click, double deltaX, double deltaY) {
        if (this.dragging) {
            int newX = (int)click.comp_4798() - this.dragOffsetX;
            int newY = (int)click.comp_4799() - this.dragOffsetY;
            int totalWidth = this.getTextWidth() + 8;
            int totalHeight = this.getTextHeight() + 8;
            newX = Math.max(0, Math.min(this.field_22789 - totalWidth, newX));
            newY = Math.max(0, Math.min(this.field_22790 - totalHeight, newY));
            ModConfig.networthHudX = newX;
            ModConfig.networthHudY = newY;
            return true;
        }
        return super.method_25403(click, deltaX, deltaY);
    }

    public boolean method_25421() {
        return false;
    }

    public void method_25419() {
        ModConfig.save();
        super.method_25419();
    }
}
