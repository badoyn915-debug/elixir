package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
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
import java.util.List;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class Tracers extends Module {
   private final MultiSelectSetting targets = new MultiSelectSetting("Цели", "Сущности для трассеров")
      .value("Игроки", "Друзья", "Мобы", "Предметы")
      .selected("Игроки", "Друзья");
   private final RadioSetting colorMode = new RadioSetting(
      "Режим цвета", "Выбор цвета линий", new String[]{"Клиентский", "Дистанция", "Кастом"}, "Клиентский"
   );
   private final RadioSetting origin = new RadioSetting(
      "Начало линии", "Точка исхода трассера", new String[]{"Прицел", "Ноги"}, "Прицел"
   );
   private final ColorSetting customColor = new ColorSetting("Цвет", "Цвет линий")
      .value(new Color(255, 255, 255, 200).getRGB())
      .visible(() -> this.colorMode.get().equals("Кастом"));
   private final ColorSetting friendColor = new ColorSetting("Цвет друзей", "Цвет линий к друзьям")
      .value(new Color(0, 255, 120, 220).getRGB());
   private final SliderSettings alpha = new SliderSettings("Прозрачность", "Прозрачность линий")
      .range(0.1F, 1.0F)
      .setValue(0.85F)
      .step(0.05F);

   public static Tracers getInstance() {
      return Instance.get(Tracers.class);
   }

   public Tracers() {
      super("Tracers", "Tracers", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.targets, this.colorMode, this.origin, this.customColor, this.friendColor, this.alpha});
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null) return;

      float partialTicks = event.getPartialTicks();
      MatrixStack matrices = event.getStack();
      Matrix4f mat = matrices.peek().getPositionMatrix();

      // Determine Start/Origin Point
      Vec3d startPos;
      if (this.origin.get().equals("Прицел")) {
         Vec3d camPos = mc.gameRenderer.getCamera().getPos();
         Vec3d lookVec = mc.player.getRotationVec(partialTicks);
         startPos = camPos.add(lookVec.multiply(0.3));
      } else {
         double px = MathHelper.lerp(partialTicks, mc.player.prevX, mc.player.getX());
         double py = MathHelper.lerp(partialTicks, mc.player.prevY, mc.player.getY()) + 0.1;
         double pz = MathHelper.lerp(partialTicks, mc.player.prevZ, mc.player.getZ());
         startPos = new Vec3d(px, py, pz);
      }

      List<Entity> entityList = new ArrayList<>();
      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player || !entity.isAlive()) continue;

         if (entity instanceof PlayerEntity player) {
            boolean isFriend = FriendUtils.isFriend(player);
            if (isFriend && this.targets.isSelected("Друзья")) {
               entityList.add(player);
            } else if (!isFriend && this.targets.isSelected("Игроки")) {
               entityList.add(player);
            }
         } else if (entity instanceof MobEntity && this.targets.isSelected("Мобы")) {
            entityList.add(entity);
         } else if (entity instanceof ItemEntity && this.targets.isSelected("Предметы")) {
            entityList.add(entity);
         }
      }

      if (entityList.isEmpty()) return;

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

      BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      float alphaMul = this.alpha.getValue();

      for (Entity entity : entityList) {
         double ex = MathHelper.lerp(partialTicks, entity.prevX, entity.getX());
         double ey = MathHelper.lerp(partialTicks, entity.prevY, entity.getY()) + entity.getHeight() * 0.5;
         double ez = MathHelper.lerp(partialTicks, entity.prevZ, entity.getZ());

         int color = this.getEntityColor(entity, startPos.distanceTo(new Vec3d(ex, ey, ez)), alphaMul);

         buffer.vertex(mat, (float)startPos.x, (float)startPos.y, (float)startPos.z).color(color);
         buffer.vertex(mat, (float)ex, (float)ey, (float)ez).color(color);
      }

      BufferDraw.draw(buffer);

      RenderSystem.enableDepthTest();
      RenderSystem.depthMask(true);
      RenderSystem.disableBlend();
   }

   private int getEntityColor(Entity entity, double distance, float alphaMul) {
      if (entity instanceof PlayerEntity player && FriendUtils.isFriend(player)) {
         return ColorAssist.multAlpha(this.friendColor.getColor(), alphaMul);
      }

      String mode = this.colorMode.get();
      if ("Дистанция".equals(mode)) {
         // Gradient: Red when close (<12 blocks), Yellow (12-30 blocks), Green (>30 blocks)
         float factor = MathHelper.clamp((float)(distance / 35.0), 0.0F, 1.0F);
         int r = (int)(255 * (1.0F - factor));
         int g = (int)(255 * factor);
         int b = 30;
         int a = (int)(255 * alphaMul);
         return a << 24 | r << 16 | g << 8 | b;
      } else if ("Кастом".equals(mode)) {
         return ColorAssist.multAlpha(this.customColor.getColor(), alphaMul);
      } else {
         return ColorAssist.multAlpha(ThemeManager.primaryColor.getColor(), alphaMul);
      }
   }
}
