package code.elixir.display.hud;

import code.elixir.common.animation.implement.EaseOut;
import code.elixir.features.impl.render.Hud;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import code.elixir.utils.client.sound.SoundManager;
import code.elixir.utils.display.render.font.FontRenderer;
import code.elixir.utils.display.render.font.Fonts;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.display.render.post.KawaseBlur;
import code.elixir.utils.display.render.shape.ShapeProperties;
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
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

public class ArmorBar extends AbstractDraggable {
   private static final int WARNING_PERCENT = 15;
   private static final int CRITICAL_PERCENT = 5;
   private final List<ItemStack> armorStacks = new ArrayList<>();
   private final Map<EquipmentSlot, Item> trackedItems = new EnumMap<>(EquipmentSlot.class);
   private final Map<EquipmentSlot, Integer> warningStage = new EnumMap<>(EquipmentSlot.class);
   private final FontRenderer durabilityFont = Fonts.getSize(10, Fonts.Type.DEFAULT);

   public ArmorBar() {
      super("ArmorBar", 0, 0, 150, 24, false);
      this.scaleAnimation = new EaseOut().setValue(1.0).setMs(100);
   }

   @Override
   public boolean visible() {
      Hud.getInstance();
      return Hud.interfaceSettings.isSelected("ArmorBar") && Hud.getInstance().state;
   }

   @Override
   public void tick() {
      if (mc.player != null) {
         this.armorStacks.clear();
         this.trackArmorSlot(EquipmentSlot.FEET);
         this.trackArmorSlot(EquipmentSlot.LEGS);
         this.trackArmorSlot(EquipmentSlot.CHEST);
         this.trackArmorSlot(EquipmentSlot.HEAD);
      }
   }

   @Override
   public void drawDraggable(DrawContext context) {
      if (this.visible()) {
         MatrixStack matrix = context.getMatrices();
         float slotSize = 20.0F;
         float spacing = 3.0F;
         float padding = 3.0F;
         float durabilityBarHeight = 2.0F;
         float barSpacing = 2.0F;
         float totalWidth = (slotSize + spacing) * 4.0F - spacing + padding * 2.0F;
         float totalHeight = slotSize + padding * 2.0F + barSpacing + durabilityBarHeight;
         int screenWidth = window.getScaledWidth();
         int screenHeight = window.getScaledHeight();
         CustomHotbar.HotbarLayout layout = CustomHotbar.computeLayout(screenWidth, screenHeight);
         float armorPanelX = layout.hotbarPanelX() + layout.hotbarPanelWidth() + CustomHotbar.getArmorBarGap();
         float armorPanelY = layout.hotbarPanelY();
         if (armorPanelX + totalWidth > screenWidth - 1.0F) {
            armorPanelX = screenWidth - 1.0F - totalWidth;
         }

         this.setX((int)armorPanelX);
         this.setY((int)armorPanelY);
         this.setWidth((int)totalWidth);
         this.setHeight((int)totalHeight);
         this.clampToScreen();
         float x = armorPanelX;
         float y = armorPanelY;
         Hud.getInstance();
         if (Hud.blur.isValue() && !KawaseBlur.INSTANCE.fbos.isEmpty()) {
            Render2D.rectangleWithMask(
               matrix.peek().getPositionMatrix(),
               armorPanelX,
               armorPanelY,
               totalWidth,
               totalHeight,
               6.0F,
               0,
               KawaseBlur.INSTANCE.fbos.getFirst().getColorAttachment()
            );
         }

         rectangle.render(
            ShapeProperties.create(matrix, armorPanelX, armorPanelY, totalWidth, totalHeight)
               .round(6.0F)
               .softness(2.5F)
               .color(new Color(0, 0, 0, 210).getRGB())
               .build()
         );

         for (int i = 0; i < 4; i++) {
            float slotX = x + padding + i * (slotSize + spacing);
            float slotY = y + padding;
            rectangle.render(
               ShapeProperties.create(matrix, slotX, slotY, slotSize, slotSize).round(4.0F).color(ThemeManager.BackgroundSettings.getColor()).build()
            );
            ItemStack stack = i < this.armorStacks.size() ? this.armorStacks.get(i) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
               float itemSize = 16.0F;
               float itemX = slotX + (slotSize - itemSize) / 2.0F - 1.0F;
               float itemY = slotY + (slotSize - itemSize) / 2.0F;
               Render2D.defaultDrawStack(context, stack, itemX, itemY, false, false, 1.0F);
               if (stack.isDamageable()) {
                  int maxDurability = stack.getMaxDamage();
                  int currentDurability = maxDurability - stack.getDamage();
                  float durabilityPercent = (float)currentDurability / maxDurability;
                  float barX = slotX + 2.0F;
                  float barY = slotY + slotSize + barSpacing;
                  float barWidth = slotSize - 4.0F;
                  rectangle.render(
                     ShapeProperties.create(matrix, barX, barY, barWidth, durabilityBarHeight).round(1.0F).color(new Color(0, 0, 0, 210).getRGB()).build()
                  );
                  Color barColor = durabilityPercent > 0.5F
                     ? new Color(76, 187, 23)
                     : (durabilityPercent > 0.25F ? new Color(255, 187, 40) : new Color(255, 51, 51));
                  float filledWidth = barWidth * durabilityPercent;
                  if (filledWidth > 0.5F) {
                     rectangle.render(ShapeProperties.create(matrix, barX, barY, filledWidth, durabilityBarHeight).round(1.0F).color(barColor.getRGB()).build());
                  }
               }
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
