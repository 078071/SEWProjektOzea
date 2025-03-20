package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Scanner;
import com.google.gson.*;
import java.util.Base64;

public class Main {
    private static final String CSV_FILE = "results.csv";

    private static final String DB_URL = "jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres";
    private static final String DB_USER = "postgres.eqkmvnnlhzaerwgzxvnm";
    private static final String DB_PASSWORD = "ozbozBroja321.";

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        long startTime = System.nanoTime();

        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> futures = new ArrayList<>();
        List<String> csvLines = new ArrayList<>();

        csvLines.add("id,title,word_count,main_word_count,mensch_count,long_words");

        logger.info("Connecting to DB: " + DB_URL);

        System.out.println("Basic Auth Header: " + getBasicAuthHeader());

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            logger.info("Connected to Supabase successfully!");

            String jsonResponse = fetchJson(apiUrl);
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            if (!jsonObject.has("books") || !jsonObject.get("books").isJsonArray()) {
                throw new IllegalStateException("Unexpected JSON structure: 'books' field is missing or not an array");
            }

            JsonArray booksArray = jsonObject.getAsJsonArray("books");
            String uuid = jsonObject.get("uuid").getAsString();

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
                    String result = future.get();
                    logger.info("Processed book result: " + result);
                    csvLines.add(result);

                    writeToDatabase(result, connection);

                } catch (InterruptedException | ExecutionException e) {
                    logger.severe("Error processing book: " + e.getMessage());
                }
            }

            writeCsv(csvLines);

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000;
            logger.info("Execution time: " + duration + " ms");
            System.out.println("Execution time: " + duration + " ms");

            // ✅ Send results to the API
            String jsonPayload = createJsonPayload(uuid, duration, csvLines);
            logger.info("Sending results to API...");

            try {
                sendPostRequest(jsonPayload);
            } catch (IOException e) {
                logger.severe("Failed to send POST request: " + e.getMessage());
            }

        } catch (IOException e) {
            logger.severe("Error fetching or parsing JSON: " + e.getMessage());
        } catch (SQLException e) {
            logger.severe("Database connection error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }

    private static String getBasicAuthHeader() {
        String username = "student";
        String password = "supersecret";
        String auth = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
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
            logger.info("CSV successfully written to " + CSV_FILE);
        } catch (IOException e) {
            logger.severe("Error writing CSV file: " + e.getMessage());
        }
    }

    private static void writeToDatabase(String result, Connection connection) throws SQLException {
        String[] values = result.split(",", 6);
        int id = Integer.parseInt(values[0]);

        String checkSql = "SELECT COUNT(*) FROM results WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                logger.warning("Skipping duplicate entry for book ID: " + id);
                return;
            }
        }

        String sql = "INSERT INTO results (id, title, word_count, main_word_count, mensch_count, long_words) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setString(2, values[1]);
            stmt.setInt(3, Integer.parseInt(values[2]));
            stmt.setInt(4, Integer.parseInt(values[3]));
            stmt.setInt(5, Integer.parseInt(values[4]));
            stmt.setString(6, values[5]);

            stmt.executeUpdate();
            logger.info("Inserted data into Supabase for book: " + values[1]);
        }
    }

    private static String createJsonPayload(String uuid, long duration, List<String> results) {
        StringBuilder resultsJson = new StringBuilder();
        resultsJson.append("[");

        for (String result : results.subList(1, results.size())) {
            String[] parts = result.split(",", 6);
            resultsJson.append(String.format("""
            {
                "id": %s,
                "title": "%s",
                "word_count": %s,
                "main_word_count": %s,
                "mensch_count": %s,
                "long_words": "%s"
            }
        """, parts[0], parts[1], parts[2], parts[3], parts[4], parts[5]));
            resultsJson.append(","); // Add comma after each object
        }

        // ✅ Remove trailing comma
        if (resultsJson.length() > 1) {
            resultsJson.deleteCharAt(resultsJson.length() - 1);
        }
        resultsJson.append("]");

        return String.format("""
    {
        "uuid": "%s",
        "duration": %d,
        "name": "Ozea Gjoni",
        "url": "https://github.com/078071/SEWProjektOzea",
        "results": %s
    }
    """, uuid, duration, resultsJson.toString());
    }



    private static void sendPostRequest(String jsonPayload) throws IOException {
        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";

        // DEBUG: Print JSON payload
        System.out.println("DEBUG: Sending JSON payload:");
        System.out.println(jsonPayload);

        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthHeader());
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        int responseCode = conn.getResponseCode();
        logger.info("POST Response Code: " + responseCode);

        if (responseCode == 200 || responseCode == 201) {
            System.out.println("Data successfully sent to API!");
        } else {
            System.out.println("Failed to send data. Response Code: " + responseCode);
        }
    }


}
