package org.example;

import java.util.concurrent.Callable;
import java.util.List;

/**
 * A callable task that performs text analysis on a book.
 * It extracts various statistics such as word count, main word count,
 * occurrences of the word "Mensch," and long words (19+ characters).
 *
 * @author Ozea Gjoni
 * @version 1.0
 */
public class BookAnalysisTask implements Callable<String> {
    private final int id;
    private final String title;
    private final String text;

    /**
     * Constructs a BookAnalysisTask instance.
     *
     * @param id    The unique identifier of the book.
     * @param title The title of the book.
     * @param text  The text content of the book.
     */
    public BookAnalysisTask(int id, String title, String text) {
        this.id = id;
        this.title = title;
        this.text = text;
    }

    /**
     * Executes the book analysis and returns the results as a formatted CSV string.
     *
     * @return A CSV-formatted string containing book analysis results.
     */
    @Override
    public String call() {
        BookAnalysis analysis = new BookAnalysis(title, text);

        // Format the analysis results as a CSV line
        return String.format(
                "%d,%s,%d,%d,%d,%s",
                id,
                title.replace(",", ""), // Removes commas to avoid breaking CSV formatting
                analysis.getWordCount(),
                analysis.getMainWordCount(),
                analysis.getMenschCount(),
                String.join(" ", analysis.getLongWords()) // Converts the list to a space-separated string
        );
    }
}