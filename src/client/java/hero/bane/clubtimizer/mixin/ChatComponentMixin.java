package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.action.Hush;
import hero.bane.clubtimizer.command.ClubtimizerConfig;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PlayerUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.GuiMessageTag;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void club$chatCanceller(
            Component message,
            MessageSignature signature,
            GuiMessageTag tag,
            CallbackInfo ci
    ) {
        if (MCPVPStateChanger.inLobby() || PlayerUtil.inSpawnArea()) {
            String noFormatting = message.getString();

            if (ClubtimizerConfig.getLobby().hideChat) {
                int arrowIndex = noFormatting.indexOf('»');

                if (arrowIndex >= 0) {
                    String before = noFormatting.substring(0, arrowIndex).strip();
                    String name = club$extractName(before);

                    if (!PlayerUtil.isSelfOrFriend(name)) ci.cancel();
                    return;
                }
            }

            if (ClubtimizerConfig.getLobby().hidePublicParties) {
                if (noFormatting.isEmpty() || noFormatting.contains("[✔ Join]")) {
                    ci.cancel();
                    return;
                }
            }

            if (!ClubtimizerConfig.getLobby().warning
                    && noFormatting.startsWith("⚠ WARNING")) {
                ci.cancel();
            }
        }

        if (MCPVPStateChanger.inGame()
                && ClubtimizerConfig.getSpecChat().mode == ClubtimizerConfig.specChatMode.off) {
            if (TextUtil.toLegacyString(message).contains("§#7a7a7a »") || message.getString().startsWith("\uD83D\uDC41")) {
                ci.cancel();
            }
        }
    }

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component club$applyAutoHush(Component value) {
        return Hush.replaceMessage(value);
    }

    @Unique
    private String club$extractName(String raw) {
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
