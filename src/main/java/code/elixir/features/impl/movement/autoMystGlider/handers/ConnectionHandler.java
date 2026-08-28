package code.elixir.features.impl.movement.autoMystGlider.handers;

import code.elixir.features.impl.movement.autoMystGlider.parsers.SpookyParser;
import code.elixir.features.impl.movement.autoMystGlider.receivers.AutoMystGliderModule;
import code.elixir.features.impl.movement.autoMystGlider.states.ConnectionState;
import code.elixir.features.impl.movement.autoMystGlider.states.GlidePhase;
import code.elixir.features.impl.movement.autoMystGlider.states.GlideState;
import code.elixir.utils.client.chat.ChatMessage;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.interactions.inv.InventoryTask;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos.Mutable;

public class ConnectionHandler implements QuickImports {
   private final GlideState glideState;
   private final ConnectionState connection;
   private final AutoMystGliderModule module;
   private long startTime = -1L;
   private long elytraChangeTime;
   private boolean elytraSwaped = false;
   private boolean homeTeleported = false;
   private long homeTpTime = -1L;
   private long lastDropTime = -1L;
   private boolean eventDelaySent = false;
   private long eventDelayTime = 0L;
   private boolean apiFetched = false;
   private boolean apiReady = false;
   private int nextAnarchy = -1;
   private long apiFetchTime = 0L;
   private long lastPhaseLogTime = 0L;
   private boolean rtpSent = false;
   private boolean slotClicked = false;

   public ConnectionHandler(GlideState glideState, StealHandler stealHandler, AutoMystGliderModule module) {
      this.glideState = glideState;
      this.module = module;
      this.connection = new ConnectionState(glideState, stealHandler);
   }

   public void onTick() {
      GlidePhase phase = this.connection.getPhase();
      if (System.currentTimeMillis() - this.lastPhaseLogTime > 5000L) {
         long elapsed = System.currentTimeMillis() - this.connection.getPhaseStartTime();
         ChatMessage.brandmessage("[AutoMyst] Фаза: " + phase + " | connected=" + this.connection.isConnected() + " | elapsed=" + elapsed / 1000L + "s");
         this.lastPhaseLogTime = System.currentTimeMillis();
      }

      switch (phase) {
         case FLYING:
            this.handleFlying();
            break;
         case WAITING_REOPEN:
            this.handleWaitingReopen();
         case LOOTING:
         default:
            break;
         case HUB_TO_HOME:
            this.handleHubToHome();
            break;
         case AT_HOME:
            this.handleAtHome();
            break;
         case HUB_TO_NEXT:
            this.handleHubToNext();
            break;
         case WAITING_COORDS:
            this.handleWaitingCoords();
      }
   }

   private void handleFlying() {
      if (this.connection.successfully() && this.isPlayerInBlocks() && this.isPlayer4BlocksBelowTarget() && this.connection.isConnected()) {
         if (this.startTime == -1L) {
            this.startTime = System.currentTimeMillis();
         }

         if (System.currentTimeMillis() - this.startTime >= 700L) {
            this.connection.setAnarchy(SpookyParser.parseAnarchy());
            this.connection.setRemainingTime(SpookyParser.parseRemainingTime());
            this.connection.getStealHandler().onDisconnect();
            this.connection.hub(GlidePhase.WAITING_REOPEN);
            ChatMessage.brandmessage(
               "Фаза: ожидание реопена (ан " + this.connection.getAnarchy() + ", ~" + this.connection.getRemainingTime() / 1000L + " сек)"
            );
            this.startTime = -1L;
         }
      } else {
         this.startTime = -1L;
      }
   }

   private void handleWaitingReopen() {
      if (System.currentTimeMillis() - this.connection.getLeaveTime() > this.connection.getRemainingTime() && this.connection.getAnarchy() != -1) {
         this.connection.getStealHandler().onConnect();
         this.connection.joinAnarchy(this.connection.getAnarchy(), GlidePhase.LOOTING);
         ChatMessage.brandmessage("Фаза: заходим на ан" + this.connection.getAnarchy() + " для лута");
      }
   }

   private void handleHubToHome() {
      long elapsed = System.currentTimeMillis() - this.connection.getPhaseStartTime();
      int homeAnarchy;
      if (elapsed > 3000L && (homeAnarchy = this.getHomeAnarchy()) != -1) {
         this.connection.joinAnarchy(homeAnarchy, GlidePhase.AT_HOME);
         this.homeTeleported = false;
         this.homeTpTime = -1L;
         this.lastDropTime = -1L;
         ChatMessage.brandmessage("Фаза: заходим на домашнюю ан" + homeAnarchy);
      }
   }

