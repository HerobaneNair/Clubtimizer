package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.gui.hud.spectator.TeleportSpectatorMenu;
import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(TeleportSpectatorMenu.class)
public abstract class TeleportSpectatorMenuMixin {

    @ModifyVariable(
            method = "<init>(Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static Collection<PlayerListEntry> club$replaceEntryList(Collection<PlayerListEntry> original) {
        if (Clubtimizer.player == null || !Clubtimizer.player.isSpectator()) return original;
        if (!MCPVPStateChanger.inSpec()) return original;
        if (!Spectator.hasEntries()) return original;

        List<PlayerListEntry> result = new ArrayList<>();

        for (String clean : Spectator.getAll()) {
            PlayerListEntry e = Spectator.findEntryByCleanName(clean);
            if (e != null) {
                result.add(e);
            }
        }

        return result.isEmpty() ? original : result;
    }
}
