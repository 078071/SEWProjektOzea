package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Analyzes book text to extract various statistics such as word count,
 * main word count (excluding stop words), occurrences of the word "Mensch",
 * and long words (19+ characters).
 *
 * @author Ozea Gjoni
 * @version 1.0
 */
public class BookAnalysis {
    private final String title;
    private final String text;

    // List of common stop words to be excluded from the main word count
    private static final Set<String> STOP_WORDS = Set.of("und", "oder", "der", "die", "das", "ein", "eine");

    // Pattern to match the word "Mensch" case-insensitively
    private static final Pattern MENSCH_PATTERN = Pattern.compile("\\b[mM]ensch\\b");

    // Pattern to match long words (19+ characters)
    private static final Pattern LONG_WORD_PATTERN = Pattern.compile("\\b\\w{19,}\\b");

    /**
     * Constructs a BookAnalysis instance.
     *
     * @param title The title of the book.
     * @param text  The text content of the book.
     */
    public BookAnalysis(String title, String text) {
        this.title = title;
        this.text = text;
    }

    /**
     * Counts the total number of words in the book's text.
     *
     * @return The total word count.
     */
    public int getWordCount() {
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return 0;
        }
        String[] words = trimmedText.split("\\s+");
        return words.length;
    }

    /**
     * Counts the number of main words (excluding stop words) in the text.
     *
     * @return The count of main words.
     */
    public int getMainWordCount() {
        return (int) Arrays.stream(text.split("\\s+"))
                .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
                .count();
    }

    /**
     * Counts occurrences of the word "Mensch" in the text.
     *
     * @return The number of occurrences of "Mensch".
     */
    public int getMenschCount() {
        Matcher matcher = MENSCH_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Finds all words in the text that are 19 or more characters long.
     *
     * @return A list of long words.
     */
    public List<String> getLongWords() {
        List<String> longWords = new ArrayList<>();
        Matcher matcher = LONG_WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            longWords.add(matcher.group());
        }
        return longWords;
    }

    /**
     * Prints the analysis results including word counts and detected long words.
     */
    public void printAnalysis() {
        System.out.println("Title: " + title);
        System.out.println("Total Words: " + getWordCount());
        System.out.println("Main Words (without Stop-Words): " + getMainWordCount());
        System.out.println("'Mensch' Count: " + getMenschCount());
        System.out.println("Long Words (19+ characters): " + getLongWords());
        System.out.println("-------------------------------------------------");
    }
}
