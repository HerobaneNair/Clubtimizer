package hero.bane.mixin;

import hero.bane.action.AutoCope;
import hero.bane.action.AutoGG;
import hero.bane.action.AutoResponse;
import hero.bane.auto.FriendList;
import hero.bane.auto.PartyMaker;
import hero.bane.auto.Rematch;
import hero.bane.auto.TotemResetter;
import hero.bane.util.TextUtil;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void club$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        Text original = packet.content();

        String legacyString = TextUtil.toLegacyString(original);
        String noFormatting = original.getString();

        PartyMaker.handleMessage(noFormatting);
        AutoCope.handleMessage(noFormatting);
        AutoResponse.handleMessage(legacyString, noFormatting);
        AutoGG.handleMessage(noFormatting);
        Rematch.handleMessage(noFormatting);
        TotemResetter.handleMessage(noFormatting);
    }

    @Inject(method = "onCommandSuggestions", at = @At("HEAD"))
    private void club$onSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        if (packet.id() != FriendList.pendingId) return;

        FriendList.complete(
                packet.suggestions().stream()
                        .map(CommandSuggestionsS2CPacket.Suggestion::text)
                        .toList()
        );
    }
}