package code.elixir.utils.client.managers.event;

import code.elixir.utils.client.managers.event.events.Event;
import java.util.function.Supplier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SafeEvents {
   private static final Logger LOGGER = LogManager.getLogger("Elixir");

   private SafeEvents() {
   }

   public static void run(Runnable action) {
      try {
         action.run();
      } catch (Throwable var2) {
         if (var2 instanceof LinkageError || var2 instanceof VirtualMachineError) {
            LOGGER.warn("Skipped mixin event hook during class reload: {}", var2.toString());
         }

         LOGGER.warn("Mixin event hook failed safely", var2);
      }
   }

   public static <E extends Event> E dispatch(Class<E> eventClass, Supplier<E> factory) {
      if (!EventManager.hasListeners(eventClass)) {
         return null;
      } else {
         try {
            E event = (E)factory.get();
            if (event != null) {
               EventManager.callEvent(event);
            }

            return event;
         } catch (Throwable var3) {
            if (!(var3 instanceof LinkageError) && !(var3 instanceof VirtualMachineError)) {
               LOGGER.warn("Event dispatch failed safely", var3);
            } else {
               LOGGER.warn("Skipped event dispatch during class reload: {}", var3.toString());
            }

            return null;
         }
      }
   }

   public static <E extends Event> E dispatch(Supplier<E> factory) {
      try {
         E event = (E)factory.get();
         if (event != null && EventManager.hasListeners((Class<? extends Event>)event.getClass())) {
            EventManager.callEvent(event);
         }

         return event;
      } catch (Throwable var2) {
         if (!(var2 instanceof LinkageError) && !(var2 instanceof VirtualMachineError)) {
            LOGGER.warn("Event dispatch failed safely", var2);
         } else {
            LOGGER.warn("Skipped event dispatch during class reload: {}", var2.toString());
         }

         return null;
      }
   }

   public static void dispatch(Event event) {
      if (event != null && EventManager.hasListeners((Class<? extends Event>)event.getClass())) {
         try {
            EventManager.callEvent(event);
         } catch (Throwable var2) {
            if (!(var2 instanceof LinkageError) && !(var2 instanceof VirtualMachineError)) {
               LOGGER.warn("Event dispatch failed safely", var2);
            } else {
               LOGGER.warn("Skipped event dispatch during class reload: {}", var2.toString());
            }
         }
      }
   }

   public static boolean hasListeners(Class<? extends Event> eventClass) {
      try {
         return EventManager.hasListeners(eventClass);
      } catch (Throwable var2) {
         return false;
      }
   }
}
