package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.features.impl.render.custommodels.CustomModelSyncManager;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.utils.client.Instance;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

public class CustomModelModule extends Module {
   private static CustomModelModule instance;
   public final SelectSetting models = new SelectSetting("Model", "Select custom player model")
      .value("White Demon", "Red Demon", "Crazy Rabbit", "Freddy Bear", "Sonic", "Amogus", "Jeff Killer", "Crab", "Chinchilla")
      .selected("White Demon");
   public final BooleanSetting friends = new BooleanSetting("Apply to Friends", "Apply custom models to friends").setValue(true);

   public CustomModelModule() {
      super("CustomModel", "Custom Models", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.models, this.friends});
      instance = this;
   }

   public static CustomModelModule getInstance() {
      return instance;
   }

   @Nullable
   public static String getModelType(@Nullable PlayerEntity player) {
      CustomModelModule module = Instance.get(CustomModelModule.class);
      if (module == null || !module.isState()) {
         return null;
      } else if (player == null) {
         return null;
      } else if (player == MinecraftClient.getInstance().player) {
         return module.models.getSelected();
      } else {
         CustomModelSyncManager syncManager = CustomModelSyncManager.getInstance();
         CustomModelSyncManager.RemoteModelState remoteState;
         if (syncManager != null && (remoteState = syncManager.getRemoteState(player.getUuid())) != null) {
            return remoteState.enabled() ? remoteState.modelType() : null;
         } else {
            return module.friends.isValue() && FriendUtils.isFriend(player) ? module.models.getSelected() : null;
         }
      }
   }
}
