package code.elixir.features.impl.misc.newAutoMine.api;

import com.google.gson.Gson;
import java.net.http.HttpClient;
import java.time.Duration;

public interface MineApi {
   String URL = "http://85.208.139.128:8000/api/mines";
   Gson GSON = new Gson();
   HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2L)).build();
}
