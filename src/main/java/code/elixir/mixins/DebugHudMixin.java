package code.elixir.mixins;

import code.elixir.features.impl.misc.Panic;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({DebugHud.class})
public abstract class DebugHudMixin {
   @ModifyVariable(
      method = {"getLeftText"},
      at = @At("RETURN"),
      ordinal = 0
   )
   private List<String> modifyLeftText(List<String> list) {
      if (!list.isEmpty()) {
         if (Panic.isPanicMode()) {
            list.set(0, "Minecraft 1.21.4");
         } else {
            String fps = MinecraftClient.getInstance().fpsDebugString.split(" ")[0];
            list.set(0, "Elixir Recode // " + fps + "FPS");
         }
      }

      int index = -1;

      for (int i = 0; i < list.size(); i++) {
         if (list.get(i).startsWith("SC:")) {
            index = i;
            break;
         }
      }

      if (index != -1) {
         list.subList(index, list.size()).clear();
      }

      return list;
   }

   @ModifyVariable(
      method = {"getRightText"},
      at = @At("RETURN"),
      ordinal = 0
   )
   private List<String> modifyRightText(List<String> list) {
      list.removeIf(
         s -> s.contains("Targeted Block")
            || s.contains("Targeted Fluid")
            || s.contains("Targeted Entity")
            || s.startsWith("minecraft:")
            || s.startsWith("#minecraft:")
      );
      return list;
   }

   @Redirect(
      method = {"drawText"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"
      )
   )
   private void redirectFill(DrawContext instance, int x1, int y1, int x2, int y2, int color) {
      int newColor = -939524096;
      instance.fill(x1, y1, x2, y2, newColor);
   }

   @Redirect(
      method = {"getLeftText"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;getYaw()F"
      )
   )
   private float redirectYaw(Entity entity) {
      return entity == null ? 0.0F : TurnsConnection.INSTANCE.getRotation().getYaw();
   }

   @Redirect(
      method = {"getLeftText"},
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/entity/Entity;getPitch()F"
      )
   )
   private float redirectPitch(Entity entity) {
      return entity == null ? 0.0F : TurnsConnection.INSTANCE.getRotation().getPitch();
   }
}
