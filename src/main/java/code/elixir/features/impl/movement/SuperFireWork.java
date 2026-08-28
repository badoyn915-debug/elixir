package code.elixir.features.impl.movement;

import code.elixir.events.player.FireworkEvent;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BooleanSetting;
import code.elixir.features.module.setting.implement.SelectSetting;
import code.elixir.features.module.setting.implement.SliderSettings;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class SuperFireWork extends Module {
   private static final float BUCKET_DEGREES = 5.0F;
   private static final int BUCKET_COUNT = 9;
   private final BooleanSetting acceleration = new BooleanSetting("Ускорение", "Включить кастомный буст фейерверка").setValue(true);
   private final SelectSetting modeSetting = new SelectSetting("Режим ускорения", "Тип расчёта скорости")
      .value("ReallyWorld", "Bravo", "BravoGrief", "Кастомный")
      .visible(this.acceleration::isValue);
   private final SliderSettings customSpeed = new SliderSettings("Скорость", "Скорость в кастомном режиме")
      .range(1.0F, 50.0F)
      .setValue(20.0F)
      .visible(() -> this.acceleration.isValue() && this.modeSetting.isSelected("Кастомный"));
   private final SliderSettings[] xzBuckets = this.createBuckets("XZ", 1.52F);
   private final SliderSettings[] yBuckets = this.createBuckets("Y", 1.51F);
   private static final float[] BRAVO_GRIEF_XZ = new float[]{1.65F, 1.66F, 1.67F, 1.68F, 1.69F, 1.7F, 1.71F, 1.72F, 1.73F};
   private static final float[] BRAVO_GRIEF_Y = new float[]{1.6F, 1.61F, 1.62F, 1.63F, 1.64F, 1.65F, 1.66F, 1.67F, 1.68F};

   public SuperFireWork() {
      super("SuperFireWork", "SuperFirework", ModuleCategory.MOVEMENT);
      Setting[] all = new Setting[this.xzBuckets.length + this.yBuckets.length + 3];
      int i = 0;
      all[i++] = this.acceleration;
      all[i++] = this.modeSetting;
      all[i++] = this.customSpeed;
      System.arraycopy(this.xzBuckets, 0, all, i, this.xzBuckets.length);
      int var6;
      System.arraycopy(this.yBuckets, 0, all, var6 = i + this.xzBuckets.length, this.yBuckets.length);
      this.setup(all);
   }

   private SliderSettings[] createBuckets(String axis, float defaultValue) {
      SliderSettings[] buckets = new SliderSettings[9];

      for (int i = 0; i < 9; i++) {
         int from = i * 5;
         int to = from + 5;
         buckets[i] = new SliderSettings(axis + " " + from + "-" + to + "°", "Множитель скорости")
            .setValue(defaultValue + i * 0.01F)
            .range(1.0F, 3.0F)
            .step(0.01F)
            .visible(() -> this.acceleration.isValue() && this.modeSetting.isSelected("ReallyWorld"));
      }

      return buckets;
   }

   @EventHandler
   public void onFirework(FireworkEvent event) {
      if (this.acceleration.isValue() && mc.player != null) {
         float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
         float pitch = mc.player.getPitch();
         String var6 = this.modeSetting.getSelected();
         float y;
         float xz;
         switch (var6) {
            case "ReallyWorld":
               xz = this.bucketValue(this.xzBuckets, this.foldAngle(yaw));
               y = this.bucketValue(this.yBuckets, this.foldAngle(pitch));
               if (y > xz) {
                  xz = y;
               }
               break;
            case "Bravo":
               xz = this.bravoSpeed(pitch, yaw);
               y = this.bravoPitchSpeed(pitch);
               break;
            case "BravoGrief":
               xz = this.lookup(BRAVO_GRIEF_XZ, this.foldAngle(yaw));
               y = this.lookup(BRAVO_GRIEF_Y, this.foldAngle(pitch));
               break;
            default:
               int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0.0F ? 45 : -45;
               double accel = Math.abs((TurnsConnection.INSTANCE.getRotation().getYaw() + ff) % 90.0F - ff) / 45.0F;
               double rotationBoost = 1.0 + 0.3 * accel * accel;
               boolean yAccel = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60.0F;
               Vec3d direction = TurnsConnection.INSTANCE.getMoveRotation().toVector();
               float speed = this.customSpeed.getValue() / 20.0F;
               double finalSpeed = speed * rotationBoost;
               event.setVector(new Vec3d(direction.x * finalSpeed, yAccel ? direction.y * finalSpeed : direction.y * speed, direction.z * finalSpeed));
               return;
         }

         event.setVector(new Vec3d(xz, y, xz));
      }
   }

   private float bucketValue(SliderSettings[] buckets, float angle) {
      int index = MathHelper.clamp((int)(angle / 5.0F), 0, buckets.length - 1);
      return buckets[index].getValue();
   }

   private float lookup(float[] table, float angle) {
      int index = MathHelper.clamp((int)(angle / 5.0F), 0, table.length - 1);
      return table[index];
   }

   private float foldAngle(float angle) {
      float value = Math.abs(angle);
      if (value > 90.0F) {
         value = 180.0F - value;
      }

      if (value > 45.0F) {
         value = 90.0F - value;
      }

      return value;
   }

   private float bravoPitchSpeed(float pitch) {
      float abs = Math.abs(pitch);
      if (abs <= 5.0F) {
         return 1.51F;
      } else if (abs <= 10.0F) {
         return 2.0F;
      } else if (abs <= 15.0F) {
         return 1.55F;
      } else if (abs <= 20.0F) {
         return 1.56F;
      } else if (abs <= 25.0F) {
         return 1.57F;
      } else if (abs <= 30.0F) {
         return 1.58F;
      } else if (abs <= 35.0F) {
         return 1.59F;
      } else {
         return abs <= 40.0F ? 1.6F : 1.61F;
      }
   }

   private float bravoSpeed(float pitch, float yaw) {
      float absPitch = Math.abs(pitch);
      float absYaw = Math.abs(MathHelper.wrapDegrees(yaw) % 90.0F);
      float speed = absPitch >= 5.0F && absPitch <= 10.0F
         ? 2.0F
         : (
            absPitch >= 10.0F && absPitch <= 15.0F
               ? 1.54F
               : (
                  absPitch >= 15.0F && absPitch <= 20.0F
                     ? 1.55F
                     : (
                        (!(absYaw >= 30.0F) || !(absYaw <= 35.0F)) && (!(absPitch >= 20.0F) || !(absPitch <= 25.0F))
                           ? (
                              (!(absYaw >= 35.0F) || !(absYaw <= 40.0F)) && (!(absPitch >= 25.0F) || !(absPitch <= 30.0F))
                                 ? (
                                    (!(absYaw >= 40.0F) || !(absYaw <= 45.0F)) && (!(absPitch >= 30.0F) || !(absPitch <= 35.0F))
                                       ? (
                                          absPitch >= 35.0F && absPitch <= 40.0F
                                             ? 1.59F
                                             : (absPitch >= 40.0F && absPitch <= 45.0F ? 1.6F : (!(absYaw <= 5.0F) && !(absPitch <= 5.0F) ? 1.62F : 1.53F))
                                       )
                                       : 1.58F
                                 )
                                 : 1.57F
                           )
                           : 1.56F
                     )
               )
         );
      return pitch > 0.0F ? speed : speed;
   }
}
