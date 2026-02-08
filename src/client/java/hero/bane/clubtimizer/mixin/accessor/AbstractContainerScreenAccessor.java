package hero.bane.clubtimizer.mixin.accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.jspecify.annotations.Nullable;

import java.util.List;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Invoker("getHoveredSlot")
    @Nullable
    Slot invokeGetHoveredSlot(double mouseX, double mouseY);

    @Invoker("getTooltipFromContainerItem")
    List<Component> invokeGetTooltipFromContainerItem(ItemStack stack);
}
