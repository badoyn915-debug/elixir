package code.elixir.mixins;

import com.llamalad7.mixinextras.sugar.Local;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.option.ServerList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ServerList.class})
public class ServerListMixin {
   @Unique
   private final List<ServerInfo> sponsorServers = List.of(
      new ServerInfo("Лучший HvH сервер!", "dddd", ServerType.REALM), new ServerInfo("Много режимов + подарки!", "ddd", ServerType.REALM)
   );
   @Shadow
   @Final
   private List<ServerInfo> servers;

   @Redirect(
      method = {"saveFile"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/nbt/NbtList;add(Ljava/lang/Object;)Z",
         ordinal = 0
      )
   )
   private boolean saveFileHook(NbtList instance, Object o, @Local(ordinal = 0) ServerInfo info) {
      return this.sponsorServers.contains(info) ? true : instance.add((NbtElement)o);
   }
}
