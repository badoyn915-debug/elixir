package code.elixir.mixins;

import code.elixir.events.player.InputEvent;
import code.elixir.utils.client.managers.event.SafeEvents;
import code.elixir.utils.features.aura.AuraMixinHelper;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.features.aura.warp.TurnsConstructor;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({KeyboardInput.class})
public class KeyboardInputMixin extends InputMixin {
   @ModifyExpressionValue(
      method = {"tick"},
      at = {@At(
         value = "NEW",
         target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"
      )}
   )
   private PlayerInput tickHook(PlayerInput original) {
      InputEvent event = SafeEvents.dispatch(() -> new InputEvent(original));
      if (event != null) {
         InventoryFlowManager.input(event);
      }

      return this.transformInput(event != null ? event.getInput() : original);
   }

   @Unique
   private PlayerInput transformInput(PlayerInput input) {
      if (AuraMixinHelper.hasMoveCorrection()) {
         return input;
      } else {
         TurnsConnection rotationController = TurnsConnection.INSTANCE;
         Turns angle = rotationController.getCurrentAngle();
         TurnsConstructor configurable = rotationController.getCurrentRotationPlan();
         if (mc.player != null && angle != null && configurable != null && configurable.isMoveCorrection() && configurable.isFreeCorrection()) {
            float deltaYaw = mc.player.getYaw() - angle.getYaw();
            float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
            float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
            float newX = x * MathHelper.cos(deltaYaw * (float) (Math.PI / 180.0)) - z * MathHelper.sin(deltaYaw * (float) (Math.PI / 180.0));
            float newZ = z * MathHelper.cos(deltaYaw * (float) (Math.PI / 180.0)) + x * MathHelper.sin(deltaYaw * (float) (Math.PI / 180.0));
            int movementSideways = Math.round(newX);
            int movementForward = Math.round(newZ);
            return new PlayerInput(
               movementForward > 0.0F, movementForward < 0.0F, movementSideways > 0.0F, movementSideways < 0.0F, input.jump(), input.sneak(), input.sprint()
            );
         } else {
            return input;
         }
      }
   }
}
