package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.action.Cope;
import hero.bane.clubtimizer.action.GG;
import hero.bane.clubtimizer.action.Response;
import hero.bane.clubtimizer.auto.PartyMaker;
import hero.bane.clubtimizer.auto.Rematch;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.auto.Totem;
import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PlayerUtil;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "handleSystemChat", at = @At("HEAD"))
    private void club$onGameMessage(ClientboundSystemChatPacket packet, CallbackInfo ci) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;

        Component original = packet.content();
//        String legacyString = TextUtil.toLegacyString(original);
        String noFormatting = original.getString();

        Response.handleMessage(noFormatting);
        Cope.handleMessage(noFormatting);
        GG.handleMessage(noFormatting);
        PartyMaker.handleMessage(noFormatting);
        Rematch.handleMessage(noFormatting);
        Spectator.handleMessage(noFormatting);
        Totem.handleMessage(noFormatting);
    }

    @Inject(method = "handleCommandSuggestions", at = @At("HEAD"))
    private void club$captureSuggestions(ClientboundCommandSuggestionsPacket packet, CallbackInfo ci) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;
        if (!PlayerUtil.isOurRequest(packet.id())) return;

        List<String> list = new ArrayList<>();
        packet.suggestions().forEach(e -> list.add(e.text()));

        PlayerUtil.replaceAll(list);
    }

    @Inject(method = "sendCommand", at = @At("HEAD"))
    private void club$resendPartyCommand(String command, CallbackInfo ci) {
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return;
        if (!command.startsWith("party ")) return;

        int firstSpace = command.indexOf(' ');
        if (firstSpace == -1 || firstSpace == command.length() - 1) return;

        String sub = command.substring(firstSpace + 1);

        if (!PartyMaker.PARTY_COMMAND_TAB_COMPLETES.contains(sub)) {
            PartyMaker.lastPartyCommand = "/" + command;
        } else {
            PartyMaker.lastPartyCommand = "";
        }
    }

    @Inject(method = "handleRemoveEntities", at = @At("HEAD"))
    private void club$specDeath(ClientboundRemoveEntitiesPacket packet, CallbackInfo ci) {
        Spectator.onEntitiesDestroyed(packet.getEntityIds());
    }
}
