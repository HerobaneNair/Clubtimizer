package hero.bane.clubtimizer.mixin.accessor;

import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInfo.class)
public interface PlayerListEntryAccessor {

    @Accessor("latency")
    void setLatency(int latency);
}
