package code.elixir.features.impl.movement;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.EventOnMovePost;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.PlayerTravelEvent;
import code.elixir.events.player.PostMotionEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.combat.AttackAura;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.timer.TimerManager;
import code.elixir.utils.input.MoveUtil;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.network.NetworkUtils;
import java.util.Iterator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Speed extends Module {
   private SelectSetting mode = new SelectSetting("Режим", "Выберите режим скорости")
      .value("Обычный", "Грим", "ХолиВорлд", "ФанТайм", "RWNew")
      .selected("Грим");
   private SliderSettings speed = new SliderSettings("Скорость", "Настройка скорости передвижения")
      .range(1.0F, 5.0F)
      .setValue(1.5F)
      .visible(() -> this.mode.getSelected().equals("Обычный"));
   private SliderSettings grimBaseBoost = new SliderSettings("Буст", "Базовый буст за одну коллизию")
      .range(0.01F, 0.25F)
      .setValue(0.08F)
      .step(0.005F)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private SliderSettings grimCollisionRange = new SliderSettings("Радиус коллизий", "Радиус поиска сущностей для Grim")
      .range(0.1F, 2.5F)
      .setValue(0.4F)
      .step(0.01F)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private SliderSettings grimMaxCollisions = new SliderSettings("Лимит коллизий", "Максимум сущностей для учёта буста")
      .range(1.0F, 10.0F)
      .setValue(5.0F)
      .step(1.0F)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private BooleanSetting grimOnlyGround = new BooleanSetting("Только на земле", "Работать Grim speed только когда игрок на земле")
      .setValue(true)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private BooleanSetting grimIncludeBoats = new BooleanSetting("Учитывать лодки", "Считать лодки как коллизии для Grim")
      .setValue(true)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private BooleanSetting up = new BooleanSetting("Усиление", "Увеличивает дистанцию ускорения до цели в Aura")
      .setValue(true)
      .visible(() -> this.mode.getSelected().equals("Грим"));
   private SliderSettings strength = new SliderSettings("Сила", "Фактор умножения до цели")
      .range(1.0F, 6.0F)
      .setValue(1.5F)
      .visible(() -> this.mode.getSelected().equals("Грим") && this.up.isValue());
   private final SliderSettings bypassDistance = new SliderSettings("Дистанция", "Дистанция обнаружения коллизий")
      .range(0.1F, 2.0F)
      .setValue(0.3F)
      .step(0.01F)
      .visible(() -> this.mode.getSelected().equals("ХолиВорлд"));
   private final SliderSettings bypassSpeed = new SliderSettings("Скорость", "Скорость байпаса")
      .range(0.1F, 20.0F)
      .setValue(3.1F)
      .step(0.1F)
      .visible(() -> this.mode.getSelected().equals("ХолиВорлд"));
   private Vec3d currentVelocity = Vec3d.ZERO;
   private int grimTargetCollisions = 0;
   private long lastCollisionCheck = 0L;
   private int ticks = 0;
   private int groundTicks = 0;

   public Speed() {
      super("Speed", "Speed", ModuleCategory.MOVEMENT);
      this.setup(
         new Setting[]{
            this.mode,
            this.speed,
            this.grimBaseBoost,
            this.grimCollisionRange,
            this.grimMaxCollisions,
            this.grimOnlyGround,
            this.grimIncludeBoats,
            this.up,
            this.strength,
            this.bypassDistance,
            this.bypassSpeed
         }
      );
   }

   @EventHandler
   public void onTick(TickEvent e) {
      String modeVal = this.mode.getSelected();
      if (modeVal.equals("Обычный")) {
         Simulations.setVelocity(this.speed.getValue() / 3.0F);
      }
   }

   @EventHandler
   public void onMotion(PlayerTravelEvent e) {
      String modeVal = this.mode.getSelected();
      if (modeVal.equals("ФанТайм")
         && !mc.player.isSwimming()
         && !mc.player.isGliding()
         && !mc.player.isSneaking()
         && mc.player.getBoundingBox().maxY - mc.player.getBoundingBox().minY < 1.5) {
         float motion = mc.player.hasStatusEffect(StatusEffects.SPEED) ? 0.32F : 0.28F;
         Simulations.setVelocity(motion);
      }

      if (modeVal.equals("Грим") && e.isPre() && Simulations.hasPlayerMovement()) {
         this.handleGrimSpeed();
      }

      if (modeVal.equals("ХолиВорлд") && e.isPre()) {
         if (System.currentTimeMillis() - this.lastCollisionCheck > 100L) {
            this.updateGrimTargetCollisions();
            this.lastCollisionCheck = System.currentTimeMillis();
         }

         if (this.grimTargetCollisions > 0) {
            this.applyGrimTargetBypass();
         }
      }
   }

   @EventHandler
   public void onMovePost(EventOnMovePost event) {
      if (this.mode.getSelected().equals("RWNew")) {
         TimerManager.setTimer(1.7F);
         if (this.ticks > 3) {
            double bst = 0.03;
            if (this.ticks % 2 == 0) {
               mc.player.addVelocity(0.0, 0.03F, 0.0);
               bst = mc.player.isOnGround() ? 0.085 : 0.03;
            }

            double yaw = Math.toRadians(MoveUtil.getdir());
            double xt = -Math.sin(yaw);
            double zt = Math.cos(yaw);
            if (MoveUtil.getdir() == -1.0F) {
               xt = 0.0;
               zt = 0.0;
            }

            mc.player.addVelocity(xt * bst, 0.0, zt * bst);
         }

         this.ticks++;
      }
   }

   @EventHandler
   public void onInput(InputEvent event) {
      if (this.mode.getSelected().equals("RWNew")) {
         this.groundTicks = mc.player.verticalCollision ? ++this.groundTicks : 0;
         if (this.groundTicks >= 1) {
            mc.player.jump();
         }
      }
   }

   @EventHandler
   public void onPostMotion(PostMotionEvent event) {
      if (this.mode.getSelected().equals("RWNew")) {
         if (this.ticks % 2 == 0) {
            TimerManager.setTimer(0.3F);
            NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket(mc.player, Mode.START_FALL_FLYING));
         }
      }
   }

   @EventHandler
   public void onPacket(PacketEvent event) {
      if (this.mode.getSelected().equals("RWNew")) {
         if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (this.ticks % 2 == 1) {
               this.ticks++;
            }

            TimerManager.setTimer(1.0F);
         }
      }
   }

   private void handleGrimSpeed() {
      if (mc.player != null && mc.world != null) {
         if (!this.grimOnlyGround.isValue() || mc.player.isOnGround()) {
            int collisions = 0;
            float box = this.grimCollisionRange.getValue();
            if (AttackAura.getInstance().isState()
               && AttackAura.getInstance().getTarget() != null
               && AttackAura.getInstance().getTarget().isSprinting()
               && mc.player.isSprinting()
               && this.up.isValue()) {
               box = Math.max(box, this.strength.getValue());
            }

            Box expandedBox = mc.player.getBoundingBox().expand(box);
            int maxCollisions = (int)this.grimMaxCollisions.getValue();
            Iterator iterator = mc.world.getEntities().iterator();

            while (iterator.hasNext()) {
               Entity ent;
               if ((ent = (Entity)iterator.next()) != mc.player
                  && !(ent instanceof ArmorStandEntity)
                  && (!(ent instanceof BoatEntity) || this.grimIncludeBoats.isValue())
                  && (ent instanceof LivingEntity || ent instanceof BoatEntity)
                  && expandedBox.intersects(ent.getBoundingBox())
                  && ++collisions >= maxCollisions) {
                  break;
               }
            }

            double[] motion = Simulations.forward(this.grimBaseBoost.getValue() * collisions);
            mc.player.addVelocity(motion[0], 0.0, motion[1]);
         }
      }
   }

   private void updateGrimTargetCollisions() {
      this.grimTargetCollisions = 0;
      if (mc.player != null && mc.world != null) {
         float expandValue = this.bypassDistance.getValue();
         Box expandedBox = mc.player.getBoundingBox().expand(expandValue);

         for (Entity ent : mc.world.getEntities()) {
            if (ent != mc.player
               && !(ent instanceof ArmorStandEntity)
               && (ent instanceof LivingEntity || ent instanceof BoatEntity)
               && expandedBox.intersects(ent.getBoundingBox())) {
               this.grimTargetCollisions++;
            }
         }
      }
   }

   private void applyGrimTargetBypass() {
      if (this.grimTargetCollisions > 0) {
         LivingEntity target = AttackAura.getInstance().getTarget();
         if (target != null && target.isAlive()) {
            double var10000 = mc.player.getPos().distanceTo(target.getPos());
            double maxRange = 3.0;
            if (var10000 <= 3.0) {
               this.applySpeedBoost(target);
               return;
            }
         }

         double maxRange2 = 3.0;
         double maxRangeSq = maxRange2 * maxRange2;
         Entity nearest = null;
         double bestSq = Double.MAX_VALUE;

         for (Entity ent : mc.world.getEntities()) {
            double dz;
            double dx;
            double sq;
            if (ent != mc.player
               && !(ent instanceof ArmorStandEntity)
               && (ent instanceof LivingEntity || ent instanceof BoatEntity)
               && (sq = (dx = ent.getX() - mc.player.getX()) * dx + (dz = ent.getZ() - mc.player.getZ()) * dz) <= maxRangeSq
               && sq < bestSq) {
               bestSq = sq;
               nearest = ent;
            }
         }

         if (nearest != null) {
            this.applySpeedBoost(nearest);
         }
      }
   }

   private void applySpeedBoost(Entity targetEntity) {
      double finalSpeed = this.bypassSpeed.getValue() * 0.01 * this.grimTargetCollisions;
      if (!(finalSpeed <= 0.0)) {
         double[] direction = this.getDirectionToPoint(mc.player.getPos(), targetEntity.getPos(), finalSpeed);
         mc.player.addVelocity(direction[0], 0.0, direction[1]);
         Vec3d currentVel = mc.player.getVelocity();
         this.currentVelocity = new Vec3d(currentVel.x, currentVel.y, currentVel.z);
      }
   }

   private double[] getDirectionToPoint(Vec3d from, Vec3d to, double speed) {
      double dx = to.x - from.x;
      double dz = to.z - from.z;
      double length = Math.sqrt(dx * dx + dz * dz);
      return length == 0.0 ? new double[]{0.0, 0.0} : new double[]{dx / length * speed, dz / length * speed};
   }

   @Override
   public void activate() {
      super.activate();
      this.grimTargetCollisions = 0;
      this.lastCollisionCheck = System.currentTimeMillis();
      this.ticks = 0;
      this.groundTicks = 0;
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.grimTargetCollisions = 0;
      this.ticks = 0;
      this.groundTicks = 0;
      TimerManager.reset();
   }
}
