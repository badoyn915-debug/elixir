package code.elixir.features.impl.misc.astrummc;

import code.elixir.events.item.ClickSlotEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.HotBarUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.misc.astrummc.craft.LegendaryCraftDatabase;
import code.elixir.features.impl.misc.astrummc.craft.LegendaryCraftLogic;
import code.elixir.features.impl.misc.astrummc.craft.LegendaryCraftNotifier;
import code.elixir.features.impl.misc.astrummc.craft.LegendaryCraftRecipe;
import code.elixir.features.impl.misc.astrummc.craft.LegendaryCraftRecipes;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.inv.InventoryTask;
import java.util.Map;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;

public class AstrummcHelper extends Module {
   private final LegendaryCraftNotifier notifier = new LegendaryCraftNotifier();
   private String lastInventoryFingerprint = "";

   public AstrummcHelper() {
      super("AstrummcHelper", "Astrummc Helper", ModuleCategory.MISC);
   }

   @Override
   public void activate() {
      super.activate();
      this.lastInventoryFingerprint = "";
      this.notifier.reset();
      this.checkInventory(true);
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.lastInventoryFingerprint = "";
      this.notifier.reset();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      this.checkInventory(false);
   }

   @EventHandler
   public void onClickSlot(ClickSlotEvent event) {
      this.checkInventory(false);
   }

   @EventHandler
   public void onHotBarUpdate(HotBarUpdateEvent event) {
      this.checkInventory(false);
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (event.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket) {
         this.checkInventory(false);
      }
   }

   private void checkInventory(boolean force) {
      if (mc.player != null && mc.world != null) {
         Map<String, Integer> inventory = LegendaryCraftDatabase.collectInventoryCounts(InventoryTask::getInventoryCount);
         String fingerprint = LegendaryCraftDatabase.buildFingerprint(inventory);
         if (force || !fingerprint.equals(this.lastInventoryFingerprint)) {
            this.lastInventoryFingerprint = fingerprint;

            for (LegendaryCraftRecipe recipe : LegendaryCraftRecipes.ALL) {
               LegendaryCraftLogic.RecipeStatus status = LegendaryCraftLogic.evaluate(recipe, inventory);

               for (String message : this.notifier.process(recipe, status)) {
                  ChatMessage.brandmessage(message);
               }
            }
         }
      }
   }
}
