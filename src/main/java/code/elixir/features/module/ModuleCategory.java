package code.elixir.features.module;

public enum ModuleCategory {
   COMBAT("Combat"),
   MOVEMENT("Movement"),
   RENDER("Render"),
   ESP("ESP"),
   PLAYER("Player"),
   MISC("Misc"),
   CUSTOMIZATION("Customization"),
   CONFIGS("IRC");

   final String readableName;

   private ModuleCategory(String readableName) {
      this.readableName = readableName;
   }

   public String getReadableName() {
      return this.readableName;
   }
}
