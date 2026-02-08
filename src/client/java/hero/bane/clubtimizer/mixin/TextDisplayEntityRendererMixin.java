package hero.bane.clubtimizer.mixin;

import hero.bane.clubtimizer.state.MCPVPState;
import hero.bane.clubtimizer.state.MCPVPStateChanger;
import hero.bane.clubtimizer.util.IconUtil;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class TextDisplayEntityRendererMixin {

    @ModifyVariable(
            method = "getLines(Lnet/minecraft/text/Text;I)Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;",
            at = @At("HEAD"),
            argsOnly = true
    )
    private Text club$replaceTextDisplay(Text text) {
        if (text == null) return null;
        if (MCPVPStateChanger.get().equals(MCPVPState.NONE)) return text;
        return IconUtil.remapIcons(text);
    }
}
