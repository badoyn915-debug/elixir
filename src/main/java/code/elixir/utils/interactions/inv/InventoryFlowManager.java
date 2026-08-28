package code.elixir.utils.interactions.inv;

import code.elixir.events.player.InputEvent;
import code.elixir.features.impl.movement.GuiMove;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleCategory;
import code.elixir.utils.display.interfaces.QuickImports;
import code.elixir.utils.features.aura.utils.MathAngle;
import code.elixir.utils.features.aura.warp.TurnsConfig;
import code.elixir.utils.features.aura.warp.TurnsConnection;
import code.elixir.utils.interactions.interact.PlayerInteractionHelper;
import code.elixir.utils.interactions.simulate.Simulations;
import code.elixir.utils.math.script.Script;
import code.elixir.utils.math.task.TaskPriority;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.client.gui.screen.ingame.AbstractCommandBlockScreen;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.gui.screen.ingame.StructureBlockScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public final class InventoryFlowManager implements QuickImports {
   public static final List<KeyBinding> moveKeys = List.of(
      mc.options.forwardKey, mc.options.backKey, mc.options.leftKey, mc.options.rightKey, mc.options.jumpKey
   );
   public static final Script script = new Script();
   public static final Script postScript = new Script();
   private static final Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<>();
   public static boolean canMove = true;

   private static void log(String message) {
   }

   public static void tick() {
      script.update();
      InventoryTask.updateSwapAndUseScript();
      Runnable nextTask;
      if (script.isFinished() && !taskQueue.isEmpty() && (nextTask = taskQueue.poll()) != null) {
         log("§a[InventoryFlow] Обрабатываю задачу из очереди");
         addTask(nextTask);
      }
   }

   public static void postMotion() {
      postScript.update();
   }

   public static void input(InputEvent e) {
   }

   private static void waitForGroundAndExecute(Runnable task) {
      script.cleanup().addTickStep(0, () -> {
         disableMoveKeys();
         script.addTickStep(2, () -> {
            task.run();
            enableMoveKeys();
         });
      });
   }

   public static void addTask(Runnable task) {
      boolean scriptFinished = script.isFinished();
      boolean hasMovement = Simulations.hasPlayerMovement();
      String selectedMode = GuiMove.mode.getSelected();
      log("§b[InventoryFlow] addTask вызван | scriptFinished=" + scriptFinished + " | hasMovement=" + hasMovement + " | mode=" + selectedMode);
      if (scriptFinished) {
         log("§a[InventoryFlow] Условие выполнено, добавляю в script");
         switch (selectedMode) {
            case "Funtime":
               log("§a[InventoryFlow] Режим: Funtime");
               script.cleanup().addTickStep(0, () -> {
                  log("§a[InventoryFlow] Funtime: disableMoveKeys");
                  disableMoveKeys();
               }).addTickStep(2, () -> {
                  log("§a[InventoryFlow] Funtime: выполнение задачи");
                  task.run();
               }).addTickStep(2, () -> {
                  log("§a[InventoryFlow] Funtime: enableMoveKeys");
                  enableMoveKeys();
               });
               return;
            case "SpookyTime":
               log("§a[InventoryFlow] Режим: SpookyTime");
               script.cleanup().addTickStep(0, () -> {
                  log("§a[InventoryFlow] SpookyTime: disableMoveKeys");
                  disableMoveKeys();
               }).addTickStep(2, () -> {
                  log("§a[InventoryFlow] SpookyTime: выполнение задачи (swapAndUse)");
                  task.run();
               }).addTickStep(2, () -> {
                  log("§a[InventoryFlow] SpookyTime: enableMoveKeys");
                  enableMoveKeys();
               });
               return;
            case "Reallyworld":
               log("§a[InventoryFlow] Режим: Reallyworld");
               script.cleanup().addTickStep(0, () -> {
                  log("§a[InventoryFlow] Reallyworld: disableMoveKeys");
                  disableMoveKeys();
                  script.addTickStep(0, () -> {
                     log("§a[InventoryFlow] Reallyworld: выполнение задачи");
                     task.run();
                     log("§a[InventoryFlow] Reallyworld: enableMoveKeys");
                     enableMoveKeys();
                  });
               });
               return;
            case "LonyGrief":
               log("§a[InventoryFlow] Режим: LonyGrief");
               script.cleanup().addTickStep(0, () -> {
                  log("§a[InventoryFlow] LonyGrief: disableMoveKeys");
                  disableMoveKeys();
               }).addTickStep(2, () -> {
                  log("§a[InventoryFlow] LonyGrief: выполнение задачи");
                  task.run();
               }).addTickStep(3, () -> {
                  log("§a[InventoryFlow] LonyGrief: enableMoveKeys");
                  enableMoveKeys();
               });
               return;
            case "Holyworld":
               log("§a[InventoryFlow] Режим: Holyworld");
               script.cleanup().addTickStep(0, () -> {
                  log("§a[InventoryFlow] Holyworld: disableMoveKeys");
                  disableMoveKeys();
               }).addTickStep(4, () -> {
                  log("§a[InventoryFlow] Holyworld: выполнение задачи");
                  task.run();
               }).addTickStep(5, () -> {
                  log("§a[InventoryFlow] Holyworld: enableMoveKeys");
                  enableMoveKeys();
               });
               return;
            case "Vanilla":
            case "Легит":
               task.run();
               return;
            default:
               log("§c[InventoryFlow] Неизвестный режим: " + selectedMode + ", добавляю в очередь задач");
               taskQueue.offer(task);
         }
      } else {
         log("§c[InventoryFlow] Условие НЕ выполнено, добавляю в очередь задач");
         taskQueue.offer(task);
      }
   }

   private static void rotateToCamera() {
      Module module = new Module("InventoryComponent", "Inventory Component", ModuleCategory.PLAYER);
      module.state = true;
      TurnsConnection.INSTANCE.rotateTo(MathAngle.cameraAngle(), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_3, module);
   }

   public static void disableMoveKeys() {
      log("§c[InventoryFlow] disableMoveKeys | scriptFinished=" + script.isFinished());
      canMove = false;
      unPressMoveKeys();
   }

   public static void enableMoveKeys() {
      log("§a[InventoryFlow] enableMoveKeys | scriptFinished=" + script.isFinished());
      InventoryTask.closeScreen(true);
      canMove = true;
      updateMoveKeys();
   }

   public static void unPressMoveKeys() {
      moveKeys.forEach(keyBinding -> keyBinding.setPressed(false));
      mc.player.setSprinting(false);
   }

   public static void updateMoveKeys() {
      moveKeys.forEach(keyBinding -> keyBinding.setPressed(InputUtil.isKeyPressed(mc.getWindow().getHandle(), keyBinding.getDefaultKey().getCode())));
   }

   public static boolean shouldSkipExecution() {
      if (mc.currentScreen == null) {
         return false;
      } else if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
         return false;
      } else {
         return !(mc.currentScreen instanceof SignEditScreen)
               && !(mc.currentScreen instanceof AnvilScreen)
               && !(mc.currentScreen instanceof AbstractCommandBlockScreen)
               && !(mc.currentScreen instanceof StructureBlockScreen)
            ? !GuiMove.mode.isSelected("Holyworld") || !(mc.currentScreen instanceof GenericContainerScreen)
            : false;
      }
   }

   private InventoryFlowManager() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
