package org.example;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.Scanner;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) {
        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
        ExecutorService executor = Executors.newFixedThreadPool(4); // Pool mit 4 Threads
        List<Future<String>> futures = new ArrayList<>();

        try {
            String jsonResponse = fetchJson(apiUrl);
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!jsonObject.has("books") || !jsonObject.get("books").isJsonArray()) {
                throw new IllegalStateException("Unexpected JSON structure: 'books' field is missing or not an array");
            }

            JsonArray booksArray = jsonObject.getAsJsonArray("books");

            for (JsonElement element : booksArray) {
                JsonObject bookObject = element.getAsJsonObject();
                String title = bookObject.get("title").getAsString();
                String text = bookObject.get("text").getAsString();

                // Parallel Book Analysis
                BookAnalysisTask task = new BookAnalysisTask(title, text);
                futures.add(executor.submit(task));
            }

            // Ergebnisse sammeln und ausgeben
            for (Future<String> future : futures) {
                try {
                    System.out.println(future.get()); // Ergebnisse ausgeben
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error processing book: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.err.println("Error fetching or parsing JSON: " + e.getMessage());
        } finally {
            executor.shutdown(); // ExecutorService sauber beenden
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
}
