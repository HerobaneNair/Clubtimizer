package hero.bane.mixin;

import hero.bane.auto.FriendList;
import hero.bane.mixin.accessor.ClientCommandSourceAccessor;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.RequestCommandCompletionsC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ClientCommandSource.class)
public class ClientCommandSourceMixin implements ClientCommandSourceAccessor {

    @Shadow private int completionId;
    @Shadow @Final private ClientPlayNetworkHandler networkHandler;

    @Override
    public void club$requestFriendList() {
        int id = ++this.completionId;
        FriendList.pendingId = id;
        this.networkHandler.sendPacket(
                new RequestCommandCompletionsC2SPacket(
                        id,
                        "/friend remove "
                )
        );
    }
}
