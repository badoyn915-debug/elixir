package code.elixir.display.hud;

import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.geometry.Render2D;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;

public class Armor extends AbstractDraggable {
   private static final int WARNING_PERCENT = 15;
   private static final int CRITICAL_PERCENT = 5;
   private final List<ItemStack> armorStacks = new ArrayList<>();
   private final Map<EquipmentSlot, Item> trackedItems = new EnumMap<>(EquipmentSlot.class);
   private final Map<EquipmentSlot, Integer> warningStage = new EnumMap<>(EquipmentSlot.class);

   public Armor() {
      super("Armor", 10, 120, 80, 15, true);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("Armor") && Hud.getInstance().state;
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         this.armorStacks.clear();
         this.trackArmorSlot(EquipmentSlot.HEAD);
         this.trackArmorSlot(EquipmentSlot.CHEST);
         this.trackArmorSlot(EquipmentSlot.LEGS);
         this.trackArmorSlot(EquipmentSlot.FEET);
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.visible()) {
         MatrixStack matrix = context.getMatrices();
         int slotSize = 18;
         int spacing = 2;
         int totalWidth = (slotSize + spacing) * 4 - spacing;
         int totalHeight = slotSize + 2;
         this.setWidth(totalWidth);
         this.setHeight(totalHeight);
         float x = this.getX();
         float y = this.getY();

         for (int i = 0; i < 4; i++) {
            float slotX = x + i * (slotSize + spacing);
            ItemStack stack = i < this.armorStacks.size() ? this.armorStacks.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
               Render2D.defaultDrawStack(context, stack, slotX, y, true, true, 1.0F);
            }
         }
      }
   }

   private void trackArmorSlot(EquipmentSlot slot) {
      ItemStack stack = mc.player.getEquippedStack(slot);
      this.armorStacks.add(stack);
      if (!stack.isEmpty() && stack.isDamageable()) {
         if (this.trackedItems.get(slot) != stack.getItem()) {
            this.trackedItems.put(slot, stack.getItem());
            this.warningStage.put(slot, 0);
         }

         Hud hud;
         if ((hud = Hud.getInstance()) != null && hud.notificationSettings.isSelected("Скорой поломке брони")) {
            int percent = Math.round((stack.getMaxDamage() - stack.getDamage()) * 100.0F / stack.getMaxDamage());
            int currentStage = this.warningStage.getOrDefault(slot, 0);
            if (percent > 15) {
               this.warningStage.put(slot, 0);
            } else if (percent <= 5 && currentStage < 2) {
               this.notifyArmorState(stack, "Критическая прочность: " + stack.getName().getString() + " (" + percent + "%)", SoundManager.DISABLE_MODULE);
               this.warningStage.put(slot, 2);
            } else {
               if (currentStage < 1) {
                  this.notifyArmorState(stack, "Скоро сломается: " + stack.getName().getString() + " (" + percent + "%)", null);
                  this.warningStage.put(slot, 1);
               }
            }
         }
      } else {
         this.trackedItems.remove(slot);
         this.warningStage.remove(slot);
      }
   }

   private void notifyArmorState(ItemStack stack, String message, SoundEvent sound) {
      Notifications.getInstance().addList(Text.literal(message), 3000, sound, false, stack.copyWithCount(1));
   }
}
