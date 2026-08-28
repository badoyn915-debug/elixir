package code.elixir.features.impl.combat;

import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.render.Hud;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.interfaces.IArmorItem;
import code.elixir.utils.interactions.inv.InventoryFlowManager;
import code.elixir.utils.interactions.inv.InventoryTask;
import java.util.ArrayList;
import java.util.Comparator;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EquipmentSlot.Type;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

public class AutoArmor extends Module {
   public AutoArmor() {
      super("AutoArmor", "AutoArmor", ModuleCategory.PLAYER);
      this.setup(new Setting[0]);
   }

   @EventHandler
   public void onTick(TickEvent e) {
      if (!InventoryTask.isServerScreen()) {
         if (InventoryFlowManager.script.isFinished()) {
            ArrayList<Runnable> list = new ArrayList<>();

            for (EquipmentSlot equipment : EquipmentSlot.values()) {
               ItemStack equipStack = mc.player.getInventory().getArmorStack(equipment.getEntitySlotId());
               if (equipment.getType() == Type.HUMANOID_ARMOR && (!equipment.equals(EquipmentSlot.CHEST) || !equipStack.getItem().equals(Items.ELYTRA))) {
                  int armorSlot = 8 - equipment.getEntitySlotId();
                  Slot slot = InventoryTask.getSlot(
                     s -> {
                        ItemStack stack = s.getStack();
                        ArmorItem armorItem;
                        Item patt0$temp;
                        return s.id != armorSlot
                           && !this.isBroken(stack)
                           && !this.hasCurseOfBinding(stack)
                           && (patt0$temp = stack.getItem()) instanceof ArmorItem
                           && ((IArmorItem)(armorItem = (ArmorItem)patt0$temp)).zov_pidarok$getType().getEquipmentSlot().equals(equipment);
                     },
                     Comparator.comparingDouble(s -> this.calculateArmorValue(s.getStack(), (IArmorItem)s.getStack().getItem()))
                  );
                  if (slot != null && this.isBetter(slot.getStack(), equipStack)) {
                     list.add(() -> InventoryTask.moveItem(slot, armorSlot));
                  } else if (this.isBroken(equipStack)) {
                     Hud hud = Hud.getInstance();
                     if (slot != null) {
                        list.add(() -> InventoryTask.moveItem(slot, armorSlot));
                     } else if (InventoryTask.getSlot(Items.AIR, s -> s.id >= 9) != null) {
                        list.add(() -> InventoryTask.clickSlot(armorSlot, 0, SlotActionType.QUICK_MOVE, false));
                     }
                  }
               }
            }

            if (!list.isEmpty()) {
               InventoryFlowManager.addTask(() -> {
                  list.forEach(Runnable::run);
                  InventoryTask.updateSlots();
               });
            }
         }
      }
   }

   private float calculateArmorValue(ItemStack stack, IArmorItem armorItem) {
      Registry enchantments = mc.world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
      ArmorMaterial material = armorItem.zov_pidarok$getMaterial();
      float protection = material.defense().getOrDefault(armorItem.zov_pidarok$getType(), 0).intValue()
         + material.toughness()
         + EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.PROTECTION.getValue()).orElseThrow(), stack);
      float unbreaking = EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.UNBREAKING.getValue()).orElseThrow(), stack);
      float mending = EnchantmentHelper.getLevel((RegistryEntry)enchantments.getEntry(Enchantments.MENDING.getValue()).orElseThrow(), stack);
      return protection + unbreaking * 0.1F + mending * 0.2F;
   }

   private boolean hasCurseOfBinding(ItemStack stack) {
      RegistryEntry curseEntry = (RegistryEntry)mc.world
         .getRegistryManager()
         .getOrThrow(RegistryKeys.ENCHANTMENT)
         .getEntry(Enchantments.BINDING_CURSE.getValue())
         .orElse(null);
      return curseEntry == null ? false : EnchantmentHelper.getLevel(curseEntry, stack) > 0;
   }

   private boolean isBroken(ItemStack stack) {
      return (double)stack.getDamage() / stack.getMaxDamage() > 0.98;
   }

   private boolean isBetter(ItemStack newArmor, ItemStack currentArmor) {
      if (currentArmor.isEmpty()) {
         return true;
      } else {
         return newArmor.getItem() instanceof ArmorItem newItem && currentArmor.getItem() instanceof ArmorItem currentItem
            ? this.calculateArmorValue(newArmor, (IArmorItem)newItem) > this.calculateArmorValue(currentArmor, (IArmorItem)currentItem)
            : false;
      }
   }

   private String equipName(EquipmentSlot equipmentSlot) {
      return switch (equipmentSlot) {
         case FEET -> "Ботинки";
         case LEGS -> "Поножи";
         case CHEST -> "Нагрудник";
         case HEAD -> "Шлем";
         default -> "ДЕЗЗ СВИНЬЯ СУКААА!";
      };
   }
}
