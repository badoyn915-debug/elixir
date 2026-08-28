package code.elixir.utils.client.managers.event;

import code.elixir.features.module.exception.ModuleException;
import code.elixir.utils.client.managers.event.events.Event;
import code.elixir.utils.client.managers.event.events.EventStoppable;
import code.elixir.utils.client.managers.event.types.Priority;
import code.elixir.utils.display.interfaces.QuickImports;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class EventManager implements QuickImports {
   private static final Map<Class<? extends Event>, List<EventManager.MethodData>> REGISTRY_MAP = new HashMap<>();
   private static final Map<Class<? extends Event>, EventManager.FastInvoker[]> FAST_LOOKUP_MAP = new ConcurrentHashMap<>();
   private static final Logger LOGGER = LogManager.getLogger("Elixir");
   private static final Lookup LOOKUP = MethodHandles.lookup();

   public static void register(Object object) {
      if (canInspectEventHandlers(object)) {
         Method[] methods = getDeclaredEventHandlerMethods(object);
         if (methods != null) {
            for (Method method : methods) {
               if (!isMethodBad(method)) {
                  register(method, object);
               }
            }
         }
      }
   }

   public void register(Object object, Class<? extends Event> eventClass) {
      if (canInspectEventHandlers(object)) {
         Method[] methods = getDeclaredEventHandlerMethods(object);
         if (methods != null) {
            for (Method method : methods) {
               if (!isMethodBad(method, eventClass)) {
                  register(method, object);
               }
            }
         }
      }
   }

   public static boolean canInspectEventHandlers(Object object) {
      if (object == null) {
         return false;
      } else {
         try {
            object.getClass().getDeclaredMethods();
            return true;
         } catch (Throwable var2) {
            LOGGER.error("Skipping broken event handler source {}", object.getClass().getName(), var2);
            return false;
         }
      }
   }

   private static Method[] getDeclaredEventHandlerMethods(Object object) {
      try {
         return object.getClass().getDeclaredMethods();
      } catch (Throwable var2) {
         LOGGER.error("Failed to inspect event handlers for {}", object.getClass().getName(), var2);
         return null;
      }
   }

   public void unregister(Object object) {
      for (List<EventManager.MethodData> dataList : REGISTRY_MAP.values()) {
         dataList.removeIf(data -> data.source().equals(object));
      }

      cleanMap(true);
      rebuildAllFastLookups();
   }

   public void unregister(Object object, Class<? extends Event> eventClass) {
      if (REGISTRY_MAP.containsKey(eventClass)) {
         REGISTRY_MAP.get(eventClass).removeIf(data -> data.source().equals(object));
         cleanMap(true);
         rebuildFastLookup(eventClass);
      }
   }

   private static void register(Method method, Object object) {
      Class<? extends Event> indexClass = method.getParameterTypes()[0].asSubclass(Event.class);
      MethodHandle handle = null;

      try {
         if (!method.isAccessible()) {
            method.setAccessible(true);
         }

         handle = LOOKUP.unreflect(method);
      } catch (Throwable var6) {
         LOGGER.warn("Failed to create MethodHandle for {}: {}", method.getName(), var6.getMessage());
      }

      EventManager.MethodData data = new EventManager.MethodData(object, method, handle, method.getAnnotation(EventHandler.class).value());
      if (REGISTRY_MAP.containsKey(indexClass)) {
         List<EventManager.MethodData> list = REGISTRY_MAP.get(indexClass);
         if (!list.contains(data)) {
            list.add(data);
            sortListValue(indexClass);
         }
      } else {
         List<EventManager.MethodData> list = new ArrayList<>();
         list.add(data);
         REGISTRY_MAP.put(indexClass, list);
         rebuildFastLookup(indexClass);
      }
   }

   public void removeEntry(Class<? extends Event> indexClass) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         if (mapIterator.next().getKey().equals(indexClass)) {
            mapIterator.remove();
            break;
         }
      }

      FAST_LOOKUP_MAP.remove(indexClass);
   }

   public static void cleanMap(boolean onlyEmptyEntries) {
      Iterator<Entry<Class<? extends Event>, List<EventManager.MethodData>>> mapIterator = REGISTRY_MAP.entrySet().iterator();

      while (mapIterator.hasNext()) {
         Entry<Class<? extends Event>, List<EventManager.MethodData>> entry = mapIterator.next();
         if (!onlyEmptyEntries || entry.getValue().isEmpty()) {
            mapIterator.remove();
            FAST_LOOKUP_MAP.remove(entry.getKey());
         }
      }
   }

   private static void sortListValue(Class<? extends Event> indexClass) {
      List<EventManager.MethodData> current = REGISTRY_MAP.get(indexClass);
      if (current == null) {
         FAST_LOOKUP_MAP.remove(indexClass);
      } else {
         ArrayList<EventManager.MethodData> sortedList = new ArrayList<>(current.size());

         for (byte priority : Priority.VALUE_ARRAY) {
            for (EventManager.MethodData data : current) {
               if (data.priority() == priority) {
                  sortedList.add(data);
               }
            }
         }

         REGISTRY_MAP.put(indexClass, sortedList);
         rebuildFastLookup(indexClass);
      }
   }

   private static void rebuildFastLookup(Class<? extends Event> eventClass) {
      List<EventManager.MethodData> list = REGISTRY_MAP.get(eventClass);
      if (list != null && !list.isEmpty()) {
         EventManager.FastInvoker[] array = new EventManager.FastInvoker[list.size()];

         for (int i = 0; i < list.size(); i++) {
            EventManager.MethodData md = list.get(i);
            array[i] = new EventManager.FastInvoker(md.source(), md.target(), md.handle());
         }

         FAST_LOOKUP_MAP.put(eventClass, array);
      } else {
         FAST_LOOKUP_MAP.remove(eventClass);
      }
   }

   private static void rebuildAllFastLookups() {
      FAST_LOOKUP_MAP.clear();

      for (Class<? extends Event> eventClass : REGISTRY_MAP.keySet()) {
         rebuildFastLookup(eventClass);
      }
   }

   private static boolean isMethodBad(Method method) {
      return method.getParameterTypes().length != 1 || !method.isAnnotationPresent(EventHandler.class);
   }

   private static boolean isMethodBad(Method method, Class<? extends Event> eventClass) {
      return isMethodBad(method) || !method.getParameterTypes()[0].equals(eventClass);
   }

   public static Event callEvent(Event event) {
      try {
         return callEventInternal(event);
      } catch (VirtualMachineError | LinkageError var2) {
         LOGGER.warn("Skipped event dispatch during class reload: {}", event != null ? event.getClass().getName() : "null", var2);
         return event;
      } catch (Throwable var3) {
         LOGGER.warn("Event dispatch failed safely: {}", event != null ? event.getClass().getName() : "null", var3);
         return event;
      }
   }

   private static Event callEventInternal(Event event) {
      EventManager.FastInvoker[] handlers = FAST_LOOKUP_MAP.get(event.getClass());
      if (handlers != null && handlers.length != 0) {
         boolean stoppable = event instanceof EventStoppable;

         for (int i = 0; i < handlers.length; i++) {
            handlers[i].invoke(event);
            if (stoppable && ((EventStoppable)event).isStopped()) {
               break;
            }
         }

         return event;
      } else {
         return event;
      }
   }

   private static void logModuleException(ModuleException moduleException) {
      MutableText message = Text.literal("[" + moduleException.getModuleName() + "] ")
         .append(Text.literal(moduleException.getMessage()).formatted(Formatting.RED));
      if (mc != null && mc.player != null && mc.inGameHud != null) {
         mc.inGameHud.getChatHud().addMessage(message);
      } else {
         LOGGER.warn("[{}] {}", moduleException.getModuleName(), moduleException.getMessage());
      }
   }

   public static boolean hasListeners(Class<? extends Event> eventClass) {
      EventManager.FastInvoker[] listeners = FAST_LOOKUP_MAP.get(eventClass);
      return listeners != null && listeners.length > 0;
   }

   private static final class FastInvoker {
      private final Object source;
      private final Method target;
      private final MethodHandle handle;

      public FastInvoker(Object source, Method target, MethodHandle handle) {
         this.source = source;
         this.target = target;
         this.handle = handle;
      }

      public void invoke(Event argument) {
         try {
            if (this.handle != null) {
               this.handle.invoke((Object)this.source, (Event)argument);
               return;
            }

            this.target.invoke(this.source, argument);
         } catch (ModuleException var5) {
            EventManager.logModuleException(var5);
         } catch (InvocationTargetException var6) {
            Throwable cause = (Throwable)(var6.getCause() != null ? var6.getCause() : var6);
            if (cause instanceof ModuleException moduleException) {
               EventManager.logModuleException(moduleException);
            } else {
               EventManager.LOGGER
                  .error("Exception in event handler {} on {} for {}", this.target.getName(), this.source.getClass().getName(), argument, cause);
            }
         } catch (Throwable var7) {
            if ((var7.getCause() != null ? var7.getCause() : var7) instanceof ModuleException moduleException) {
               EventManager.logModuleException(moduleException);
            } else {
               EventManager.LOGGER
                  .error("Unexpected error in event handler {} on {} for {}", this.target.getName(), this.source.getClass().getName(), argument, var7);
            }
         }
      }
   }

   private record MethodData(Object source, Method target, MethodHandle handle, byte priority) {
   }
}
