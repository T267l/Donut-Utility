package donut.utility.mixin;

import donut.utility.DonutUtilityClient;
import donut.utility.features.networth.EChestMemory;
import donut.utility.features.networth.NetworthCalculator;
import donut.utility.features.networth.NetworthOverlay;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1707;
import net.minecraft.class_1799;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_465;
import net.minecraft.class_476;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_476.class})
public abstract class GenericContainerScreenMixin
extends class_465<class_1707> {
    @Unique
    private boolean isEnderChest = false;
    @Unique
    private boolean hasScanned = false;
    @Unique
    private int echestRows = 6;
    @Unique
    private int renderFrameCount = 0;
    @Unique
    private static final int CAPTURE_DELAY_FRAMES = 60;

    public GenericContainerScreenMixin(class_1707 handler, class_1661 inventory, class_2561 title) {
        super((class_1703)handler, inventory, title);
    }

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void onInit(class_1707 handler, class_1661 inventory, class_2561 title, CallbackInfo ci) {
        if (!DonutUtilityClient.isOnDonutSMP(class_310.method_1551())) {
            return;
        }
        String titleStr = title.getString();
        String titleLower = titleStr.toLowerCase();
        int rows = handler.method_17388();
        DonutUtilityClient.LOGGER.info("[DonutUtility] Container opened: '{}' with {} rows", (Object)titleStr, (Object)rows);
        boolean bl = this.isEnderChest = titleLower.contains("ender") && titleLower.contains("chest");
        if (this.isEnderChest) {
            DonutUtilityClient.LOGGER.info("[DonutUtility] Detected as Ender Chest! ({} rows)", (Object)rows);
            this.echestRows = rows;
        }
    }

    @Inject(method={"method_25394"}, at={@At(value="TAIL")})
    private void onRender(class_332 context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!DonutUtilityClient.isOnDonutSMP(class_310.method_1551())) {
            return;
        }
        if (!DonutUtilityClient.isNetworthEnabled()) {
            return;
        }
        if (this.isEnderChest && !this.hasScanned) {
            ++this.renderFrameCount;
            if (this.renderFrameCount >= 60) {
                this.captureEchestContents();
                this.hasScanned = true;
            }
        }
        NetworthCalculator calculator = DonutUtilityClient.getNetworthCalculator();
        NetworthCalculator.NetworthResult result = calculator.getLastResult();
        NetworthOverlay.render(context, result, this.field_22789, this.field_22790);
    }

    @Unique
    private void captureEchestContents() {
        class_1707 handler = (class_1707)this.field_2797;
        int slots = this.echestRows * 9;
        ArrayList<class_1799> contents = new ArrayList<class_1799>(slots);
        int itemCount = 0;
        for (int i = 0; i < slots; ++i) {
            class_1799 stack = handler.method_7611(i).method_7677().method_7972();
            contents.add(stack);
            if (stack.method_7960()) continue;
            ++itemCount;
        }
        EChestMemory.setContents(contents);
        DonutUtilityClient.LOGGER.info("[DonutUtility] Echest captured! {} slots, {} non-empty items", (Object)slots, (Object)itemCount);
    }
}
