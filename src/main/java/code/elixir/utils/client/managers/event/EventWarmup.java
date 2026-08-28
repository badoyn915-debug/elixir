package code.elixir.utils.client.managers.event;

import code.elixir.events.block.BlockBreakingEvent;
import code.elixir.events.block.BlockCollisionEvent;
import code.elixir.events.block.BlockEntityProgressEvent;
import code.elixir.events.block.BlockUpdateEvent;
import code.elixir.events.block.BreakBlockEvent;
import code.elixir.events.block.PushEvent;
import code.elixir.events.chat.ChatEvent;
import code.elixir.events.chat.TabCompleteEvent;
import code.elixir.events.container.CloseScreenEvent;
import code.elixir.events.container.HandledScreenEvent;
import code.elixir.events.container.SetScreenEvent;
import code.elixir.events.item.ClickSlotEvent;
import code.elixir.events.item.HandAnimationEvent;
import code.elixir.events.item.HandOffsetEvent;
import code.elixir.events.item.SwingDurationEvent;
import code.elixir.events.item.UsingItemEvent;
import code.elixir.events.keyboard.HotBarScrollEvent;
import code.elixir.events.keyboard.KeyEvent;
import code.elixir.events.keyboard.MouseRotationEvent;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.AttackEvent;
import code.elixir.events.player.BoundingBoxControlEvent;
import code.elixir.events.player.DeathScreenEvent;
import code.elixir.events.player.EntityDeathEvent;
import code.elixir.events.player.EntitySpawnEvent;
import code.elixir.events.player.EventNoSlow;
import code.elixir.events.player.FireworkEvent;
import code.elixir.events.player.GameUpdateEvent;
import code.elixir.events.player.HeldItemRendererEvent;
import code.elixir.events.player.HotBarUpdateEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.JumpEvent;
import code.elixir.events.player.MotionEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.player.PlayerCollisionEvent;
import code.elixir.events.player.PlayerTravelEvent;
import code.elixir.events.player.PlayerVelocityStrafeEvent;
import code.elixir.events.player.PostMotionEvent;
import code.elixir.events.player.PostTickEvent;
import code.elixir.events.player.RotatedMovementInputEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.events.player.SprintEvent;
import code.elixir.events.player.SwimmingEvent;
import code.elixir.events.player.TickEvent;
import code.elixir.events.render.AspectRatioEvent;
import code.elixir.events.render.CameraEvent;
import code.elixir.events.render.CameraPositionEvent;
import code.elixir.events.render.DrawEvent;
import code.elixir.events.render.EntityColorEvent;
import code.elixir.events.render.FogEvent;
import code.elixir.events.render.FovEvent;
import code.elixir.events.render.ItemRendererEvent;
import code.elixir.events.render.TextFactoryEvent;
import code.elixir.events.render.WorldLoadEvent;
import code.elixir.events.render.WorldRenderEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventWarmup {
   private static final Logger LOGGER = LogManager.getLogger("Elixir");
   private static final Class<?>[] EVENT_TYPES = new Class[]{
      AttackEvent.class,
      AspectRatioEvent.class,
      BlockBreakingEvent.class,
      BlockCollisionEvent.class,
      BlockEntityProgressEvent.class,
      BlockUpdateEvent.class,
      BreakBlockEvent.class,
      BoundingBoxControlEvent.class,
      CameraEvent.class,
      CameraPositionEvent.class,
      ChatEvent.class,
      ClickSlotEvent.class,
      CloseScreenEvent.class,
      DeathScreenEvent.class,
      DrawEvent.class,
      EntityColorEvent.class,
      EntityDeathEvent.class,
      EntitySpawnEvent.class,
      EventNoSlow.class,
      FireworkEvent.class,
      FogEvent.class,
      FovEvent.class,
      GameUpdateEvent.class,
      HandAnimationEvent.class,
      HandOffsetEvent.class,
      HandledScreenEvent.class,
      HeldItemRendererEvent.class,
      HotBarScrollEvent.class,
      HotBarUpdateEvent.class,
      InputEvent.class,
      ItemRendererEvent.class,
      JumpEvent.class,
      KeyEvent.class,
      MotionEvent.class,
      MouseRotationEvent.class,
      MoveEvent.class,
      PacketEvent.class,
      PlayerCollisionEvent.class,
      PlayerTravelEvent.class,
      PlayerVelocityStrafeEvent.class,
      PostMotionEvent.class,
      PostTickEvent.class,
      PushEvent.class,
      RotatedMovementInputEvent.class,
      RotationUpdateEvent.class,
      SetScreenEvent.class,
      SprintEvent.class,
      SwimmingEvent.class,
      SwingDurationEvent.class,
      TabCompleteEvent.class,
      TextFactoryEvent.class,
      TickEvent.class,
      UsingItemEvent.class,
      WorldLoadEvent.class,
      WorldRenderEvent.class
   };

   private EventWarmup() {
   }

   public static void warmup() {
      for (Class<?> type : EVENT_TYPES) {
         touch(type);
      }

      LOGGER.debug("Preloaded {} event types", EVENT_TYPES.length);
   }

   private static void touch(Class<?> type) {
      try {
         Class.forName(type.getName(), true, type.getClassLoader());
      } catch (ClassNotFoundException var2) {
         LOGGER.error("Missing event class: {}", type.getName(), var2);
      } catch (LinkageError var3) {
         LOGGER.error("Failed to initialize event class: {}", type.getName(), var3);
      }
   }
}
