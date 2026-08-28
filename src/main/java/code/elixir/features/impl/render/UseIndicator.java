package code.elixir.features.impl.render;

import code.elixir.events.render.DrawEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.UseIndicatorRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Hand;

public class UseIndicator extends Module {
   public static UseIndicator getInstance() {
      return Instance.get(UseIndicator.class);
   }

   public UseIndicator() {
      super("UseIndicator", "UseIndicator", ModuleCategory.RENDER);
      this.setState(true);
   }

   @EventHandler
   public void onDraw(DrawEvent event) {
      if (mc.world != null && mc.player != null && mc.gameRenderer != null && event.getDrawContext() != null) {
         Camera camera = mc.gameRenderer.getCamera();

         for (PlayerEntity player : mc.world.getPlayers()) {
            ItemStack stack;
            if (this.shouldRender(player) && !(stack = this.resolveConsumableStack(player)).isEmpty()) {
               UseIndicatorRenderer.render(event.getDrawContext(), player, stack, camera);
            }
         }
      }
   }

   private boolean shouldRender(LivingEntity entity) {
      if (!entity.isAlive() || !entity.isUsingItem()) {
         return false;
      } else if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) {
         return false;
      } else {
         return entity.isInvisible() && entity != mc.player ? false : !this.resolveConsumableStack(entity).isEmpty();
      }
   }

   private ItemStack resolveConsumableStack(LivingEntity entity) {
      if (!entity.isUsingItem()) {
         return ItemStack.EMPTY;
      } else {
         ItemStack activeItem = entity.getActiveItem();
         if (this.isConsumable(activeItem)) {
            return activeItem;
         } else {
            Hand activeHand = entity.getActiveHand();
            ItemStack handStack;
            return activeHand != null && this.isConsumable(handStack = entity.getStackInHand(activeHand)) ? handStack : ItemStack.EMPTY;
         }
      }
   }

   private boolean isConsumable(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         UseAction action = stack.getUseAction();
         return action == UseAction.EAT || action == UseAction.DRINK;
      } else {
         return false;
      }
   }
}
