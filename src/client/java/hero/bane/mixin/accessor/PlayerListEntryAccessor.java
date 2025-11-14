package hero.bane.mixin.accessor;

import net.minecraft.client.network.PlayerListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerListEntry.class)
public interface PlayerListEntryAccessor {
    @Accessor("latency")
    void setLatency(int latency);
}