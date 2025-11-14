package hero.bane.mixin.accessor;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {

    @Invoker("getSlotAt")
    Slot invokeGetSlotAt(double mouseX, double mouseY);

    @Invoker("getTooltipFromItem")
    List<Text> invokeGetTooltipFromItem(ItemStack stack);
}