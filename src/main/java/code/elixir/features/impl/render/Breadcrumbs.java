package code.elixir.features.impl.render;

import code.elixir.events.player.TickEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.RadioSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.color.ColorAssist;
import code.elixir.utils.display.render.geometry.BufferDraw;
import code.elixir.utils.theme.ThemeManager;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Breadcrumbs extends Module {
   private final SliderSettings length = new SliderSettings("Длительность (сек)", "Время жизни следа")
      .range(1.0F, 15.0F)
      .setValue(4.0F)
      .step(0.5F);
   private final RadioSetting style = new RadioSetting(
      "Стиль", "Стиль отрисовки следов", new String[]{"Лента", "Линия"}, "Лента"
   );
   private final RadioSetting colorMode = new RadioSetting(
      "Режим цвета", "Выбор источника цвета", new String[]{"Клиентский", "Градиент", "Кастом"}, "Клиентский"
   );
   private final ColorSetting customColor = new ColorSetting("Цвет 1", "Основной цвет следа")
      .value(new Color(0, 246, 255, 255).getRGB())
      .visible(() -> !this.colorMode.get().equals("Клиентский"));
   private final ColorSetting secondaryColor = new ColorSetting("Цвет 2", "Второй цвет градиента")
      .value(new Color(183, 1, 195, 255).getRGB())
      .visible(() -> this.colorMode.get().equals("Градиент"));

   private final List<BreadcrumbPoint> points = new ArrayList<>();

   public static class BreadcrumbPoint {
      public final Vec3d pos;
      public final long time;

      public BreadcrumbPoint(Vec3d pos, long time) {
         this.pos = pos;
         this.time = time;
      }
   }

   public static Breadcrumbs getInstance() {
      return Instance.get(Breadcrumbs.class);
   }

   public Breadcrumbs() {
      super("Breadcrumbs", "Breadcrumbs", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.length, this.style, this.colorMode, this.customColor, this.secondaryColor});
   }

   @Override
   public void deactivate() {
      this.points.clear();
      super.deactivate();
   }

   @EventHandler
   public void onTick(TickEvent event) {
      if (mc.player == null || mc.world == null) return;

      Vec3d currentPos = mc.player.getPos().add(0, 0.05, 0);
      long now = System.currentTimeMillis();
      long maxAge = (long)(this.length.getValue() * 1000.0F);

      // Only add point if moved sufficiently
      if (this.points.isEmpty() || this.points.get(this.points.size() - 1).pos.distanceTo(currentPos) > 0.08) {
         this.points.add(new BreadcrumbPoint(currentPos, now));
      }

      // Remove old points
      Iterator<BreadcrumbPoint> it = this.points.iterator();
      while (it.hasNext()) {
         BreadcrumbPoint pt = it.next();
         if (now - pt.time > maxAge) {
            it.remove();
         }
      }
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null || this.points.size() < 2) return;

      float partialTicks = event.getPartialTicks();
      MatrixStack matrices = event.getStack();
      Matrix4f mat = matrices.peek().getPositionMatrix();

      long now = System.currentTimeMillis();
      long maxAge = (long)(this.length.getValue() * 1000.0F);
      boolean isRibbon = "Лента".equals(this.style.get());

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);

      if (isRibbon) {
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
         float height = 0.25F;

         for (int i = 0; i < this.points.size(); i++) {
            BreadcrumbPoint pt = this.points.get(i);
            float ageFraction = MathHelper.clamp((float)(now - pt.time) / maxAge, 0.0F, 1.0F);
            float alpha = (1.0F - ageFraction) * 0.85F;

            int color = this.getColorForPoint((float)i / this.points.size(), alpha);

            buffer.vertex(mat, (float)pt.pos.x, (float)pt.pos.y, (float)pt.pos.z).color(color);
            buffer.vertex(mat, (float)pt.pos.x, (float)(pt.pos.y + height), (float)pt.pos.z).color(color);
         }

         BufferDraw.draw(buffer);
      } else {
         BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

         for (int i = 0; i < this.points.size() - 1; i++) {
            BreadcrumbPoint p1 = this.points.get(i);
            BreadcrumbPoint p2 = this.points.get(i + 1);

            float age1 = MathHelper.clamp((float)(now - p1.time) / maxAge, 0.0F, 1.0F);
            float age2 = MathHelper.clamp((float)(now - p2.time) / maxAge, 0.0F, 1.0F);

            int c1 = this.getColorForPoint((float)i / this.points.size(), (1.0F - age1) * 0.9F);
            int c2 = this.getColorForPoint((float)(i + 1) / this.points.size(), (1.0F - age2) * 0.9F);

            buffer.vertex(mat, (float)p1.pos.x, (float)p1.pos.y, (float)p1.pos.z).color(c1);
            buffer.vertex(mat, (float)p2.pos.x, (float)p2.pos.y, (float)p2.pos.z).color(c2);
         }

         BufferDraw.draw(buffer);
      }

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
   }

   private int getColorForPoint(float progress, float alpha) {
      String mode = this.colorMode.get();
      if ("Градиент".equals(mode)) {
         int c1 = this.customColor.getColor();
         int c2 = this.secondaryColor.getColor();
         int blended = ColorAssist.interpolateColor(c1, c2, progress);
         return ColorAssist.multAlpha(blended, alpha);
      } else if ("Кастом".equals(mode)) {
         return ColorAssist.multAlpha(this.customColor.getColor(), alpha);
      } else {
         int c1 = ThemeManager.primaryColor.getColor();
         int c2 = ThemeManager.secondaryColor.getColor();
         int blended = ColorAssist.interpolateColor(c1, c2, progress);
         return ColorAssist.multAlpha(blended, alpha);
      }
   }
}
