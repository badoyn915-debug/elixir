package code.elixir.common.logger.implement;

import code.elixir.common.logger.Logger;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;

public class ConsoleLogger implements Logger {
   private final org.apache.logging.log4j.Logger logger = LogManager.getLogger("Elixir");

   @Override
   public void log(Object message) {
      this.logger.info("[Elixir] {}", message);
   }

   @Override
   public void minecraftLog(Text... components) {
   }
}
