package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.IconUtil;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class TextDisplayEntityRendererMixin {

    @ModifyVariable(
            method = "splitLines(Lnet/minecraft/network/chat/Component;I)Lnet/minecraft/world/entity/Display$TextDisplay$CachedInfo;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Component club$replaceTextDisplay(Component text) {
        if (text == null) return null;
        if (MCPVPStateChanger.get() == MCPVPState.NONE) return text;
        return IconUtil.remapIcons(text);
    }
}
