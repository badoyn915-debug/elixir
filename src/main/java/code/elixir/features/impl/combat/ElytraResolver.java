package code.elixir.features.impl.combat;

import code.elixir.events.player.TickEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.Instance;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.interactions.inv.HotbarUtil;
import code.elixir.utils.interactions.inv.SlotSearchResult;
import java.util.ArrayList;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;

public class ElytraResolver extends Module {
   private static final float MIN_HEIGHT = 4.0F;
   private static final long RETREAT_TIMEOUT_MS = 50L;
   private static final double MIN_DIR_SQ = 1.0E-4;
   private final SliderSettings approachDistance = new SliderSettings("Долет", "Дистанция долёта").setValue(3.0F).range(1.5F, 6.0F).step(0.1F);
   private final SliderSettings retreatDistance = new SliderSettings("Отлет", "Дистанция отлёта").setValue(8.0F).range(4.0F, 16.0F).step(0.5F);
   private final SliderSettings retreatAngle = new SliderSettings("Угол отлета", "Угол отлёта").setValue(50.0F).range(0.0F, 90.0F).step(1.0F);
   private final SelectSetting retreatMode = new SelectSetting("Режим отлета", "Направление отлёта").value("В сторону", "Вверх", "Диагональ");
   private final BooleanSetting alternateSides = new BooleanSetting("Чередовать стороны", "Чередует сторону отлёта").setValue(true);
   private final SliderSettings escapeDistance = new SliderSettings("Дистанция отлета (Aura)", "Дистанция отлёта при ударе")
      .setValue(6.0F)
      .range(4.0F, 8.0F)
      .step(0.1F);
   private final BooleanSetting autoFirework = new BooleanSetting("Авто-фейерверк", "Авто-фейерверк при отлёте Aura").setValue(true);
   private ElytraResolver.ResolveState state = ElytraResolver.ResolveState.APPROACHING;
   private Vec3d retreatDirection;
   private long stateSince;
   private LivingEntity trackedTarget;
   private boolean escaping;
   private Vec3d escapePos;
   private long escapeStartTime;
   private int returnFireworkTicks = -1;
   private Vec3d lastEscapeDirection;
   private boolean alternateFlip;

   public ElytraResolver() {
      super("ElytraResolver", "Elytra Resolver", ModuleCategory.MOVEMENT);
      this.setup(
         new Setting[]{
            this.approachDistance, this.retreatDistance, this.retreatAngle, this.retreatMode, this.alternateSides, this.escapeDistance, this.autoFirework
         }
      );
   }

   public static ElytraResolver getInstance() {
      return Instance.get(ElytraResolver.class);
   }

   public Vec3d resolve(LivingEntity target, Vec3d point) {
      if (!this.isState() || mc.player == null || target == null || point == null) {
         this.resetApproach();
         return point;
      } else if (!mc.player.isGliding()) {
         this.resetApproach();
         return point;
      } else {
         if (target != this.trackedTarget) {
            this.trackedTarget = target;
            this.state = ElytraResolver.ResolveState.APPROACHING;
            this.retreatDirection = null;
            this.stateSince = System.currentTimeMillis();
         }

         double distance = mc.player.getEyePos().distanceTo(point);
         if (this.state == ElytraResolver.ResolveState.APPROACHING && distance <= this.approachDistance.getValue()) {
            this.beginRetreat();
         } else if (this.state == ElytraResolver.ResolveState.RETREATING) {
            boolean farEnough = distance >= this.retreatDistance.getValue();
            boolean timedOut = System.currentTimeMillis() - this.stateSince > 50L;
            if (farEnough || timedOut) {
               this.beginApproach();
            }
         }

         if (this.state == ElytraResolver.ResolveState.APPROACHING) {
            return point;
         } else {
            if (this.retreatDirection == null || this.retreatDirection.lengthSquared() < 1.0E-4) {
               this.retreatDirection = this.buildRetreatDirection(point, mc.player.getPos());
            }

            return point.add(this.retreatDirection.multiply(this.retreatDistance.getValue()));
         }
      }
   }

   public void onAttack(LivingEntity target) {
      if (target != null) {
         if (target != this.trackedTarget) {
            this.trackedTarget = target;
         }

         if (this.state == ElytraResolver.ResolveState.APPROACHING) {
            this.beginRetreat();
         }
      }

      this.onAuraAttack();
   }

   @EventHandler
   public void onUpdate(TickEvent event) {
      if (mc.player != null && mc.world != null) {
         if (this.returnFireworkTicks > 0) {
            this.returnFireworkTicks--;
            if (this.returnFireworkTicks == 0) {
               if (this.autoFirework.isValue()) {
                  this.useFirework();
               }

               this.returnFireworkTicks = -1;
            }
         }

         if (this.escaping && this.escapePos != null) {
            double dist = mc.player.getPos().distanceTo(this.escapePos);
            long elapsed = System.currentTimeMillis() - this.escapeStartTime;
            if (dist < 2.0 || elapsed > 1000L) {
               this.escaping = false;
               this.escapePos = null;
               if (this.autoFirework.isValue()) {
                  this.returnFireworkTicks = 2;
               }
            }
         }
      }
   }

