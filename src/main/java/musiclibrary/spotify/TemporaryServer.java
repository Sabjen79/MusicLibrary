package musiclibrary.spotify;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.InetSocketAddress;

public class TemporaryServer {
    private static HttpServer server;
    private static String request = null;

    public static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 7999), 0);
        server.createContext("/spotify", exchange -> {
            if("GET".equals(exchange.getRequestMethod())) {

                SpotifyAuth.auth(exchange.getRequestURI().toString().split("code=")[1]);

                OutputStream outputStream = exchange.getResponseBody();
                StringBuilder htmlBuilder = new StringBuilder();

                htmlBuilder.append("""
                        <!doctype html><html><head><script>
                              window.onload = function load() {
                                window.open('', '_self', '');
                                window.close();
                              };
                        </script></head><body></body></html>""");


                exchange.sendResponseHeaders(200, htmlBuilder.toString().length());
                outputStream.write(htmlBuilder.toString().getBytes());
                outputStream.flush();
                outputStream.close();

                server.stop(0);
            }
        });
        server.setExecutor(null);
        server.start();
    }

    public static void setRequest(String s) {
        request = s;
    }

    public static String getRequest() {

        //server.stop(1);
        return request;
    }
}
