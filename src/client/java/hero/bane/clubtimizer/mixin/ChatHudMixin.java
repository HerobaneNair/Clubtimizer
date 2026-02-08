package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.action.AutoGG;
import hero.bane.clubtimizer.action.AutoHush;
import hero.bane.clubtimizer.auto.Requeue;
import hero.bane.clubtimizer.config.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.FriendUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatHud.class)
public abstract class ChatHudMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void club$chatCanceller(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (MCPVPStateChanger.inLobby() || AutoGG.inSpawn()) {
            String noFormatting = message.getString();

            if(ClubtimizerConfig.getLobby().hideChat) {
                int arrowIndex = noFormatting.indexOf('»');

                if (arrowIndex >= 0) {
                    String before = noFormatting.substring(0, arrowIndex).strip();
                    String name = extractName(before);

                    if (!FriendUtil.isSelfOrFriend(name)) ci.cancel();
                    return;
                }
            }

            if(!ClubtimizerConfig.getLobby().warning && noFormatting.startsWith("⚠ WARNING")) ci.cancel();
        }

        if (MCPVPStateChanger.inGame()
                && ClubtimizerConfig.getAutoHush().specChat) {
            if (TextUtil.toLegacyString(message).contains("§#7a7a7a »")) {
                ci.cancel();
            }

        }
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text club$applyAutoHush(Text value) {
        return AutoHush.replaceMessage(value);
    }


    @Unique
    private String extractName(String raw) {
        if (raw == null) return "";
        String stripped = TextUtil.stripFormatting(raw).strip();
        if (stripped.isEmpty()) return stripped;

        String[] parts = stripped.split("\\s+");
        for (String p : parts) {
            if (p.length() > 1) return p;
        }
        return parts.length > 0 ? parts[parts.length - 1] : stripped;
    }
}
