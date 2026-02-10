package donut.utility.mixin;

import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import donut.utility.features.ShowMoney;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import net.minecraft.class_742;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_1297.class})
public abstract class EntityGlowMixin {
    @Unique
    private static final double SPAWN_RADIUS = 400.0;

    @Unique
    private boolean isClientPlayerInSpawnArea(class_310 client) {
        if (client.field_1724 == null) {
            return false;
        }
        double x = client.field_1724.method_23317();
        double z = client.field_1724.method_23321();
        return x >= -400.0 && x <= 400.0 && z >= -400.0 && z <= 400.0;
    }

    @Inject(method={"method_5851"}, at={@At(value="HEAD")}, cancellable=true)
    private void onIsGlowing(CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.auraEnabled) {
            return;
        }
        class_1297 self = (class_1297)this;
        if (!(self instanceof class_742)) {
            return;
        }
        class_742 player = (class_742)self;
        class_310 client = class_310.method_1551();
        if (!DonutUtilityClient.isOnDonutSMP(client)) {
            return;
        }
        if (!this.isClientPlayerInSpawnArea(client)) {
            return;
        }
        ShowMoney showMoney = DonutUtilityClient.getShowMoney();
        if (showMoney == null) {
            return;
        }
        String playerName = player.method_7334().name();
        if (showMoney.shouldGlow(playerName)) {
            cir.setReturnValue((Object)true);
        }
    }

    @Inject(method={"method_22861"}, at={@At(value="HEAD")}, cancellable=true)
    private void onGetTeamColorValue(CallbackInfoReturnable<Integer> cir) {
        if (!ModConfig.auraEnabled) {
            return;
        }
        class_1297 self = (class_1297)this;
        if (!(self instanceof class_742)) {
            return;
        }
        class_742 player = (class_742)self;
        class_310 client = class_310.method_1551();
        if (!DonutUtilityClient.isOnDonutSMP(client)) {
            return;
        }
        if (!this.isClientPlayerInSpawnArea(client)) {
            return;
        }
        ShowMoney showMoney = DonutUtilityClient.getShowMoney();
        if (showMoney == null) {
            return;
        }
        String playerName = player.method_7334().name();
        int glowColor = showMoney.getGlowColor(playerName);
        if (glowColor != -1) {
            cir.setReturnValue((Object)glowColor);
        }
    }
}
