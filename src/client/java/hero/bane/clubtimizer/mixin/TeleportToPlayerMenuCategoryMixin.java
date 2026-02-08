package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.Clubtimizer;
import hero.bane.clubtimizer.auto.Spectator;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import net.minecraft.client.gui.spectator.categories.TeleportToPlayerMenuCategory;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(TeleportToPlayerMenuCategory.class)
public abstract class TeleportToPlayerMenuCategoryMixin {

    @ModifyVariable(
            method = "<init>(Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true
    )
    private static Collection<PlayerInfo> club$replaceEntryList(Collection<PlayerInfo> original) {
        if (Clubtimizer.player == null || !Clubtimizer.player.isSpectator()) return original;
        if (!MCPVPStateChanger.inSpec()) return original;
        if (!Spectator.hasEntries()) return original;

        List<PlayerInfo> result = new ArrayList<>();

        for (String clean : Spectator.getAll()) {
            PlayerInfo info = Spectator.findInfoByCleanName(clean);
            if (info != null) {
                result.add(info);
            }
        }

        return result.isEmpty() ? original : result;
    }
}
