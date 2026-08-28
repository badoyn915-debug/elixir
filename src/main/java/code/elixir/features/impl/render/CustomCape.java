package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.impl.render.customcape.CapeSyncManager;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.util.Identifier;

public class CustomCape extends Module {
   public static final Identifier CAPE_TEXTURE = Identifier.of("elixir", "textures/cape.png");

   public static CustomCape getInstance() {
      return Instance.get(CustomCape.class);
   }

   public CustomCape() {
      super("CustomCape", "CustomCape", ModuleCategory.RENDER);
      this.setState(true);
   }

   public boolean shouldApply(AbstractClientPlayerEntity player) {
      if (this.isState() && player != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client.player != null && client.player.getUuid().equals(player.getUuid())) {
            return true;
         } else {
            CapeSyncManager syncManager = CapeSyncManager.getInstance();
            return syncManager != null && syncManager.hasRemoteCape(player.getUuid()) ? true : FriendUtils.isFriend(player);
         }
      } else {
         return false;
      }
   }

   public static SkinTextures withCustomCape(SkinTextures original) {
      return original == null
         ? null
         : new SkinTextures(original.texture(), original.textureUrl(), CAPE_TEXTURE, original.elytraTexture(), original.model(), original.secure());
   }
}
