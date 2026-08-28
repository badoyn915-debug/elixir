package code.elixir.mixins;

import code.elixir.utils.display.interfaces.IArmorItem;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item.Settings;
import net.minecraft.item.equipment.ArmorMaterial;
import net.minecraft.item.equipment.EquipmentType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ArmorItem.class})
public abstract class ArmorItemMixin implements IArmorItem {
   @Unique
   private ArmorMaterial armorMaterial;
   @Unique
   private EquipmentType type;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void hookCatchArgs(ArmorMaterial material, EquipmentType type, Settings settings, CallbackInfo ci) {
      this.armorMaterial = material;
      this.type = type;
   }

   @Override
   public ArmorMaterial zov_pidarok$getMaterial() {
      return this.armorMaterial;
   }

   @Override
   public EquipmentType zov_pidarok$getType() {
      return this.type;
   }
}
