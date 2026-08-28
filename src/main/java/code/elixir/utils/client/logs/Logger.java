package code.elixir.utils.client.logs;

import org.apache.logging.log4j.LogManager;

public class Logger {
   private static final org.apache.logging.log4j.Logger log = LogManager.getLogger(Logger.class);

   public static void info(Object message) {
      log.info("\u001b[0;30m\u001b[42m" + message + "\u001b[0m");
   }

   public static void info(Object message, Throwable exception) {
      log.info("\u001b[0;30m\u001b[42m" + message + "\u001b[0m", exception);
   }

   public static void info(Object message, Object o) {
      log.info("\u001b[0;30m\u001b[42m" + message + "\u001b[0m", o);
   }

   public static void warn(Object message) {
      log.warn("\u001b[0;30m\u001b[43m" + message + "\u001b[0m");
   }

   public static void warn(Object message, Throwable exception) {
      log.warn("\u001b[0;30m\u001b[43m" + message + "\u001b[0m", exception);
   }

   public static void warn(Object message, Object o) {
      log.warn("\u001b[0;30m\u001b[43m" + message + "\u001b[0m", o);
   }

   public static void error(Object message) {
      log.error("\u001b[0;30m\u001b[41m" + message + "\u001b[0m");
   }

   public static void error(Object message, Throwable exception) {
      log.error("\u001b[0;30m\u001b[41m" + message + "\u001b[0m", exception);
   }

   public void error(Object message, Object o) {
      log.error("\u001b[0;30m\u001b[41m" + message + "\u001b[0m", o);
   }
}
