package org.example;

import java.io.IOException;
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
import java.util.Base64; // Added for Basic Auth

public class Main {
    private static final String CSV_FILE = "results.csv";

    // Hardcoded Supabase Pooler Connection
    private static final String DB_URL = "jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres";
    private static final String DB_USER = "postgres.eqkmvnnlhzaerwgzxvnm";
    private static final String DB_PASSWORD = "ozbozBroja321.";

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        long startTime = System.nanoTime(); // Start timing

        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<String>> futures = new ArrayList<>();
        List<String> csvLines = new ArrayList<>();

        csvLines.add("id,title,word_count,main_word_count,mensch_count,long_words");

        logger.info("Connecting to DB: " + DB_URL);

        // TEST Basic Auth Header
        System.out.println("Basic Auth Header: " + getBasicAuthHeader());

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            logger.info("Connected to Supabase successfully!");

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
                    String result = future.get();
                    logger.info("Processed book result: " + result);
                    csvLines.add(result);

                    // Store data in Supabase!
                    writeToDatabase(result, connection);

                } catch (InterruptedException | ExecutionException e) {
                    logger.severe("Error processing book: " + e.getMessage());
                }
            }

            writeCsv(csvLines);

        } catch (IOException e) {
            logger.severe("Error fetching or parsing JSON: " + e.getMessage());
        } catch (SQLException e) {
            logger.severe("Database connection error: " + e.getMessage());
        } finally {
            executor.shutdown();
        }

        long endTime = System.nanoTime();
        long duration = (endTime - startTime) / 1_000_000;
        logger.info("Execution time: " + duration + " ms");
        System.out.println("Execution time: " + duration + " ms");
    }

    //  Basic Authentication Function
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

        // Check if the ID already exists
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
}
