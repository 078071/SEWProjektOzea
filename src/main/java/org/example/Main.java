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

/**
 * The main class for the SEW5 project.
 * Fetches book data from an API, processes it, stores results in a PostgreSQL database,
 * writes the data to a CSV file, and sends the processed results via a REST API.
 *
 * @author Ozea Gjoni
 * @version 1.0
 */
public class Main {
    // Path of the CSV file where results are stored
    private static final String CSV_FILE = "results.csv";

    // Database connection details (Supabase PostgreSQL instance)
    private static final String DB_URL = "jdbc:postgresql://aws-0-eu-central-1.pooler.supabase.com:5432/postgres";
    private static final String DB_USER = "postgres.eqkmvnnlhzaerwgzxvnm";
    private static final String DB_PASSWORD = "ozbozBroja321.";

    // Logger for debugging and tracking execution
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Main entry point of the program.
     * Fetches book data, processes it, writes results to CSV and database, and sends processed data to an API.
     *
     * @param args Command-line arguments (not used)
     */
    public static void main(String[] args) {
        long startTime = System.nanoTime(); // Start execution timer

        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";
        ExecutorService executor = Executors.newFixedThreadPool(4); // Thread pool for parallel processing
        List<Future<String>> futures = new ArrayList<>(); // List to store future results
        List<String> csvLines = new ArrayList<>(); // List to store CSV output lines

        csvLines.add("id,title,word_count,main_word_count,mensch_count,long_words"); // CSV header row

        logger.info("Connecting to DB: " + DB_URL);
        System.out.println("Basic Auth Header: " + getBasicAuthHeader());

        try (Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            logger.info("Connected to Supabase successfully!");

            // Fetch data from API
            String jsonResponse = fetchJson(apiUrl);
            JsonObject jsonObject = JsonParser.parseString(jsonResponse).getAsJsonObject();

            // Validate response structure
            if (!jsonObject.has("books") || !jsonObject.get("books").isJsonArray()) {
                throw new IllegalStateException("Unexpected JSON structure: 'books' field is missing or not an array");
            }

            JsonArray booksArray = jsonObject.getAsJsonArray("books");
            String uuid = jsonObject.get("uuid").getAsString(); // Retrieve UUID from API response

            // Iterate through books and create tasks for processing
            for (JsonElement element : booksArray) {
                JsonObject bookObject = element.getAsJsonObject();
                int id = bookObject.get("id").getAsInt();
                String title = bookObject.get("title").getAsString();
                String text = bookObject.get("text").getAsString();

                BookAnalysisTask task = new BookAnalysisTask(id, title, text);
                futures.add(executor.submit(task)); // Submit task to thread pool
            }

            // Retrieve and process results
            for (Future<String> future : futures) {
                try {
                    String result = future.get();
                    logger.info("Processed book result: " + result);
                    csvLines.add(result);
                    writeToDatabase(result, connection); // Store result in database
                } catch (InterruptedException | ExecutionException e) {
                    logger.severe("Error processing book: " + e.getMessage());
                }
            }

            writeCsv(csvLines); // Write results to CSV

            long endTime = System.nanoTime();
            long duration = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            logger.info("Execution time: " + duration + " ms");

            // Send processed results to API
            String jsonPayload = createJsonPayload(uuid, duration, csvLines);
            logger.info("Sending results to API...");
            sendPostRequest(jsonPayload);

        } catch (IOException | SQLException e) {
            logger.severe("Error during execution: " + e.getMessage());
        } finally {
            executor.shutdown(); // Shutdown thread pool
        }
    }

    /**
     * Generates a Basic Authentication header for API requests.
     *
     * @return Encoded Basic Auth header
     */
    private static String getBasicAuthHeader() {
        String auth = "student:supersecret";
        return "Basic " + Base64.getEncoder().encodeToString(auth.getBytes());
    }

    /**
     * Fetches JSON data from a given API URL.
     *
     * @param apiUrl The API endpoint URL
     * @return JSON response as a string
     * @throws IOException If an error occurs during the request
     */
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

    /**
     * Writes the processed book data to a CSV file.
     *
     * @param lines The list of CSV-formatted strings
     */
    private static void writeCsv(List<String> lines) {
        Path path = Paths.get(CSV_FILE);
        try {
            Files.write(path, lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("CSV successfully written to " + CSV_FILE);
        } catch (IOException e) {
            logger.severe("Error writing CSV file: " + e.getMessage());
        }
    }

    /**
     * Stores processed book data in the PostgreSQL database.
     *
     * @param result     The processed book data string
     * @param connection Database connection object
     * @throws SQLException If an SQL error occurs
     */
    private static void writeToDatabase(String result, Connection connection) throws SQLException {
        String[] values = result.split(",", 6);
        int id = Integer.parseInt(values[0]);

        // Check if the entry already exists
        String checkSql = "SELECT COUNT(*) FROM results WHERE id = ?";
        try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
            checkStmt.setInt(1, id);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                logger.warning("Skipping duplicate entry for book ID: " + id);
                return;
            }
        }

        // Insert book data into the database
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

    /**
     * Creates a JSON payload containing the processed results.
     *
     * @param uuid     The unique identifier received from the API.
     * @param duration The execution time of the program in milliseconds.
     * @param results  The list of processed book results in CSV format.
     * @return A formatted JSON string containing the results.
     */
    private static String createJsonPayload(String uuid, long duration, List<String> results) {
        StringBuilder resultsJson = new StringBuilder();
        resultsJson.append("["); // Start JSON array

        // Convert each result entry into a JSON object
        for (String result : results.subList(1, results.size())) { // Skip header row
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
            resultsJson.append(","); // Append comma after each object
        }

        // Remove the trailing comma to maintain valid JSON format
        if (resultsJson.length() > 1) {
            resultsJson.deleteCharAt(resultsJson.length() - 1);
        }
        resultsJson.append("]"); // Close JSON array

        // Construct final JSON payload
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

    /**
     * Sends a POST request with the processed JSON payload to the API.
     *
     * @param jsonPayload The JSON string containing processed book results.
     * @throws IOException If an error occurs while sending the request.
     */
    private static void sendPostRequest(String jsonPayload) throws IOException {
        String apiUrl = "https://htl-assistant.vercel.app/api/projects/sew5";

        // DEBUG: Print JSON payload before sending
        System.out.println("DEBUG: Sending JSON payload:");
        System.out.println(jsonPayload);

        // Open HTTP connection
        HttpURLConnection conn = (HttpURLConnection) new URL(apiUrl).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", getBasicAuthHeader());
        conn.setDoOutput(true);

        // Send JSON payload
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonPayload.getBytes());
            os.flush();
        }

        // Get response status code
        int responseCode = conn.getResponseCode();
        logger.info("POST Response Code: " + responseCode);

        // Check if data was successfully sent
        if (responseCode == 200 || responseCode == 201) {
            System.out.println("Data successfully sent to API!");
        } else {
            System.out.println("Failed to send data. Response Code: " + responseCode);
        }
    }
}
