package code.elixir.features.impl.render.zenisesp;

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
import code.elixir.utils.theme.ThemeManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ZenisESP extends Module {
   private final RadioSetting mode = new RadioSetting(
      "Режим", "Выбор режима работы", new String[]{"Физика", "Стояк", "Голова"}, "Физика"
   );

   // Physics Mode Settings
   private final RadioSetting posture = new RadioSetting(
      "Положение", "Физическое состояние палки", new String[]{"Стоячий", "Нестоячий"}, "Стоячий"
   ).visible(() -> this.mode.get().equals("Физика"));
   private final SliderSettings stiffness = new SliderSettings("Упругость", "Сила упругого возврата в исходное положение")
      .range(0.2F, 2.5F)
      .setValue(1.0F)
      .step(0.05F)
      .visible(() -> this.mode.get().equals("Физика"));
   private final SliderSettings damping = new SliderSettings("Затухание", "Коэффициент затухания физических колебаний")
      .range(0.2F, 0.98F)
      .setValue(0.78F)
      .step(0.02F)
      .visible(() -> this.mode.get().equals("Физика"));
   private final SliderSettings gravity = new SliderSettings("Гравитация", "Влияние гравитации на палку")
      .range(0.0F, 2.0F)
      .setValue(1.0F)
      .step(0.1F)
      .visible(() -> this.mode.get().equals("Физика"));

   // Static Mode Settings
   private final SliderSettings standAngle = new SliderSettings("Угол наклона", "Угол наклона стояка")
      .range(0.0F, 90.0F)
      .setValue(35.0F)
      .step(1.0F)
      .visible(() -> this.mode.get().equals("Стояк"));

   // Head Mode Settings
   private final SliderSettings headHeightOffset = new SliderSettings("Смещение по высоте", "Высота относительно головы")
      .range(-0.3F, 0.6F)
      .setValue(0.05F)
      .step(0.05F)
      .visible(() -> this.mode.get().equals("Голова"));

   // Common Settings
   private final SliderSettings stickLength = new SliderSettings("Stick Length", "Длина палки")
      .range(0.2F, 2.5F)
      .setValue(0.85F)
      .step(0.05F);
   private final SliderSettings stickThickness = new SliderSettings("Толщина", "Толщина палки")
      .range(0.05F, 0.3F)
      .setValue(0.11F)
      .step(0.01F);
   private final MultiSelectSetting targets = new MultiSelectSetting("Цели", "Сущности для отображения")
      .value("Себя", "Друзей", "Игроков", "Мобов")
      .selected("Себя", "Друзей", "Игроков");
   private final RadioSetting colorMode = new RadioSetting("Режим цвета", "Выбор источника цвета", new String[]{"Клиентский", "Кастом"}, "Клиентский");
   private final ColorSetting shaftColor = new ColorSetting("Цвет тела", "Основной цвет палки")
      .value(new Color(245, 185, 150, 255).getRGB())
      .presets(
         new Color(245, 185, 150, 255).getRGB(),
         new Color(0, 246, 255, 255).getRGB(),
         new Color(183, 1, 195, 255).getRGB(),
         new Color(255, 60, 0, 255).getRGB()
      )
      .visible(() -> this.colorMode.get().equals("Кастом"));
   private final ColorSetting tipColor = new ColorSetting("Цвет головки", "Цвет кончика палки")
      .value(new Color(255, 110, 140, 255).getRGB())
      .presets(
         new Color(255, 110, 140, 255).getRGB(),
         new Color(183, 1, 195, 255).getRGB(),
         new Color(255, 255, 255, 255).getRGB()
      )
      .visible(() -> this.colorMode.get().equals("Кастом"));

   private final StickPhysicsSystem physicsSystem = new StickPhysicsSystem();

   public static ZenisESP getInstance() {
      return Instance.get(ZenisESP.class);
   }

   public ZenisESP() {
      super("ZenisESP", "Zenis ESP", ModuleCategory.RENDER);
      this.setup(new Setting[]{
         this.mode,
         this.posture,
         this.standAngle,
         this.headHeightOffset,
         this.stickLength,
         this.stickThickness,
         this.stiffness,
         this.damping,
         this.gravity,
         this.targets,
         this.colorMode,
         this.shaftColor,
         this.tipColor
      });
   }

   @Override
   public void deactivate() {
      this.physicsSystem.clear();
      super.deactivate();
   }

   @EventHandler
   public void onWorldRender(WorldRenderEvent event) {
      if (mc.world == null || mc.player == null) return;

      float partialTicks = event.getPartialTicks();
      MatrixStack matrices = event.getStack();

      int cShaft = this.colorMode.get().equals("Клиентский") ? ThemeManager.primaryColor.getColor() : this.shaftColor.getColor();
      int cTip = this.colorMode.get().equals("Клиентский") ? ThemeManager.secondaryColor.getColor() : this.tipColor.getColor();

      List<LivingEntity> renderList = new ArrayList<>();

      // Self (Visible in 1st, 2nd, and 3rd person perspectives)
      if (this.targets.isSelected("Себя") && mc.player != null) {
         renderList.add(mc.player);
      }

      // Other entities completely independent of TargetESP
      for (Entity entity : mc.world.getEntities()) {
         if (entity == mc.player || !(entity instanceof LivingEntity living) || !living.isAlive()) continue;

         if (entity instanceof PlayerEntity player) {
            boolean isFriend = FriendUtils.isFriend(player);
            if (isFriend && this.targets.isSelected("Друзей")) {
               renderList.add(player);
            } else if (!isFriend && this.targets.isSelected("Игроков")) {
               renderList.add(player);
            }
         } else if (entity instanceof MobEntity && this.targets.isSelected("Мобов")) {
            renderList.add(living);
         }
      }

      float length = this.stickLength.getValue();
      float thickness = this.stickThickness.getValue();
      String currentMode = this.mode.get();

      for (LivingEntity living : renderList) {
         double posX = MathHelper.lerp(partialTicks, living.prevX, living.getX());
         double posY = MathHelper.lerp(partialTicks, living.prevY, living.getY());
         double posZ = MathHelper.lerp(partialTicks, living.prevZ, living.getZ());
         float height = living.getHeight();
         float bodyYaw = MathHelper.lerpAngleDegrees(partialTicks, living.prevBodyYaw, living.bodyYaw);
         float pitch = MathHelper.lerp(partialTicks, living.prevPitch, living.getPitch());

         if ("Голова".equals(currentMode)) {
            // Mode "Голова": Placed right on top of the head, pointing straight up vertically
            float headY = (float)(posY + height + (living.isInSneakingPose() ? -0.15F : 0.02F) + this.headHeightOffset.getValue());
            Vec3d rootPos = new Vec3d(posX, headY, posZ);
            StickPhysicsSystem.EntityStickState state = this.physicsSystem.getOrCreateState(living.getId(), rootPos);

            float segLength = length / (StickPhysicsSystem.NODE_COUNT - 1);
            Vec3d dir = new Vec3d(0, 1, 0); // straight up

            for (int i = 0; i < StickPhysicsSystem.NODE_COUNT; i++) {
               Vec3d nodePos = rootPos.add(dir.multiply(i * segLength));
               StickSegment seg = state.nodes.get(i);
               seg.pos = nodePos;
               seg.prevPos = nodePos;
               seg.renderPos = nodePos;
               seg.prevRenderPos = nodePos;
            }

            StickRenderer.renderStick(matrices, state.nodes, partialTicks, thickness, cShaft, cTip);
         } else if ("Стояк".equals(currentMode)) {
            // Rigid Static Mode ("Стояк"): Directly lock nodes to player body yaw and angle in front of crotch
            float sneakOffset = living.isInSneakingPose() ? 0.18F : 0.0F;
            float crotchY = (float)(posY + height * 0.52F - sneakOffset);

            double yawRad = Math.toRadians(bodyYaw);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double forwardOffset = living.getWidth() * 0.30;
            double rootX = posX + forwardX * forwardOffset;
            double rootZ = posZ + forwardZ * forwardOffset;
            Vec3d rootPos = new Vec3d(rootX, crotchY, rootZ);

            StickPhysicsSystem.EntityStickState state = this.physicsSystem.getOrCreateState(living.getId(), rootPos);
            float segLength = length / (StickPhysicsSystem.NODE_COUNT - 1);
            double pitchRad = Math.toRadians(this.standAngle.getValue());
            double dirX = -Math.sin(yawRad) * Math.cos(pitchRad);
            double dirY = Math.sin(pitchRad);
            double dirZ = Math.cos(yawRad) * Math.cos(pitchRad);
            Vec3d dir = new Vec3d(dirX, dirY, dirZ).normalize();

            for (int i = 0; i < StickPhysicsSystem.NODE_COUNT; i++) {
               Vec3d nodePos = rootPos.add(dir.multiply(i * segLength));
               StickSegment seg = state.nodes.get(i);
               seg.pos = nodePos;
               seg.prevPos = nodePos;
               seg.renderPos = nodePos;
               seg.prevRenderPos = nodePos;
            }

            StickRenderer.renderStick(matrices, state.nodes, partialTicks, thickness, cShaft, cTip);
         } else {
            // Physics Mode: Verlet simulation, collision, spring return and sway
            float sneakOffset = living.isInSneakingPose() ? 0.18F : 0.0F;
            float crotchY = (float)(posY + height * 0.52F - sneakOffset);

            double yawRad = Math.toRadians(bodyYaw);
            double forwardX = -Math.sin(yawRad);
            double forwardZ = Math.cos(yawRad);
            double forwardOffset = living.getWidth() * 0.30;
            double rootX = posX + forwardX * forwardOffset;
            double rootZ = posZ + forwardZ * forwardOffset;
            Vec3d rootPos = new Vec3d(rootX, crotchY, rootZ);

            StickPhysicsSystem.EntityStickState state = this.physicsSystem.getOrCreateState(living.getId(), rootPos);
            float stiff = this.stiffness.getValue();
            float damp = this.damping.getValue();
            float grav = this.gravity.getValue();
            String post = this.posture.get();
            this.physicsSystem.updatePhysics(living, rootPos, bodyYaw, pitch, post, length, stiff, damp, grav);

            StickRenderer.renderStick(matrices, state.nodes, partialTicks, thickness, cShaft, cTip);
         }
      }
   }
}
