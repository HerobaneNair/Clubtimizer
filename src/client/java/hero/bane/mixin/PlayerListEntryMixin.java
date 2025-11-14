package hero.bane.mixin;

import hero.bane.util.PingUtil;
import net.minecraft.client.MinecraftClient;
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
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getCurrentServerEntry() == null) return;
        String address = client.getCurrentServerEntry().address;
        if (address == null || !address.contains("mcpvp.club")) return;

        if (this.displayName == null) return;
        int parsed = PingUtil.parsePing(this.displayName.getString());
        if (parsed >= 0) {
            cir.setReturnValue(parsed);
        }
    }
}
