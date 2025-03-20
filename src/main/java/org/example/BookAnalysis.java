package org.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BookAnalysis {
    private final String title;
    private final String text;
    private static final Set<String> STOP_WORDS = Set.of("und", "oder", "der", "die", "das", "ein", "eine");
    private static final Pattern MENSCH_PATTERN = Pattern.compile("\\b[mM]ensch\\b");
    private static final Pattern LONG_WORD_PATTERN = Pattern.compile("\\b\\w{19,}\\b");

    public BookAnalysis(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public int getWordCount() {
        String trimmedText = text.trim();
        if (trimmedText.isEmpty()) {
            return 0;
        }
        String[] words = trimmedText.split("\\s+");
        return words.length;
    }


    public int getMainWordCount() {
        return (int) Arrays.stream(text.split("\\s+"))
                .filter(word -> !STOP_WORDS.contains(word.toLowerCase()))
                .count();
    }

    public int getMenschCount() {
        Matcher matcher = MENSCH_PATTERN.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    public List<String> getLongWords() {
        List<String> longWords = new ArrayList<>();
        Matcher matcher = LONG_WORD_PATTERN.matcher(text);
        while (matcher.find()) {
            longWords.add(matcher.group());
        }
        return longWords;
    }

    public void printAnalysis() {
        System.out.println("Title: " + title);
        System.out.println("Total Words: " + getWordCount());
        System.out.println("Main Words (without Stop-Words): " + getMainWordCount());
        System.out.println("'Mensch' Count: " + getMenschCount());
        System.out.println("Long Words (18+ characters): " + getLongWords());
        System.out.println("-------------------------------------------------");
    }
}
