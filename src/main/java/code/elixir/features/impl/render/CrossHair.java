package code.elixir.features.impl.render;

import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.Render2D;
import code.elixir.utils.math.calc.Calculate;
import net.minecraft.util.hit.EntityHitResult;

public class CrossHair extends Module {
   private float red = 0.0F;
   private final SliderSettings attackSetting = new SliderSettings("Отступ при атаке", "Отступ для кулдауна предмета").setValue(10.0F).range(0, 20);
   private final SliderSettings indentSetting = new SliderSettings("Отступ", "Отступ от центра экрана").setValue(0.0F).range(0, 5);
   private final SliderSettings size1Setting = new SliderSettings("Ширина", "Ширина прицела").setValue(4.0F).range(2, 10);
   private final SliderSettings size2Setting = new SliderSettings("Высота", "Высота прицела").setValue(1.0F).range(1, 4);

   public static CrossHair getInstance() {
      return Instance.get(CrossHair.class);
   }

   public CrossHair() {
      super("CrossHair", "CrossHair", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.attackSetting, this.indentSetting, this.size1Setting, this.size2Setting});
   }

   public void onRenderCrossHair() {
      this.red = Calculate.interpolateSmooth(2.0, this.red, mc.crosshairTarget instanceof EntityHitResult ? 5.0F : 1.0F);
      int firstColor = ColorAssist.multRed(ColorAssist.WHITE, this.red);
      int secondColor = ColorAssist.BLACK;
      float x = window.getScaledWidth() / 2.0F;
      float y = window.getScaledHeight() / 2.0F;
      float cooldown = this.attackSetting.getInt() - this.attackSetting.getInt() * mc.player.getAttackCooldownProgress(tickCounter.getTickDelta(false));
      float size = this.size1Setting.getValue();
      float size2 = this.size2Setting.getValue();
      float offset = size2 / 2.0F;
      float indent = this.indentSetting.getInt() + cooldown;
      this.renderMain(x, y, size, size2, 1.0F, indent, offset, secondColor);
      this.renderMain(x, y, size, size2, 0.0F, indent, offset, firstColor);
   }

   private void renderMain(float x, float y, float size, float size2, float padding, float indent, float offset, int color) {
      Render2D.drawQuad(x - offset - padding / 2.0F, y - size - indent - padding / 2.0F, size2 + padding, size + padding, color);
      Render2D.drawQuad(x - offset - padding / 2.0F, y + indent - padding / 2.0F, size2 + padding, size + padding, color);
      Render2D.drawQuad(x - size - indent - padding / 2.0F, y - offset - padding / 2.0F, size + padding, size2 + padding, color);
      Render2D.drawQuad(x + indent - padding / 2.0F, y - offset - padding / 2.0F, size + padding, size2 + padding, color);
   }
}
