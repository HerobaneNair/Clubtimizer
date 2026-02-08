package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.PingUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerListEntry.class)
public abstract class PlayerListEntryMixin {

    @Shadow
    private Text displayName;

    @Inject(method = "getLatency", at = @At("HEAD"), cancellable = true)
    private void club$getPingFromTab(CallbackInfoReturnable<Integer> cir) {
        if (MCPVPStateChanger.get().equals(MCPVPState.NONE)) return;

        if (this.displayName == null) return;
        int parsed = PingUtil.parseTablistPing(this.displayName.getString());
        if (parsed >= 0) {
            cir.setReturnValue(parsed);
        }
    }
}
