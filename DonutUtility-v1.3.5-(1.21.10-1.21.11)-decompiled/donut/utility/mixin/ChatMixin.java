package donut.utility.mixin;

import donut.utility.DonutUtilityClient;
import donut.utility.ModConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_338;
import net.minecraft.class_7469;
import net.minecraft.class_7591;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={class_338.class})
public class ChatMixin {
    @Inject(method={"method_44811(Lnet/minecraft/class_2561;Lnet/minecraft/class_7469;Lnet/minecraft/class_7591;)V"}, at={@At(value="HEAD")}, cancellable=true)
    private void onChatMessage(class_2561 message, class_7469 signatureData, class_7591 indicator, CallbackInfo ci) {
        String token;
        if (!DonutUtilityClient.isOnDonutSMP(class_310.method_1551())) {
            return;
        }
        String messageStr = message.getString();
        if (messageStr.toLowerCase().contains("your api token is:") && (token = this.extractToken(messageStr)) != null && !token.isEmpty()) {
            ModConfig.apiKey = token;
            ModConfig.save();
            DonutUtilityClient.setWaitingForApiResponse(false);
            DonutUtilityClient.LOGGER.info("[DonutUtility] API key saved successfully!");
            class_310 client = class_310.method_1551();
            if (client.field_1724 != null) {
                client.field_1724.method_7353((class_2561)class_2561.method_43470((String)"\u00a77[\u00a76DonutUtility\u00a77] \u00a7aAPI key configured! ShowMoney is now active."), false);
            }
            ci.cancel();
        }
    }

    private String extractToken(String message) {
        String afterPrefix;
        String[] parts;
        String lowerMessage = message.toLowerCase();
        int index = lowerMessage.indexOf("your api token is:");
        if (index != -1 && (parts = (afterPrefix = message.substring(index + "your api token is:".length()).trim()).split("\\s+")).length > 0) {
            return parts[0].trim();
        }
        return null;
    }
}
