package sambal.ontologysambal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class WebServer {

    private static final int PORT = 8080;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static void main(String[] args) throws IOException {
        OntologiService svc = OntologiService.get(); // load ontologi sekali di awal
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/", ex -> handleApi(ex, svc));
        server.createContext("/", WebServer::handleStatic);
        server.start();
        System.out.println("Server jalan. Buka di browser: http://localhost:" + PORT + "/");
    }

    private static void handleApi(HttpExchange ex, OntologiService svc) throws IOException {
        Object out;
        int status = 200;
        try {
            String path = ex.getRequestURI().getPath();
            Map<String, String> q = parseQuery(ex.getRequestURI().getRawQuery());
            switch (path) {
                case "/api/opsi":
                    out = svc.opsi();
                    break;
                case "/api/sambal":
                    out = svc.daftar(q.get("cari"), q.get("jenis"), q.get("pedas"),
                            q.get("bahan"), q.get("sajian"));
                    break;
                case "/api/detail":
                    out = svc.detail(q.get("id"));
                    if (out == null) {
                        status = 404;
                        out = Map.of("error", "Sambal tidak ditemukan");
                    }
                    break;
                default:
                    status = 404;
                    out = Map.of("error", "Endpoint tidak dikenal");
            }
        } catch (Exception e) {
            e.printStackTrace();
            status = 500;
            out = Map.of("error", String.valueOf(e.getMessage()));
        }
        kirim(ex, status, "application/json; charset=UTF-8",
                GSON.toJson(out).getBytes(StandardCharsets.UTF_8));
    }

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = URLDecoder.decode(ex.getRequestURI().getRawPath(), StandardCharsets.UTF_8);
        if (path.equals("/")) path = "/index.html";
        if (path.contains("..")) {
            kirim(ex, 400, "text/plain; charset=UTF-8", "Bad request".getBytes(StandardCharsets.UTF_8));
            return;
        }
        try (InputStream in = WebServer.class.getResourceAsStream("/web" + path)) {
            if (in == null) {
                kirim(ex, 404, "text/plain; charset=UTF-8", "Tidak ditemukan".getBytes(StandardCharsets.UTF_8));
                return;
            }
            kirim(ex, 200, tipe(path), in.readAllBytes());
        }
    }

    private static String tipe(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html; charset=UTF-8";
        if (p.endsWith(".css")) return "text/css; charset=UTF-8";
        if (p.endsWith(".js")) return "application/javascript; charset=UTF-8";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".webp")) return "image/webp";
        return "application/octet-stream";
    }

    private static void kirim(HttpExchange ex, int status, String type, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", type);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(body);
        }
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String pair : raw.split("&")) {
            int i = pair.indexOf('=');
            String k = i < 0 ? pair : pair.substring(0, i);
            String v = i < 0 ? "" : pair.substring(i + 1);
            m.put(URLDecoder.decode(k, StandardCharsets.UTF_8),
                  URLDecoder.decode(v, StandardCharsets.UTF_8));
        }
        return m;
    }
}