   private void handleAtHome() {
      long elapsed = System.currentTimeMillis() - this.connection.getPhaseStartTime();
      if (elapsed >= 3000L) {
         if (!this.homeTeleported) {
            String home = this.module.getHomeName().getText();
            mc.getNetworkHandler().sendCommand("home " + home);
            this.homeTeleported = true;
            this.homeTpTime = System.currentTimeMillis();
            ChatMessage.brandmessage("Фаза: телепортируемся /home " + home);
         } else if (System.currentTimeMillis() - this.homeTpTime >= 3000L) {
            if (mc.player.currentScreenHandler instanceof GenericContainerScreenHandler handler) {
               for (Slot slot : handler.slots) {
                  if (slot.inventory == mc.player.getInventory() && slot.hasStack() && System.currentTimeMillis() - this.lastDropTime > 150L) {
                     InventoryTask.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, true);
                     this.lastDropTime = System.currentTimeMillis();
                     break;
                  }
               }

               if (handler.slots.stream().filter(s -> s.inventory == mc.player.getInventory()).noneMatch(Slot::hasStack)) {
                  mc.currentScreen.close();
                  this.goToNextPhase();
               }
            } else if (mc.currentScreen == null) {
               BlockPos chest = this.findNearestChest();
               if (chest != null && System.currentTimeMillis() - this.lastDropTime > 800L) {
                  Vec3d center = Vec3d.ofCenter(chest).add(0.2, 0.375, 0.2);
                  Direction side = Direction.getFacing(center.x - mc.player.getX(), center.y - mc.player.getY(), center.z - mc.player.getZ()).getOpposite();
                  mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(center, side, chest, false));
                  mc.player.swingHand(Hand.MAIN_HAND);
                  this.lastDropTime = System.currentTimeMillis();
               } else if (chest == null) {
                  this.goToNextPhase();
               }
            }

            if (System.currentTimeMillis() - this.homeTpTime > 30000L) {
               if (mc.currentScreen != null) {
                  mc.currentScreen.close();
               }

               this.goToNextPhase();
            }
         }
      }
   }

   private void goToNextPhase() {
      this.apiFetched = false;
      this.apiReady = false;
      this.nextAnarchy = -1;
      this.connection.hub(GlidePhase.HUB_TO_NEXT);
      ChatMessage.brandmessage("Фаза: ресы скинуты, ищем следующий ивент");
   }

   private void handleWaitingCoords() {
      long elapsed = System.currentTimeMillis() - this.connection.getPhaseStartTime();
      if (elapsed >= 3000L) {
         if (!this.eventDelaySent) {
            mc.getNetworkHandler().sendCommand("event delay");
            mc.getNetworkHandler().sendCommand("rtp");
            this.eventDelaySent = true;
            this.eventDelayTime = System.currentTimeMillis();
            this.rtpSent = false;
            this.slotClicked = false;
            ChatMessage.brandmessage("[AutoMyst] Отправлено /event delay и /rtp, ждём координаты...");
         } else {
            if (!this.slotClicked && System.currentTimeMillis() - this.eventDelayTime > 100L) {
               InventoryTask.clickSlot(13, 0, SlotActionType.QUICK_MOVE, false);
               this.slotClicked = true;
               ChatMessage.brandmessage("[AutoMyst] Кликнули по слоту 15");
            }
         }
      }
   }

   private void handleHubToNext() {
      long elapsed = System.currentTimeMillis() - this.connection.getPhaseStartTime();
      if (elapsed > 2000L && !this.apiFetched) {
         this.apiFetched = true;
         this.apiFetchTime = System.currentTimeMillis();
         ChatMessage.brandmessage("[AutoMyst] Запрашиваем API...");
         new Thread(() -> {
            SpookyParser.fetchFromApi();
            this.nextAnarchy = SpookyParser.getCachedAnarchy();
            this.apiReady = true;
            ChatMessage.brandmessage("[AutoMyst] API ответил: анархия=" + this.nextAnarchy);
         }, "SpookyAPI-Fetch").start();
      }

      if (this.apiReady && this.nextAnarchy != -1) {
         ChatMessage.brandmessage("[AutoMyst] Заходим на ан" + this.nextAnarchy + ", ждём координаты из чата");
         this.eventDelaySent = false;
         this.rtpSent = false;
         this.slotClicked = false;
         this.connection.joinAnarchy(this.nextAnarchy, GlidePhase.WAITING_COORDS);
         this.apiFetched = false;
         this.apiReady = false;
      }

      if (this.apiReady && this.nextAnarchy == -1 && System.currentTimeMillis() - this.apiFetchTime > 30000L) {
         this.apiFetched = false;
         this.apiReady = false;
         ChatMessage.brandmessage("[AutoMyst] Нет ивентов, повторный запрос API...");
      }
   }

   private int getHomeAnarchy() {
      try {
         return Integer.parseInt(this.module.getAnarchyText().getText());
      } catch (NumberFormatException var2) {
         return -1;
      }
   }

   private boolean isPlayerInBlocks() {
      BlockPos playerPos = mc.player.getBlockPos();
      return mc.player.getWorld().getBlockState(playerPos).isSolid();
   }

   private boolean isPlayer4BlocksBelowTarget() {
      double targetY = this.glideState.vec3d().getY();
      double playerY = mc.player.getY();
      return playerY <= targetY - 3.0;
   }

   private BlockPos findNearestChest() {
      BlockPos playerPos = mc.player.getBlockPos();
      int range = 5;
      Mutable nearest = new Mutable(0, 0, 0);
      double nearestDistSq = Double.MAX_VALUE;

      for (BlockPos pos : BlockPos.iterate(playerPos.add(-range, -range, -range), playerPos.add(range, range, range))) {
         BlockState state = mc.world.getBlockState(pos);
         double distSq;
         if ((state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST) || state.isOf(Blocks.ENDER_CHEST))
            && (distSq = mc.player.squaredDistanceTo(Vec3d.ofCenter(pos))) < nearestDistSq
            && distSq <= 20.25) {
            nearestDistSq = distSq;
            nearest.set(pos);
         }
      }

      return nearestDistSq < Double.MAX_VALUE ? nearest.toImmutable() : null;
   }

   public void reload() {
      this.connection.reload();
      this.elytraSwaped = false;
      this.elytraChangeTime = -1L;
      this.homeTeleported = false;
      this.homeTpTime = -1L;
      this.lastDropTime = -1L;
      this.apiFetched = false;
      this.apiReady = false;
      this.nextAnarchy = -1;
      this.eventDelaySent = false;
      this.rtpSent = false;
      this.slotClicked = false;
   }

   public void setStartTime(long startTime) {
      this.startTime = startTime;
   }

   public void setElytraChangeTime(long elytraChangeTime) {
      this.elytraChangeTime = elytraChangeTime;
   }

   public void setElytraSwaped(boolean elytraSwaped) {
      this.elytraSwaped = elytraSwaped;
   }

   public void setHomeTeleported(boolean homeTeleported) {
      this.homeTeleported = homeTeleported;
   }

   public void setHomeTpTime(long homeTpTime) {
      this.homeTpTime = homeTpTime;
   }

   public void setLastDropTime(long lastDropTime) {
      this.lastDropTime = lastDropTime;
   }

   public void setEventDelaySent(boolean eventDelaySent) {
      this.eventDelaySent = eventDelaySent;
   }

   public void setEventDelayTime(long eventDelayTime) {
      this.eventDelayTime = eventDelayTime;
   }

   public void setApiFetched(boolean apiFetched) {
      this.apiFetched = apiFetched;
   }

   public void setApiReady(boolean apiReady) {
      this.apiReady = apiReady;
   }

   public void setNextAnarchy(int nextAnarchy) {
      this.nextAnarchy = nextAnarchy;
   }

   public void setApiFetchTime(long apiFetchTime) {
      this.apiFetchTime = apiFetchTime;
   }

   public void setLastPhaseLogTime(long lastPhaseLogTime) {
      this.lastPhaseLogTime = lastPhaseLogTime;
   }

   public void setRtpSent(boolean rtpSent) {
      this.rtpSent = rtpSent;
   }

   public void setSlotClicked(boolean slotClicked) {
      this.slotClicked = slotClicked;
   }

   public GlideState getGlideState() {
      return this.glideState;
   }

   public ConnectionState getConnection() {
      return this.connection;
   }

   public AutoMystGliderModule getModule() {
      return this.module;
   }

   public long getStartTime() {
      return this.startTime;
   }

   public long getElytraChangeTime() {
      return this.elytraChangeTime;
   }

   public boolean isElytraSwaped() {
      return this.elytraSwaped;
   }

   public boolean isHomeTeleported() {
      return this.homeTeleported;
   }

   public long getHomeTpTime() {
      return this.homeTpTime;
   }

   public long getLastDropTime() {
      return this.lastDropTime;
   }

   public boolean isEventDelaySent() {
      return this.eventDelaySent;
   }

   public long getEventDelayTime() {
      return this.eventDelayTime;
   }

   public boolean isApiFetched() {
      return this.apiFetched;
   }

   public boolean isApiReady() {
      return this.apiReady;
   }

   public int getNextAnarchy() {
      return this.nextAnarchy;
   }

   public long getApiFetchTime() {
      return this.apiFetchTime;
   }

   public long getLastPhaseLogTime() {
      return this.lastPhaseLogTime;
   }

   public boolean isRtpSent() {
      return this.rtpSent;
   }

   public boolean isSlotClicked() {
      return this.slotClicked;
   }
}
