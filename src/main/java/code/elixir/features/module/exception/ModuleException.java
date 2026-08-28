package code.elixir.features.module.exception;

public final class ModuleException extends RuntimeException {
   private final String message;
   private final String moduleName;

   public ModuleException(String message, String moduleName) {
      this.message = message;
      this.moduleName = moduleName;
   }

   @Override
   public String getMessage() {
      return this.message;
   }

   public String getModuleName() {
      return this.moduleName;
   }

   @Override
   public String toString() {
      return "ModuleException(message=" + this.getMessage() + ", moduleName=" + this.getModuleName() + ")";
   }

   @Override
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ModuleException other)) {
         return false;
      } else if (!other.canEqual(this)) {
         return false;
      } else if (!super.equals(o)) {
         return false;
      } else {
         String this$message = this.getMessage();
         String other$message = other.getMessage();
         if (this$message == null ? other$message == null : this$message.equals(other$message)) {
            String this$moduleName = this.getModuleName();
            String other$moduleName = other.getModuleName();
            return this$moduleName == null ? other$moduleName == null : this$moduleName.equals(other$moduleName);
         } else {
            return false;
         }
      }
   }

   protected boolean canEqual(Object other) {
      return other instanceof ModuleException;
   }

   @Override
   public int hashCode() {
      int PRIME = 59;
      int result = super.hashCode();
      String $message = this.getMessage();
      result = result * 59 + ($message == null ? 43 : $message.hashCode());
      String $moduleName = this.getModuleName();
      return result * 59 + ($moduleName == null ? 43 : $moduleName.hashCode());
   }
}
