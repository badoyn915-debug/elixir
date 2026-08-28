package code.elixir.features.impl.movement.autoMystGlider.receivers;

import code.elixir.display.hud.DynamicIsland;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.features.impl.movement.autoMystGlider.EventStatusTracker;
import code.elixir.features.impl.movement.autoMystGlider.controllers.AutoGliderController;
import code.elixir.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.elixir.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.features.module.setting.Setting;
import code.elixir.features.module.setting.implement.BindSetting;
import code.elixir.features.module.setting.implement.TextSetting;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.client.managers.event.EventHandler;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.util.math.Vec3d;

public class AutoMystGliderModule extends Module {
   private AutoGliderController autoGliderController;
   private final BindSetting bindSetting = new BindSetting("Point", "").setKey(86);
   private final TextSetting anarchyText = new TextSetting("Anarchy", "").setText("502").setMax(3).setMin(3);
   private final TextSetting homeName = new TextSetting("Home", "").setText("hata");

   public AutoMystGliderModule() {
      super("AutoMystGlider", ModuleCategory.MOVEMENT);
      this.setup(new Setting[]{this.bindSetting, this.anarchyText, this.homeName});
   }

   @EventHandler
   public void inputEventReceive(InputEvent inputEvent) {
      if (mc.player != null && this.autoGliderController != null) {
         this.autoGliderController.onInput(inputEvent);
      }
   }

   @EventHandler
   public void tickEventReceive(TickEvent tickEvent) {
      if (mc.player != null && this.autoGliderController != null) {
         this.autoGliderController.onTick();
      }
   }

   @EventHandler
   public void moveEventReceive(MoveEvent moveEvent) {
      if (mc.player != null && this.autoGliderController != null) {
         this.autoGliderController.onMove(moveEvent);
      }
   }

   @EventHandler
   public void packetEventReceive(PacketEvent packetEvent) {
      if (packetEvent.getPacket() instanceof GameMessageS2CPacket p) {
         String s = p.content().getString();
         Object cleanedMessage = s.replaceAll("§.", "");
         boolean isEventStatusMessage = ((String)cleanedMessage).toLowerCase().contains("ивент")
            && (((String)cleanedMessage).toLowerCase().contains("через") || ((String)cleanedMessage).toLowerCase().contains("уже начался"));
         EventStatusTracker.updateEventStatus((String)cleanedMessage);
         String eventStatus = EventStatusTracker.getDisplayStatus();
         if (eventStatus != null && !eventStatus.isEmpty() && isEventStatusMessage) {
            DynamicIsland.addNotification(eventStatus, DynamicIsland.NotificationType.INFO);
            packetEvent.cancel();
            return;
         }

         Pattern pattern = Pattern.compile("(-?\\d+)\\s+(-?\\d+)\\s+(-?\\d+)");

         for (String line : ((String)cleanedMessage).split("\n")) {
            Matcher matcher;
            if ((line.toLowerCase().contains("появился на координатах") || line.toLowerCase().contains("координаты:"))
               && (matcher = pattern.matcher(line)).find()) {
               int x = Integer.parseInt(matcher.group(1));
               int y = Integer.parseInt(matcher.group(2));
               int z = Integer.parseInt(matcher.group(3));
               this.mystLocation(new Vec3d(x, y, z));
            }
         }
      }

      if (packetEvent.getType() == PacketEvent.Type.SEND) {
         if (packetEvent.getPacket() instanceof CommandExecutionC2SPacket commandPacket) {
            String command = commandPacket.command();
            if (command != null && command.startsWith("an") && command.length() > 2 && mc.getNetworkHandler() != null) {
               mc.getNetworkHandler().sendCommand("event delay");
            }
         } else {
            Object command = packetEvent.getPacket();
            ChatMessageC2SPacket chatPacket;
            String message;
            if (command instanceof ChatMessageC2SPacket
               && (message = (chatPacket = (ChatMessageC2SPacket)command).chatMessage()) != null
               && message.startsWith("/an")
               && message.length() > 3
               && mc.getNetworkHandler() != null) {
               mc.getNetworkHandler().sendCommand("event delay");
            }
         }
      }

      if (mc.player != null && this.autoGliderController != null) {
         this.autoGliderController.onPacket(packetEvent);
      }
   }

   @EventHandler
   public void rotationUpdateReceive(RotationUpdateEvent rotationUpdateEvent) {
      if (mc.player != null && this.autoGliderController != null) {
         this.autoGliderController.onRotationUpdate(rotationUpdateEvent);
      }
   }

   @EventHandler
   public void keyEventReceive(KeyEvent keyEvent) {
      if (keyEvent.key() == this.bindSetting.getKey() && keyEvent.action() == 1) {
         this.mystLocation(mc.player.getPos());
      }
   }

   private void mystLocation(Vec3d vec3d) {
      if (this.autoGliderController != null) {
         GlidePhase phase = this.autoGliderController.getConnectionHandler().getConnection().getPhase();
         if (phase == GlidePhase.WAITING_COORDS) {
            ChatMessage.brandmessage("Координаты получены! Летим на " + vec3d);
         }

         this.autoGliderController.reload();
      }

      this.autoGliderController = new AutoGliderController(vec3d, this);
      this.logDirect("Place marker on coordinate: " + vec3d.toString());
   }

   @Override
   public void deactivate() {
      if (this.autoGliderController != null) {
         this.autoGliderController.reload();
      }

      super.deactivate();
   }

   @Override
   public void activate() {
      if (this.autoGliderController != null) {
         this.autoGliderController.reload();
      }

      this.autoGliderController = new AutoGliderController(new Vec3d(0.0, 0.0, 0.0), this);
      ConnectionState conn = this.autoGliderController.getConnectionHandler().getConnection();
      conn.setConnected(false);
      conn.transitionTo(GlidePhase.HUB_TO_NEXT);
      if (mc.getNetworkHandler() != null) {
         mc.getNetworkHandler().sendCommand("hub");
      }

      ChatMessage.brandmessage("[AutoMyst] Модуль включён. Фаза: HUB_TO_NEXT — ищем ивент из API...");
      super.activate();
   }

   public AutoGliderController getAutoGliderController() {
      return this.autoGliderController;
   }

   public BindSetting getBindSetting() {
      return this.bindSetting;
   }

   public TextSetting getAnarchyText() {
      return this.anarchyText;
   }

   public TextSetting getHomeName() {
      return this.homeName;
   }
}
