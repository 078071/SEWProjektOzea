package org.example;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import com.google.gson.*;

public class Main {
    public static void main(String[] args) {
        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
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

                BookAnalysis bookAnalysis = new BookAnalysis(title, text);
                bookAnalysis.printAnalysis();
            }
        } catch (IOException e) {
            System.err.println("Error fetching or parsing JSON: " + e.getMessage());
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
