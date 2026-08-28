package code.elixir.features.impl.combat;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.packet.network.Network;
import code.elixir.utils.features.aura.warp.Turns;
import code.elixir.utils.features.rotate.RotationUtils;
import code.elixir.utils.interactions.inv.InventoryTask;
import code.elixir.utils.interactions.inv.InventoryToolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;

public class AutoPotion extends Module {
   private static final int THROW_DELAY_TICKS = 20;
   private final MultiSelectSetting throwSetting = new MultiSelectSetting("Бросать", "Какие зелья бросать под себя")
      .value("Скорость", "Сила", "Огнестойкость")
      .selected("Скорость", "Сила", "Огнестойкость");
   private final BooleanSetting onlyPvP = new BooleanSetting("Только при пвп", "Использовать только в PVP").setValue(true);
   private final BooleanSetting autoDisable = new BooleanSetting("Отключать после использования", "Выключить модуль после броска").setValue(false);
   private final BooleanSetting hotbarOnly = new BooleanSetting("Только с хотбара", "Брать зелья только из хотбара").setValue(false);
   private final BooleanSetting strengthPriority = new BooleanSetting("Приоритет силы", "Сначала сила, затем скорость и огнестойкость").setValue(true);
   private int ticksSinceThrow = 20;
   private boolean usedThisCycle;

   public AutoPotion() {
      super("AutoPotion", "AutoPotion", ModuleCategory.COMBAT);
      this.setup(new Setting[]{this.throwSetting, this.onlyPvP, this.autoDisable, this.hotbarOnly, this.strengthPriority});
   }

   @Override
   public void activate() {
      this.ticksSinceThrow = 20;
      this.usedThisCycle = false;
      super.activate();
   }

   @Override
   public void deactivate() {
      this.ticksSinceThrow = 20;
      this.usedThisCycle = false;
      super.deactivate();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player != null && mc.world != null && mc.getNetworkHandler() != null) {
         if (!this.autoDisable.isValue() || !this.usedThisCycle) {
            if (!this.onlyPvP.isValue() || Network.isPvp()) {
               if (!mc.player.isGliding() && (!mc.player.isUsingItem() || mc.player.isBlocking())) {
                  this.ticksSinceThrow++;
                  if (this.ticksSinceThrow >= 20) {
                     List<Integer> slots = this.collectSlots();
                     if (!slots.isEmpty()) {
                        if (!this.hotbarOnly.isValue()
                           || !slots.stream().anyMatch(slot -> slot >= 9)
                           || !(slots = slots.stream().filter(slot -> slot < 9).toList()).isEmpty()) {
                           Turns angle = this.computeDownAngle();
                           this.syncRotation(angle);
                           this.throwPotions(slots, angle);
                           this.ticksSinceThrow = 0;
                           this.usedThisCycle = true;
                           if (this.autoDisable.isValue()) {
                              this.setState(false);
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   private List<Integer> collectSlots() {
      ArrayList<Integer> slots = new ArrayList<>();
      if (this.strengthPriority.isValue()) {
         this.addIfNeeded(slots, "Сила", StatusEffects.STRENGTH);
         this.addIfNeeded(slots, "Скорость", StatusEffects.SPEED);
         this.addIfNeeded(slots, "Огнестойкость", StatusEffects.FIRE_RESISTANCE);
      } else {
         this.addIfNeeded(slots, "Скорость", StatusEffects.SPEED);
         this.addIfNeeded(slots, "Сила", StatusEffects.STRENGTH);
         this.addIfNeeded(slots, "Огнестойкость", StatusEffects.FIRE_RESISTANCE);
      }

      return slots;
   }

   private void addIfNeeded(List<Integer> slots, String label, RegistryEntry<StatusEffect> effect) {
      if (this.throwSetting.isSelected(label) && !mc.player.hasStatusEffect(effect)) {
         Slot slot = this.findSplash(effect);
         if (slot != null) {
            slots.add(slot.id);
         }
      }
   }

   private Slot findSplash(RegistryEntry<StatusEffect> effect) {
      Slot hotbar = this.findInRange(effect, 36, 45);
      if (hotbar != null) {
         return hotbar;
      } else {
         return this.hotbarOnly.isValue() ? null : this.findInRange(effect, 9, 36);
      }
   }

   private Slot findInRange(RegistryEntry<StatusEffect> effect, int fromInclusive, int toExclusive) {
      for (Slot slot : InventoryTask.slots().toList()) {
         if (slot.id >= fromInclusive && slot.id < toExclusive && this.isSplashWithEffect(slot.getStack(), effect)) {
            return slot;
         }
      }

      return null;
   }

   private boolean isSplashWithEffect(ItemStack stack, RegistryEntry<StatusEffect> effect) {
      if (stack != null && stack.isOf(Items.SPLASH_POTION)) {
         PotionContentsComponent contents = (PotionContentsComponent)stack.get(DataComponentTypes.POTION_CONTENTS);
         if (contents == null) {
            return false;
         } else {
            boolean hasEffect = false;

            for (StatusEffectInstance instance : contents.getEffects()) {
               if (((StatusEffect)instance.getEffectType().value()).getCategory() == StatusEffectCategory.HARMFUL) {
                  return false;
               }

               if (instance.getEffectType().equals(effect)) {
                  hasEffect = true;
               }
            }

            return hasEffect;
         }
      } else {
         return false;
      }
   }

   private Turns computeDownAngle() {
      float[] rotation = RotationUtils.correctRotation(new float[]{mc.player.getYaw(), 90.0F + ThreadLocalRandom.current().nextFloat(-1.5F, 1.5F)});
      return new Turns(rotation[0], rotation[1]).adjustSensitivity();
   }

   private void syncRotation(Turns angle) {
      mc.getNetworkHandler()
         .sendPacket(
            new Full(
               mc.player.getX(), mc.player.getY(), mc.player.getZ(), angle.getYaw(), angle.getPitch(), mc.player.isOnGround(), mc.player.horizontalCollision
            )
         );
   }

   private void throwPotions(List<Integer> screenSlots, Turns angle) {
      int originalHotbar = mc.player.getInventory().selectedSlot;
      int syncId = mc.player.currentScreenHandler.syncId;
      boolean changedSlot = false;

      for (int screenSlot : screenSlots) {
         if (screenSlot >= 36 && screenSlot <= 44) {
            int hotbar = screenSlot - 36;
            if (hotbar != originalHotbar) {
               mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(hotbar));
               changedSlot = true;
            }

            this.sendThrow(angle);
         } else {
            mc.interactionManager.clickSlot(syncId, screenSlot, originalHotbar, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalHotbar));
            this.sendThrow(angle);
            mc.interactionManager.clickSlot(syncId, screenSlot, originalHotbar, SlotActionType.SWAP, mc.player);
            changedSlot = true;
         }
      }

      if (changedSlot) {
         mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(originalHotbar));
      }
   }

   private void sendThrow(Turns angle) {
      InventoryToolkit.sendSequencedPacket(seq -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, seq, angle.getYaw(), angle.getPitch()));
   }
}
