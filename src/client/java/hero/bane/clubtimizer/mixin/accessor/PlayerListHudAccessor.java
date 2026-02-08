package hero.bane.clubtimizer.mixin.accessor;

import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Mixin(PlayerListHud.class)
public interface PlayerListHudAccessor {

    @Accessor("footer") @Nullable
    Text getFooter();

    @Invoker("collectPlayerEntries")
    List<PlayerListEntry> invokeCollectPlayerEntries();
}