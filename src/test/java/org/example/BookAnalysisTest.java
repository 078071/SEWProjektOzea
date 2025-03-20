package org.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for the BookAnalysis class.
 * Verifies the accuracy of word counting, stop-word filtering,
 * "Mensch" detection, and long word identification.
 *
 * @author Ozea Gjoni
 * @version 1.0
 */
public class BookAnalysisTest {

    /**
     * Tests the word count method.
     * Ensures that words are counted correctly, including edge cases like empty strings.
     */
    @Test
    void testGetWordCount() {
        BookAnalysis book = new BookAnalysis("Test", "Das ist ein Test.");
        System.out.println("Word Count (expected 4): " + book.getWordCount());
        assertEquals(4, book.getWordCount());

        BookAnalysis emptyBook = new BookAnalysis("Empty", "");
        System.out.println("Word Count (expected 0): " + emptyBook.getWordCount());
        assertEquals(0, emptyBook.getWordCount());
    }

    /**
     * Tests the main word count method.
     * Ensures that stop words are correctly filtered out from the total word count.
     */
    @Test
    void testGetMainWordCount() {
        BookAnalysis book = new BookAnalysis("Test", "Das ist ein schöner Test und die Prüfung.");
        assertEquals(4, book.getMainWordCount()); // "Das" & "die" & "und" -> Stopwords

        BookAnalysis allStopWords = new BookAnalysis("StopWords", "und oder der die das ein eine");
        assertEquals(0, allStopWords.getMainWordCount());
    }

    /**
     * Tests the detection of occurrences of the word "Mensch."
     * Ensures only exact matches (case-insensitive) are counted.
     */
    @Test
    void testGetMenschCount() {
        BookAnalysis book = new BookAnalysis("Test", "Mensch und menschliche Menschen sind freundlich.");
        assertEquals(1, book.getMenschCount()); // "menschliche" & "Menschen" zählen nicht

        BookAnalysis noMensch = new BookAnalysis("No Mensch", "Kein einziges Vorkommen.");
        assertEquals(0, noMensch.getMenschCount());
    }

    /**
     * Tests the detection of long words (19+ characters).
     * Ensures that only words above the threshold are identified.
     */
    @Test
    void testGetLongWords() {
        BookAnalysis book = new BookAnalysis("Test", "Superkalifragilistischesexpialigetisch ist ein langes Wort.");
        List<String> longWords = book.getLongWords();
        assertEquals(1, longWords.size());
        assertTrue(longWords.contains("Superkalifragilistischesexpialigetisch"));

        BookAnalysis noLongWords = new BookAnalysis("No Long Words", "Kurze Wörter.");
        assertTrue(noLongWords.getLongWords().isEmpty());
    }
}