package code.elixir.utils.interactions.interact;

import code.elixir.events.player.RotationUpdateEvent;
import code.elixir.utils.client.managers.event.EventHandler;
import code.elixir.utils.client.managers.event.EventManager;
import code.elixir.utils.client.managers.event.impl.EventUpdate;
import java.util.LinkedList;
import java.util.Queue;

public class ScriptManager {
   private final Queue<ScriptManager.ScriptTask> tasks = new LinkedList<>();

   public ScriptManager() {
      EventManager.register(this);
   }

   public void tick(Object event) {
      ScriptManager.ScriptTask currentTask = this.tasks.peek();
      if (currentTask != null) {
         boolean finished = currentTask.tryTick(event);
         if (finished) {
            this.tasks.poll();
         }
      }
   }

   @EventHandler
   public void rotateTick(RotationUpdateEvent tickRotate) {
      this.tick(tickRotate);
   }

   @EventHandler(0)
   public void updateTick(EventUpdate tickUpdate) {
      this.tick(tickUpdate);
   }

   public void addTask(ScriptManager.ScriptTask task) {
      this.tasks.add(task);
   }

   public boolean isFinished() {
      return this.tasks.isEmpty();
   }

   public static class ScriptTask {
      private final Queue<ScriptManager.ScriptTask.Step<?>> steps = new LinkedList<>();
      private int idleTicks = 0;
      private int maxIdleTicks = 400;

      public ScriptManager.ScriptTask withMaxIdleTicks(int maxIdleTicks) {
         this.maxIdleTicks = Math.max(1, maxIdleTicks);
         return this;
      }

      public <E> ScriptManager.ScriptTask schedule(Class<E> eventClass, ScriptManager.ScriptTask.StepTask<E> action) {
         this.steps.add(new ScriptManager.ScriptTask.Step<>(eventClass, action));
         return this;
      }

      public boolean tryTick(Object event) {
         ScriptManager.ScriptTask.Step<?> nextStep = this.steps.peek();
         if (nextStep == null) {
            return true;
         } else {
            boolean progressed = false;
            if (nextStep.eventClass.isInstance(event) && nextStep.execute(event)) {
               this.steps.poll();
               progressed = true;
            }

            if (progressed) {
               this.idleTicks = 0;
               return this.steps.isEmpty();
            } else {
               this.idleTicks++;
               if (this.idleTicks > this.maxIdleTicks) {
                  this.steps.clear();
                  return true;
               } else {
                  return false;
               }
            }
         }
      }

      public boolean isCompleted() {
         return this.steps.isEmpty();
      }

      private static class Step<E> {
         private final Class<E> eventClass;
         private final ScriptManager.ScriptTask.StepTask<E> action;

         Step(Class<E> eventClass, ScriptManager.ScriptTask.StepTask<E> action) {
            this.eventClass = eventClass;
            this.action = action;
         }

         boolean execute(Object event) {
            return this.action.accept(this.eventClass.cast(event));
         }
      }

      @FunctionalInterface
      public interface StepTask<E> {
         boolean accept(E var1);
      }
   }
}
