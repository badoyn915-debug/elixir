package code.elixir.utils.client;

import code.elixir.Elixir;
import code.elixir.features.module.Module;
import code.elixir.features.module.ModuleProvider;
import code.elixir.features.module.ModuleRepository;
import code.elixir.utils.client.managers.api.draggable.AbstractDraggable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class Instance {
   private static final ConcurrentMap<Class<? extends Module>, Module> instanceModules = new ConcurrentHashMap<>();
   private static final ConcurrentMap<Class<? extends AbstractDraggable>, AbstractDraggable> instanceDraggables = new ConcurrentHashMap<>();

   public static <T extends Module> T get(Class<T> clazz) {
      return clazz.cast(
         instanceModules.computeIfAbsent(
            clazz,
            instance -> {
               Elixir alphaDlc = Elixir.getInstance();
               if (alphaDlc == null) {
                  return null;
               } else {
                  ModuleProvider moduleProvider = alphaDlc.getModuleProvider();
                  if (moduleProvider != null) {
                     return moduleProvider.get((Class<? extends Module>)instance);
                  } else {
                     ModuleRepository moduleRepository = alphaDlc.getModuleRepository();
                     return moduleRepository != null
                        ? moduleRepository.modules().stream().filter(m -> instance.isAssignableFrom(m.getClass())).map(instance::cast).findFirst().orElse(null)
                        : null;
                  }
               }
            }
         )
      );
   }

   public static <T extends Module> T get(String module) {
      Elixir alphaDlc = Elixir.getInstance();
      if (alphaDlc == null) {
         return null;
      } else {
         ModuleProvider moduleProvider = alphaDlc.getModuleProvider();
         if (moduleProvider != null) {
            return moduleProvider.get(module);
         } else {
            ModuleRepository moduleRepository = alphaDlc.getModuleRepository();
            return (T)(moduleRepository != null
               ? moduleRepository.modules().stream().filter(m -> m.getName().equalsIgnoreCase(module)).findFirst().orElse(null)
               : null);
         }
      }
   }

   public static <T extends AbstractDraggable> T getDraggable(Class<T> clazz) {
      return clazz.cast(
         instanceDraggables.computeIfAbsent(clazz, instance -> Elixir.getInstance().getDraggableRepository().get((Class<? extends AbstractDraggable>)instance))
      );
   }

   public static <T extends AbstractDraggable> T getDraggable(String draggable) {
      return Elixir.getInstance().getDraggableRepository().get(draggable);
   }

   private Instance() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
