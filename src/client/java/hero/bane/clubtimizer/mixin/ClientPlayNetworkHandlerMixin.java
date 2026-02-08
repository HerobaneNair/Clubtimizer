package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.action.AutoCope;
import hero.bane.clubtimizer.action.AutoGG;
import hero.bane.clubtimizer.action.AutoResponse;
import hero.bane.clubtimizer.auto.PartyMaker;
import hero.bane.clubtimizer.auto.Rematch;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.auto.TotemReset;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.FriendUtil;
import hero.bane.clubtimizer.util.TextUtil;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.CommandSuggestionsS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onGameMessage", at = @At("HEAD"))
    private void club$onGameMessage(GameMessageS2CPacket packet, CallbackInfo ci) {
        if(MCPVPStateChanger.get() == MCPVPState.NONE) return;
        Text original = packet.content();

        String legacyString = TextUtil.toLegacyString(original);
        String noFormatting = original.getString();

        AutoResponse.handleMessage(legacyString, noFormatting);

        AutoCope.handleMessage(noFormatting);
        AutoGG.handleMessage(noFormatting);
        PartyMaker.handleMessage(noFormatting);
        Rematch.handleMessage(noFormatting);
        Spectator.handleMessage(noFormatting);
        TotemReset.handleMessage(noFormatting);
    }

    @Inject(method = "onCommandSuggestions", at = @At("HEAD"))
    private void club$captureSuggestions(CommandSuggestionsS2CPacket packet, CallbackInfo ci) {
        if(MCPVPStateChanger.get() == MCPVPState.NONE) return;
        if (!FriendUtil.isOurRequest(packet.id())) return;

        List<String> list = new ArrayList<>();
        packet.suggestions().forEach(s -> list.add(s.text()));

        FriendUtil.replaceAll(list);
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"))
    private void onSendChatCommand(String command, CallbackInfo ci) {
        if(MCPVPStateChanger.get() == MCPVPState.NONE) return;
        if(command.startsWith("party")) PartyMaker.lastPartyCommand = "/" + command;
    }

    @Inject(method = "onEntitiesDestroy", at = @At("HEAD"))
    private void club$specDeath(EntitiesDestroyS2CPacket packet, CallbackInfo ci) {
        Spectator.onEntitiesDestroyed(packet.getEntityIds());
    }
}