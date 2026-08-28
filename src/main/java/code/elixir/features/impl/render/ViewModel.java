package code.elixir.features.impl.render;

import code.elixir.events.item.HandOffsetEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;

public class ViewModel extends Module {
   private final SliderSettings mainHandXSetting = new SliderSettings("Основная рука X", "Настройка значения X для основной руки")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings mainHandYSetting = new SliderSettings("Основная рука Y", "Настройка значения Y для основной руки")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings mainHandZSetting = new SliderSettings("Основная рука Z", "Настройка значения Z для основной руки")
      .setValue(0.0F)
      .range(-2.5F, 2.5F);
   private final SliderSettings offHandXSetting = new SliderSettings("Второстепенная рука X", "Настройка значения X для второстепенной руки")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings offHandYSetting = new SliderSettings("Второстепенная рука Y", "Настройка значения Y для второстепенной руки")
      .setValue(0.0F)
      .range(-1.0F, 1.0F);
   private final SliderSettings offHandZSetting = new SliderSettings("Второстепенная рука Z", "Настройка значения Z для второстепенной руки")
      .setValue(0.0F)
      .range(-2.5F, 2.5F);
   private final SliderSettings mainHandScaleSetting = new SliderSettings("Размер основной руки", "Настройка размера предмета в основной руке")
      .setValue(1.0F)
      .range(0.1F, 2.0F);
   private final SliderSettings offHandScaleSetting = new SliderSettings("Размер второй руки", "Настройка размера предмета во второстепенной руке")
      .setValue(1.0F)
      .range(0.1F, 2.0F);

   public ViewModel() {
      super("ViewModel", "ViewModel", ModuleCategory.RENDER);
      this.setup(
         new Setting[]{
            this.mainHandXSetting,
            this.mainHandYSetting,
            this.mainHandZSetting,
            this.offHandXSetting,
            this.offHandYSetting,
            this.offHandZSetting,
            this.mainHandScaleSetting,
            this.offHandScaleSetting
         }
      );
   }

   public void applyHandPosition(MatrixStack matrices, Arm arm) {
      if (arm == Arm.RIGHT) {
         matrices.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
         float scale = this.mainHandScaleSetting.getValue();
         if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
         }
      } else {
         matrices.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
         float scale = this.offHandScaleSetting.getValue();
         if (scale != 1.0F) {
            matrices.scale(scale, scale, scale);
         }
      }
   }

   @EventHandler
   public void onHandOffset(HandOffsetEvent e) {
      Hand hand = e.getHand();
      if (!hand.equals(Hand.MAIN_HAND) || !(e.getStack().getItem() instanceof CrossbowItem)) {
         MatrixStack matrix = e.getMatrices();
         if (hand.equals(Hand.MAIN_HAND)) {
            matrix.translate(this.mainHandXSetting.getValue(), this.mainHandYSetting.getValue(), this.mainHandZSetting.getValue());
            float scale = this.mainHandScaleSetting.getValue();
            if (scale != 1.0F) {
               matrix.scale(scale, scale, scale);
            }
         } else {
            matrix.translate(this.offHandXSetting.getValue(), this.offHandYSetting.getValue(), this.offHandZSetting.getValue());
            float scale = this.offHandScaleSetting.getValue();
            if (scale != 1.0F) {
               matrix.scale(scale, scale, scale);
            }
         }
      }
   }
}
