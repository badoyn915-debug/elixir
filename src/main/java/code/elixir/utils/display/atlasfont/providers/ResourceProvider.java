package code.elixir.utils.display.atlasfont.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public final class ResourceProvider {
   private static final Gson GSON = new Gson();

   public static Identifier getShaderIdentifier(String name) {
      return Identifier.of("mre", "core/" + name);
   }

   public static JsonObject toJson(Identifier identifier) {
      return JsonParser.parseString(toString(identifier)).getAsJsonObject();
   }

   public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
      return (T)GSON.fromJson(toString(identifier), clazz);
   }

   public static String toString(Identifier identifier) {
      return toString(identifier, "\n");
   }

   public static String toString(Identifier identifier, String delimiter) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null) {
         throw new IllegalStateException("MinecraftClient is not initialized yet for resource: " + identifier);
      } else {
         ResourceManager resourceManager = client.getResourceManager();
         if (resourceManager == null) {
            throw new IllegalStateException("ResourceManager is not ready yet for resource: " + identifier);
         } else {
            try {
               Optional resourceOpt = resourceManager.getResource(identifier);
               if (resourceOpt.isEmpty()) {
                  throw new RuntimeException(
                     "Resource not found: " + identifier + "; Make sure the file exists in assets/" + identifier.getNamespace() + "/" + identifier.getPath()
                  );
               } else {
                  String var15;
                  try (InputStream inputStream = ((Resource)resourceOpt.get()).getInputStream()) {
                     String string;
                     try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                        string = reader.lines().collect(Collectors.joining(delimiter));
                     }

                     var15 = string;
                  }

                  return var15;
               }
            } catch (IOException var14) {
               System.err.println("[ResourceProvider] CRITICAL: Failed to load resource: " + identifier);
               var14.printStackTrace();
               throw new RuntimeException("Failed to load resource: " + identifier, var14);
            }
         }
      }
   }
}
