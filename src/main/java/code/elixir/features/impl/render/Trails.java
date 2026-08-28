package code.elixir.features.impl.render;

import code.elixir.common.repository.friend.FriendUtils;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.ColorSetting;
import code.elixir.features.module.setting.implement.MultiSelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.display.render.geometry.BufferDraw;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.VertexFormat.DrawMode;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class Trails extends Module {
   private final MultiSelectSetting targetsSetting = new MultiSelectSetting("Отрисовывать на", "trails.targets.desc")
      .value("Себе", "Друзьях", "Игроках")
      .selected("Себе", "Друзьях");
   private final SliderSettings lengthSetting = new SliderSettings("Длина", "trails.length.desc").range(0.1F, 5.0F).setValue(2.5F).step(0.1F);
   private final ColorSetting color = new ColorSetting("Цвет", "Цвет следа")
      .value(new Color(150, 130, 255).getRGB())
      .presets(new Color(150, 130, 255).getRGB(), new Color(120, 180, 255).getRGB(), new Color(235, 120, 220).getRGB(), new Color(255, 255, 255).getRGB());
   private final Map<Integer, List<Trails.Trail>> trailsByEntity = new HashMap<>();

   public Trails() {
      super("Trails", "Trails", ModuleCategory.RENDER);
      this.setup(new Setting[]{this.targetsSetting, this.lengthSetting, this.color});
   }

   @Override
   public void deactivate() {
      this.trailsByEntity.clear();
      super.deactivate();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world != null && mc.player != null) {
         float partialTicks = event.getPartialTicks();
         float lengthValue = this.lengthSetting.getValue() * 1000.0F;
         int themeColor = this.color.getColor();
         ArrayList<Trails.TrailQuad> quadsToRender = new ArrayList<>();
         ArrayList<PlayerEntity> players = new ArrayList<>(mc.world.getPlayers());
         if (!players.contains(mc.player)) {
            players.add(mc.player);
         }

         for (Entity class_12972 : players) {
            if (this.isValidTarget(class_12972)
               && (class_12972 != mc.player || !mc.options.getPerspective().isFirstPerson())
               && class_12972 instanceof LivingEntity living) {
               double posX = living.prevX + (living.getX() - living.prevX) * partialTicks;
               double posY = living.prevY + (living.getY() - living.prevY) * partialTicks + 0.05 - (living.isGliding() ? 0.15 : 0.0);
               double posZ = living.prevZ + (living.getZ() - living.prevZ) * partialTicks;
               List list = this.trailsByEntity.computeIfAbsent(living.getId(), id -> new ArrayList<>());
               list.add(new Trails.Trail(posX, posY, posZ));
               float height = living.getHeight() * (living.isInSneakingPose() ? 0.8F : 1.0F);
               Trails.Trail prevTrail = null;
               boolean isFirst = true;
               int prevColor = 0;
               Iterator it = list.iterator();

               while (it.hasNext()) {
                  Trails.Trail trail = (Trails.Trail)it.next();
                  if ((float)(System.currentTimeMillis() - trail.time) > lengthValue) {
                     it.remove();
                  } else {
                     trail.alpha = Math.clamp((float)(System.currentTimeMillis() - trail.time) / lengthValue, 0.0F, 1.0F) * 255.0F;
                     int color = this.applyOpacity(themeColor, (int)(255 * (255 - Math.round(trail.alpha)) / 220.0F));
                     if (!isFirst && prevTrail != null) {
                        quadsToRender.add(
                           new Trails.TrailQuad(
                              prevTrail.x,
                              prevTrail.y,
                              prevTrail.z,
                              prevTrail.x,
                              prevTrail.y + height,
                              prevTrail.z,
                              trail.x,
                              trail.y + height,
                              trail.z,
                              trail.x,
                              trail.y,
                              trail.z,
                              prevColor,
                              color
                           )
                        );
                     }

                     isFirst = false;
                     prevTrail = trail;
                     prevColor = color;
                  }
               }

               if (list.isEmpty()) {
                  this.trailsByEntity.remove(living.getId());
               }
            }
         }

         if (!quadsToRender.isEmpty()) {
            Entry entry = event.getStack().peek();
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
            Tessellator class_2892 = Tessellator.getInstance();
            BufferBuilder buffer = class_2892.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            for (Trails.TrailQuad q : quadsToRender) {
               buffer.vertex(entry.getPositionMatrix(), (float)q.x0, (float)q.y0, (float)q.z0).color(q.color1);
               buffer.vertex(entry.getPositionMatrix(), (float)q.x1, (float)q.y1, (float)q.z1).color(q.color1);
               buffer.vertex(entry.getPositionMatrix(), (float)q.x2, (float)q.y2, (float)q.z2).color(q.color2);
               buffer.vertex(entry.getPositionMatrix(), (float)q.x3, (float)q.y3, (float)q.z3).color(q.color2);
            }

            BufferDraw.draw(buffer);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();
         }
      }
   }

   private boolean isValidTarget(Entity entity) {
      if (!entity.isAlive()) {
         return false;
      } else if (entity.isInvisible()) {
         return false;
      } else if (entity instanceof PlayerEntity player) {
         if (entity == mc.player) {
            return this.targetsSetting.isSelected("Себе");
         } else {
            return FriendUtils.isFriend(player) ? this.targetsSetting.isSelected("Друзьях") : this.targetsSetting.isSelected("Игроках");
         }
      } else {
         return false;
      }
   }

   private int applyOpacity(int color, int alpha) {
      return Math.clamp((long)alpha, 0, 255) << 24 | color & 16777215;
   }

   public static Trails getInstance() {
      return Instance.get(Trails.class);
   }

   private static final class Trail {
      final double x;
      final double y;
      final double z;
      float alpha;
      final long time;

      public Trail(double x, double y, double z) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.time = System.currentTimeMillis();
      }
   }

   private record TrailQuad(
      double x0,
      double y0,
      double z0,
      double x1,
      double y1,
      double z1,
      double x2,
      double y2,
      double z2,
      double x3,
      double y3,
      double z3,
      int color1,
      int color2
   ) {
   }
}
