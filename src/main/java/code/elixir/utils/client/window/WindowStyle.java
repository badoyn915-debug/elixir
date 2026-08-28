package code.elixir.utils.client.window;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import org.lwjgl.glfw.GLFWNativeWin32;

public class WindowStyle {
   public static void setDarkMode(long windowHandle) {
      String os = System.getProperty("os.name").toLowerCase();
      if (!os.contains("linux")) {
         long hwnd = GLFWNativeWin32.glfwGetWin32Window(windowHandle);
         HWND hwndJna = new HWND(new Pointer(hwnd));
         int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;
         Memory darkModeEnabled = new Memory(4L);
         darkModeEnabled.setInt(0L, 1);
         WindowStyle.DwmApi.INSTANCE.DwmSetWindowAttribute(hwndJna, DWMWA_USE_IMMERSIVE_DARK_MODE, darkModeEnabled, 4);
      }
   }

   public interface DwmApi extends StdCallLibrary {
      WindowStyle.DwmApi INSTANCE = (WindowStyle.DwmApi)Native.loadLibrary("dwmapi", WindowStyle.DwmApi.class);

      int DwmSetWindowAttribute(HWND var1, int var2, Pointer var3, int var4);
   }
}
