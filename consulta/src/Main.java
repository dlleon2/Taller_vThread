import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    // Ruta del archivo de entrada que contiene las URLs
    private static final String INPUT = "C:\\Users\\Victus\\Documents\\urls_parcial1-1.txt";

    // Ruta del archivo de salida donde se escribirán los resultados
    private static final String OUTPUT = "C:\\Users\\Victus\\Documents\\resultados.txt";

    // Patrón regex para localizar enlaces href que sean URLs HTTP o HTTPS
    private static final Pattern LINK_PATTERN = Pattern.compile(
            "href\\s*=\\s*\"(http[s]?://[^\"]+)\"", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) throws Exception {
        /*
         * Utiliza un ExecutorService que crea un hilo virtual por cada tarea,
         * mejorando eficiencia y escalabilidad.
         */
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        // Leer todas las URLs desde el archivo de entrada
        List<String> urls = Files.readAllLines(Paths.get(INPUT));

        // Enviar al executor una tarea de procesamiento para cada URL
        List<Future<String>> tasks = urls.stream()
                .map(url -> executor.submit(() -> processUrl(url)))
                .toList();

        // Abrir el archivo de salida y escribir los resultados línea por línea
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(OUTPUT))) {
            for (Future<String> task : tasks) {
                try {
                    writer.write(task.get());
                    writer.newLine();
                } catch (Exception e) {
                    writer.write("ERROR: " + e.getMessage());
                    writer.newLine();
                }
            }
        }

        // Apagar el executor para liberar recursos
        executor.shutdown();
    }

    /**
     * Procesa una URL: realiza una petición HTTP GET, cuenta los enlaces internos
     * y devuelve un resumen como cadena.
     */
    private static String processUrl(String urlStr) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlStr))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            // Enviar la petición y obtener la respuesta como cadena
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String html = response.body();

            // Extraer el dominio base para luego filtrar enlaces internos
            URI baseUri = URI.create(urlStr);
            String domain = baseUri.getHost();
            int count = 0;

            // Buscar todos los href en el HTML y contar los que coinciden con el dominio
            Matcher matcher = LINK_PATTERN.matcher(html);
            while (matcher.find()) {
                String link = matcher.group(1);
                try {
                    URI linkUri = URI.create(link);
                    if (linkUri.getHost() != null && linkUri.getHost().endsWith(domain)) {
                        count++;
                    }
                } catch (Exception ignored) {
                    // Omitir URLs mal formadas
                }
            }

            return urlStr + " --> " + count + " enlaces internos";
        } catch (Exception e) {
            return urlStr + " --> ERROR: " + e.getMessage();
        }
    }
}
