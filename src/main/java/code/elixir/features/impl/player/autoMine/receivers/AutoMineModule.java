package code.elixir.features.impl.player.autoMine.receivers;

import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import code.elixir.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.elixir.features.impl.player.autoMine.controllers.AutoMineController;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.managers.event.EventHandler;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.text.Text;

public class AutoMineModule extends Module {
   private final AutoMineController controller = new AutoMineController();
   private final TextSetting anarchyNumber = new TextSetting("Anarchy", "").setText("Anarchy").setMin(3).setMax(4);
   private final TextSetting homeName = new TextSetting("Home", "").setText("Home").setMax(16).setMin(0);

   public AutoMineModule() {
      super("AutoMine", ModuleCategory.PLAYER);
      this.setup(new Setting[]{this.anarchyNumber, this.homeName});
   }

   @EventHandler
   public void tickEventReceive(TickEvent tickEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         this.controller.onTick();
      }
   }

   @EventHandler
   public void rotationUpdateEventReceive(RotationUpdateEvent rotationUpdateEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         this.controller.onRotationUpdate(rotationUpdateEvent);
      }
   }

   @EventHandler
   public void worldChangeEventReceive(WorldLoadEvent worldLoadEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         if (this.controller != null) {
            this.controller.onWorldChange();
         }
      }
   }

   @EventHandler
   public void packetEventReceive(PacketEvent packetEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         if (packetEvent.getType() == PacketEvent.Type.RECEIVE) {
            if (mc.player != null && this.controller != null) {
               if (packetEvent.getPacket() instanceof GameMessageS2CPacket gameMessage) {
                  Text content = gameMessage.content();
                  if (content == null) {
                     return;
                  }

                  String message = content.getString();
                  if (message != null && (message.contains("Вы уже подключены на этот сервер!") || message.contains("Вы уже подключены к этому серверу!"))) {
                     this.controller.notifyAlreadyConnected();
                  }
               }
            }
         }
      }
   }

   @EventHandler
   public void drawEventReceive(DrawEvent drawEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         this.controller.onDraw(drawEvent);
      }
   }

   @EventHandler
   public void inputEventReceive(InputEvent inputEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         this.controller.onInput(inputEvent);
      }
   }

   @EventHandler
   public void worldRenderEventReceive(WorldRenderEvent worldRenderEvent) {
      if (!SpookyParser.isAnarchy(-1)) {
         this.controller.onWorldRender(worldRenderEvent);
      }
   }

   public AutoMineController getController() {
      return this.controller;
   }

   public TextSetting getAnarchyNumber() {
      return this.anarchyNumber;
   }

   public TextSetting getHomeName() {
      return this.homeName;
   }
}
