package code.elixir.features.impl.movement.autoMystGlider.controllers;

import code.elixir.Elixir;
import code.elixir.events.packet.PacketEvent;
import code.elixir.events.player.InputEvent;
import code.elixir.events.player.MoveEvent;
import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.features.impl.misc.ElytraHelper;
import code.elixir.features.impl.movement.autoMystGlider.handers.ConnectionHandler;
import code.elixir.features.impl.movement.autoMystGlider.handers.NoClipHandler;
import code.elixir.features.impl.movement.autoMystGlider.handers.StealHandler;
import code.elixir.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.elixir.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.elixir.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.elixir.features.impl.movement.autoMystGlider.states.GlideState;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec3d;

public class AutoGliderController implements QuickImports {
   private final ConnectionHandler connectionHandler;
   private final StealHandler stealHandler;
   private final NoClipHandler noClipHandler;
   private final GlideState state;

   public AutoGliderController(Vec3d vec3d, AutoMystGliderModule module) {
      this.state = new GlideState(vec3d);
      this.stealHandler = new StealHandler(module);
      this.connectionHandler = new ConnectionHandler(this.state, this.stealHandler, module);
      this.noClipHandler = new NoClipHandler(this.state, this.connectionHandler.getConnection());
   }

   public void onRotationUpdate(RotationUpdateEvent rotationUpdateEvent) {
      GlidePhase phase = this.getPhase();
      if (phase == GlidePhase.LOOTING) {
         this.stealHandler.onRotationUpdate(rotationUpdateEvent);
      }
   }

   public void onInput(InputEvent inputEvent) {
      if (mc.player != null && this.state.vec3d() != null && this.connectionHandler.getConnection().isConnected()) {
         GlidePhase phase = this.getPhase();
         if (phase == GlidePhase.FLYING) {
            this.handleFlyingInput(inputEvent);
         }
      }
   }

   private void handleFlyingInput(InputEvent inputEvent) {
      if (this.state.done() && this.state.shouldLand()) {
         inputEvent.setSneak(true);
      } else {
         if (this.state.verticalGlide()) {
            inputEvent.setJumping(true);
         }

         if (this.state.horizontalGlide()) {
            PlayerInput playerInput = this.state.selectInput();
            inputEvent.setDirectional(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right());
         }
      }
   }

   public void onTick() {
      if (mc.player != null) {
         GlidePhase phase = this.getPhase();
         ConnectionState connection = this.connectionHandler.getConnection();
         if (phase == GlidePhase.LOOTING
            && !this.connectionHandler.isElytraSwaped()
            && connection.getAnarchy() != -1
            && mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem().equals(Items.ELYTRA)
            && System.currentTimeMillis() - this.connectionHandler.getElytraChangeTime() > 2000L) {
            ElytraHelper elytraHelper = Elixir.getInstance().getModuleProvider().get(ElytraHelper.class);
            elytraHelper.changeChestPlate();
            this.connectionHandler.setElytraSwaped(true);
            this.connectionHandler.setElytraChangeTime(System.currentTimeMillis());
         }

         switch (phase) {
            case FLYING:
               this.connectionHandler.onTick();
               if (connection.isConnected() && this.canEnableNoClip()) {
                  this.noClipHandler.onTick();
               } else {
                  this.noClipHandler.end();
               }
               break;
            case LOOTING:
               this.stealHandler.onTick();
               break;
            default:
               this.connectionHandler.onTick();
         }
      }
   }

   public void onMove(MoveEvent moveEvent) {
      if (mc.player != null && this.connectionHandler.getConnection().isConnected()) {
         GlidePhase phase = this.getPhase();
         if (phase == GlidePhase.FLYING) {
            if (this.canEnableNoClip()) {
               this.noClipHandler.onMove(moveEvent);
            } else if (this.state.done() && !this.isAlignedToTarget()) {
               this.setMoveMotion(moveEvent, 0.1);
            }
         }
      }
   }

   private boolean isAlignedToTarget() {
      Vec3d target = this.state.vec3d();
      Vec3d pos = mc.player.getPos();
      double dx = Math.abs(target.x - pos.x);
      double dz = Math.abs(target.z - pos.z);
      return dx < 0.3 && dz < 0.3;
   }

   private void setMoveMotion(MoveEvent move, double motion) {
      if (mc.player != null) {
         ChatMessage.brandmessage("замедляюсь");
         Vec3d target = this.state.vec3d();
         Vec3d pos = mc.player.getPos();
         double dx = target.x - pos.x;
         double dz = target.z - pos.z;
         double dist = Math.sqrt(dx * dx + dz * dz);
         if (dist < 0.05) {
            move.setMovement(new Vec3d(0.0, move.getMovement().y, 0.0));
         } else {
            double nx = dx / dist * motion;
            double nz = dz / dist * motion;
            move.setMovement(new Vec3d(nx, move.getMovement().y, nz));
         }
      }
   }

   public void onPacket(PacketEvent packetEvent) {
      if (mc.player != null && this.state.vec3d() != null && this.connectionHandler.getConnection().isConnected()) {
         GlidePhase phase = this.getPhase();
         if (phase == GlidePhase.FLYING && this.canEnableNoClip()) {
            this.noClipHandler.onPacket(packetEvent);
         }
      }
   }

   public void reload() {
      this.connectionHandler.reload();
      this.noClipHandler.end();
      TurnsConnection.INSTANCE.setRotation(null);
   }

   public boolean canEnableNoClip() {
      return this.state.done() && this.state.solidBlockInRadius(2);
   }

   private GlidePhase getPhase() {
      return this.connectionHandler.getConnection().getPhase();
   }

   public ConnectionHandler getConnectionHandler() {
      return this.connectionHandler;
   }

   public StealHandler getStealHandler() {
      return this.stealHandler;
   }

   public NoClipHandler getNoClipHandler() {
      return this.noClipHandler;
   }

   public GlideState getState() {
      return this.state;
   }
}
