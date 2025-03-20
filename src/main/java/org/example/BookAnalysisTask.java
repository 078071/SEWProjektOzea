package org.example;

import java.util.concurrent.Callable;

public class BookAnalysisTask implements Callable<String> {
    private final String title;
    private final String text;

    public BookAnalysisTask(String title, String text) {
        this.title = title;
        this.text = text;
    }

    @Override
    public String call() {
        BookAnalysis analysis = new BookAnalysis(title, text);

        return String.format(
                "Title: %s%nTotal Words: %d%nMain Words (without Stop-Words): %d%n'Mensch' Count: %d%nLong Words (18+ characters): %s%n-------------------------------------------------",
                title,
                analysis.getWordCount(),
                analysis.getMainWordCount(),
                analysis.getMenschCount(),
                analysis.getLongWords()
        );
    }
}
