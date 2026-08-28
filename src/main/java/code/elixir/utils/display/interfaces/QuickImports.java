package code.elixir.utils.display.interfaces;

import code.elixir.display.screens.clickgui.components.implement.window.WindowManager;
import code.elixir.utils.display.render.draw.DrawEngine;
import code.elixir.utils.display.render.draw.DrawEngineImpl;
import code.elixir.utils.display.render.shape.implement.Arc;
import code.elixir.utils.display.render.shape.implement.Blur;
import code.elixir.utils.display.render.shape.implement.Image;
import code.elixir.utils.display.render.shape.implement.Rectangle;
import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;

public interface QuickImports extends QuickLogger {
   MinecraftClient mc = MinecraftClient.getInstance();
   RenderTickCounter tickCounter = mc.getRenderTickCounter();
   LazyWindow window = new LazyWindow();
   DrawEngine drawEngine = new DrawEngineImpl();
   Rectangle rectangle = new Rectangle();
   Blur blur = new Blur();
   Arc arc = new Arc();
   Image image = new Image();
   Gson gson = new Gson();
   WindowManager windowManager = new WindowManager();
}
