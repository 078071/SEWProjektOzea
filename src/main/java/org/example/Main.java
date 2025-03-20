package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.Scanner;
import com.google.gson.*;

public class Main {
    private static final String CSV_FILE = "results.csv";

    public static void main(String[] args) {
        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> futures = new ArrayList<>();
        List<String> csvLines = new ArrayList<>();

        // CSV-Header hinzufügen
        csvLines.add("id,title,word_count,main_word_count,mensch_count,long_words");

        try {
            String jsonResponse = fetchJson(apiUrl);
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!jsonObject.has("books") || !jsonObject.get("books").isJsonArray()) {
                throw new IllegalStateException("Unexpected JSON structure: 'books' field is missing or not an array");
            }

            JsonArray booksArray = jsonObject.getAsJsonArray("books");

            for (JsonElement element : booksArray) {
                JsonObject bookObject = element.getAsJsonObject();
                int id = bookObject.get("id").getAsInt();
                String title = bookObject.get("title").getAsString();
                String text = bookObject.get("text").getAsString();

                BookAnalysisTask task = new BookAnalysisTask(id, title, text);
                futures.add(executor.submit(task));
            }

            for (Future<String> future : futures) {
                try {
                    String result = future.get(); // Ergebnis abrufen
                    System.out.println(result);  // In der Konsole ausgeben
                    csvLines.add(result);        // In die CSV-Liste einfügen
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error processing book: " + e.getMessage());
                }
            }

            // CSV-Datei mit Java NIO schreiben
            writeCsv(csvLines);

        } catch (IOException e) {
            System.err.println("Error fetching or parsing JSON: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private static String fetchJson(String apiUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        try (Scanner scanner = new Scanner(conn.getInputStream())) {
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            return response.toString();
        }
    }

    private static void writeCsv(List<String> lines) {
        Path path = Paths.get(CSV_FILE);
        try {
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("CSV successfully written to " + CSV_FILE);
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
}