   public void onAuraAttack() {
      if (this.isState()) {
         if (mc.player != null && mc.player.isGliding()) {
            this.escapePos = this.calculateSmartEscape(mc.player.getPos(), this.escapeDistance.getValue());
            if (this.escapePos != null) {
               this.escaping = true;
               this.escapeStartTime = System.currentTimeMillis();
               if (this.autoFirework.isValue()) {
                  this.useFirework();
               }
            }
         }
      }
   }

   public boolean isEscaping() {
      return this.escaping;
   }

   public Vec3d getEscapePos() {
      return this.escapePos;
   }

   private void beginApproach() {
      this.state = ElytraResolver.ResolveState.APPROACHING;
      this.retreatDirection = null;
      this.stateSince = System.currentTimeMillis();
   }

   private void beginRetreat() {
      this.state = ElytraResolver.ResolveState.RETREATING;
      this.retreatDirection = null;
      this.stateSince = System.currentTimeMillis();
   }

   private void resetApproach() {
      this.state = ElytraResolver.ResolveState.APPROACHING;
      this.trackedTarget = null;
      this.retreatDirection = null;
      this.stateSince = 0L;
   }

   private Vec3d buildRetreatDirection(Vec3d aimPoint, Vec3d playerPos) {
      Vec3d away = playerPos.subtract(aimPoint);
      away = new Vec3d(away.x, 0.0, away.z);
      if (away.lengthSquared() < 1.0E-4) {
         Vec3d look = mc.player.getRotationVec(1.0F);
         away = new Vec3d(look.x, 0.0, look.z);
      }

      if (away.lengthSquared() < 1.0E-4) {
         away = new Vec3d(0.0, 0.0, 1.0);
      }

      away = away.normalize();
      double angle = this.retreatAngle.getValue();
      if (this.alternateSides.isValue()) {
         angle = this.alternateFlip ? angle : -angle;
         this.alternateFlip = !this.alternateFlip;
      }

      double rad = Math.toRadians(angle);
      double x = away.x * Math.cos(rad) - away.z * Math.sin(rad);
      double z = away.x * Math.sin(rad) + away.z * Math.cos(rad);
      String var14 = this.retreatMode.getSelected();

      double y = switch (var14) {
         case "Вверх" -> 1.0;
         case "Диагональ" -> 0.5;
         default -> 0.0;
      };
      return new Vec3d(x, y, z).normalize();
   }

   private Vec3d calculateSmartEscape(Vec3d origin, float dist) {
      Vec3d velocity = mc.player.getVelocity();
      Vec3d horizontalVelocity = new Vec3d(velocity.x, 0.0, velocity.z);
      Vec3d back;
      if (horizontalVelocity.lengthSquared() > 0.01) {
         back = horizontalVelocity.normalize().multiply(-1.0);
      } else {
         float yawRad = (float)Math.toRadians(mc.player.getYaw());
         back = new Vec3d(Math.sin(yawRad), 0.0, -Math.cos(yawRad));
      }

      Vec3d up = new Vec3d(0.0, 1.0, 0.0);
      Vec3d left = new Vec3d(-back.z, 0.0, back.x);
      Vec3d right = new Vec3d(back.z, 0.0, -back.x);
      ArrayList<Vec3d> directions = new ArrayList<>();
      directions.add(back);
      directions.add(back.add(up).normalize());
      directions.add(back.add(up.multiply(0.5)).normalize());
      directions.add(back.add(left.multiply(0.5)).normalize());
      directions.add(back.add(right.multiply(0.5)).normalize());
      directions.add(up);
      Vec3d best = null;
      double bestScore = -Double.MAX_VALUE;

      for (Vec3d dir : directions) {
         Vec3d candidate = origin.add(dir.multiply(dist));
         double score;
         if (!(candidate.y - mc.world.getBottomY() < 4.0)
            && mc.world.raycast(new RaycastContext(origin, candidate, ShapeType.COLLIDER, FluidHandling.NONE, mc.player)).getType() == Type.MISS
            && (score = dir.dotProduct(back) * 30.0 + dir.y * 25.0) > bestScore) {
            bestScore = score;
            best = candidate;
         }
      }

      if (best != null) {
         this.lastEscapeDirection = best.subtract(origin).normalize();
      }

      return best;
   }

   private void useFirework() {
      if (mc.player != null && mc.interactionManager != null) {
         Hand hand = HotbarUtil.getHand(Items.FIREWORK_ROCKET);
         if (hand != null) {
            mc.interactionManager.interactItem(mc.player, hand);
            mc.player.swingHand(hand);
         } else {
            SlotSearchResult result = HotbarUtil.findItemInHotBar(Items.FIREWORK_ROCKET);
            if (result.found() && result.isInHotBar()) {
               int previous = mc.player.getInventory().selectedSlot;
               HotbarUtil.switchToSilent(result.slot());
               mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
               mc.player.swingHand(Hand.MAIN_HAND);
               HotbarUtil.switchToSilent(previous);
            }
         }
      }
   }

   @Override
   public void deactivate() {
      super.deactivate();
      this.resetApproach();
      this.escaping = false;
      this.escapePos = null;
      this.returnFireworkTicks = -1;
      this.lastEscapeDirection = null;
   }

   private static enum ResolveState {
      APPROACHING,
      RETREATING;
   }
}
