package hero.bane.clubtimizer.mixin.accessor;

import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Gui.class)
public interface InGameHudAccessor {

    @Accessor("overlayMessageString")
    @Nullable
    Component getOverlayMessageString();

    @Accessor("overlayMessageTime")
    int getOverlayMessageTime();
}
