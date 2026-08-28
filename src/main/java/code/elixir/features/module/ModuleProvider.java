package code.elixir.features.module;

import java.util.List;

public class ModuleProvider {
   private final List<Module> modules;

   public <T extends Module> T get(String name) {
      return this.modules.stream().filter(module -> module.getName().equalsIgnoreCase(name)).map(module -> (T)module).findFirst().orElse(null);
   }

   public <T extends Module> T get(Class<T> clazz) {
      return this.modules.stream().filter(module -> clazz.isAssignableFrom(module.getClass())).map(clazz::cast).findFirst().orElse(null);
   }

   public List<Module> getModules() {
      return this.modules;
   }

   public ModuleProvider(List<Module> modules) {
      this.modules = modules;
   }
}
