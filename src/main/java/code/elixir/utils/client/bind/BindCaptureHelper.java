package code.elixir.utils.client.bind;

import code.elixir.features.module.Module;
import code.elixir.features.module.setting.implement.BindSetting;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.InputUtil.Key;

public final class BindCaptureHelper {
   private static Object target;

   public static boolean isCapturing() {
      return target != null;
   }

   public static boolean isCapturing(BindSetting setting) {
      return target == setting;
   }

   public static boolean isCapturing(Module module) {
      return target == module;
   }

   public static void start(BindSetting setting) {
      target = setting;
   }

   public static void start(Module module) {
      target = module;
   }

   public static void cancel() {
      target = null;
   }

   public static void toggle(BindSetting setting) {
      if (target == setting) {
         cancel();
      } else {
         start(setting);
      }
   }

   public static void toggle(Module module) {
      if (target == module) {
         cancel();
      } else {
         start(module);
      }
   }

   public static boolean handleKey(int keyCode, int scanCode) {
      if (target == null) {
         return false;
      } else if (keyCode == 256) {
         applyKey(-1);
         cancel();
         return true;
      } else {
         int resolved = resolveKey(keyCode, scanCode);
         if (BindCodes.canBind(resolved)) {
            applyKey(resolved);
            cancel();
            return true;
         } else {
            return true;
         }
      }
   }

   public static boolean handleMouse(int button) {
      if (target == null) {
         return false;
      } else {
         if (BindCodes.canBind(button)) {
            applyKey(button);
            cancel();
         }

         return true;
      }
   }

   public static boolean handleScroll(double amount) {
      if (target != null && amount != 0.0) {
         applyKey(amount > 0.0 ? 998 : 999);
         cancel();
         return true;
      } else {
         return false;
      }
   }

   private static int resolveKey(int keyCode, int scanCode) {
      if (BindCodes.canBind(keyCode)) {
         return keyCode;
      } else {
         Key key;
         return keyCode == -1 && scanCode > 0 && (key = InputUtil.fromKeyCode(keyCode, scanCode)) != null && BindCodes.canBind(key.getCode())
            ? key.getCode()
            : -1;
      }
   }

   private static void applyKey(int key) {
      if (target instanceof BindSetting setting) {
         setting.setKey(key);
         setting.clearSecondKey();
      } else if (target instanceof Module module) {
         module.setKey(key);
         module.setSecondKey(-1);
      }
   }

   private BindCaptureHelper() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }

   public static Object getTarget() {
      return target;
   }
}
